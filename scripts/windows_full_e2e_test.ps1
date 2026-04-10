param(
  [string]$BaseUrl = "http://localhost:8090/api",
  [string]$IotCode = "shaft-dev-001",
  [int]$PartitionCount = 50,
  [int]$PressureTotal = 3000,
  [int]$PressureConcurrency = 30,
  [int]$HttpTimeoutSec = 10,
  [int]$OfflineWaitSec = 75,
  [switch]$SkipOfflineTest,
  [int]$MaxErrorPrint = 30
)

if ($PartitionCount -lt 1) { throw "PartitionCount must be >= 1" }
if ($PressureTotal -lt 1) { throw "PressureTotal must be >= 1" }
if ($PressureConcurrency -lt 1) { throw "PressureConcurrency must be >= 1" }
if ($MaxErrorPrint -lt 0) { throw "MaxErrorPrint must be >= 0" }

$BaseUrl = [string]$BaseUrl
$BaseUrl = $BaseUrl.Trim()
if (($BaseUrl.StartsWith('"') -and $BaseUrl.EndsWith('"')) -or ($BaseUrl.StartsWith("'") -and $BaseUrl.EndsWith("'"))) {
  $BaseUrl = $BaseUrl.Substring(1, $BaseUrl.Length - 2)
}
if ($BaseUrl.EndsWith("/")) {
  $BaseUrl = $BaseUrl.TrimEnd("/")
}
if ($BaseUrl -notmatch "^https?://") {
  $BaseUrl = "http://" + $BaseUrl
}

$measureUrl = "$BaseUrl/shaft/iot/reports/measure"
$alarmUrl = "$BaseUrl/shaft/iot/reports/alarm"
$alarmListUrl = "$BaseUrl/shaft/alarm/list"

function Assert-AbsoluteHttpUrl {
  param(
    [string]$Name,
    [string]$Url
  )
  $parsed = $null
  $ok = [System.Uri]::TryCreate($Url, [System.UriKind]::Absolute, [ref]$parsed)
  if (-not $ok) {
    throw ("invalid url for {0}: {1}" -f $Name, $Url)
  }
  if ($parsed.Scheme -ne "http" -and $parsed.Scheme -ne "https") {
    throw ("unsupported url scheme for {0}: {1}" -f $Name, $parsed.Scheme)
  }
  if ([string]::IsNullOrWhiteSpace($parsed.Host)) {
    throw ("missing host for {0}: {1}" -f $Name, $Url)
  }
}

Assert-AbsoluteHttpUrl -Name "BaseUrl" -Url $BaseUrl
Assert-AbsoluteHttpUrl -Name "measureUrl" -Url $measureUrl
Assert-AbsoluteHttpUrl -Name "alarmUrl" -Url $alarmUrl
Assert-AbsoluteHttpUrl -Name "alarmListUrl" -Url $alarmListUrl

$apiTotal = 0
$apiOk = 0
$apiFail = 0
$stagePass = 0
$stageFail = 0
$printedError = 0
$offlineThresholdSecFromRule = 0

function Write-Stage {
  param([string]$Title)
  Write-Host ""
  Write-Host ("================ {0} ================" -f $Title) -ForegroundColor Cyan
}

function Invoke-JsonApi {
  param(
    [string]$Method,
    [string]$Url,
    [object]$Body = $null,
    [int]$TimeoutSec = 10
  )
  $script:apiTotal++
  try {
    Write-Host ("[HTTP] method={0} url={1}" -f $Method, $Url) -ForegroundColor DarkGray
    $json = $null
    if ($null -ne $Body) {
      $json = $Body | ConvertTo-Json -Depth 8 -Compress
    }
    $resp = Invoke-WebRequest -Uri $Url -Method $Method -ContentType "application/json" -Body $json -TimeoutSec $TimeoutSec
    $respBody = $resp.Content
    $bizOk = $false
    if (-not [string]::IsNullOrWhiteSpace($respBody)) {
      try {
        $obj = $respBody | ConvertFrom-Json
        if (($null -ne $obj.success -and [bool]$obj.success) -or ($null -ne $obj.code -and [int]$obj.code -eq 200)) {
          $bizOk = $true
        }
      } catch {
        $bizOk = ($resp.StatusCode -eq 200)
      }
    } else {
      $bizOk = ($resp.StatusCode -eq 200)
    }

    $ok = (($resp.StatusCode -eq 200) -and $bizOk)
    if ($ok) {
      $script:apiOk++
    } else {
      $script:apiFail++
      Write-Host ("[API_FAIL] method={0} status={1} url={2}" -f $Method, $resp.StatusCode, $Url) -ForegroundColor Red
      if (-not [string]::IsNullOrWhiteSpace($respBody)) {
        $print = $respBody
        if ($print.Length -gt 400) { $print = $print.Substring(0, 400) }
        Write-Host ("[API_FAIL_BODY] {0}" -f $print) -ForegroundColor DarkRed
      }
    }

    return [pscustomobject]@{
      ok = $ok
      status = [int]$resp.StatusCode
      bodyText = $respBody
    }
  } catch {
    $script:apiFail++
    $msg = $_.Exception.Message
    Write-Host ("[API_ERROR] method={0} url={1} msg={2}" -f $Method, $Url, $msg) -ForegroundColor Red
    return [pscustomobject]@{
      ok = $false
      status = 0
      bodyText = $msg
    }
  }
}

function New-MeasureBody {
  param(
    [int]$PartitionId,
    [double]$MaxTemp,
    [double]$MinTemp,
    [double]$AvgTemp
  )
  $partStr = "{0:D2}" -f $PartitionId
  $ref = "/TMP/$IotCode" + "_TMP_th$partStr"
  return @{
    topic = "$ref/Measure"
    iotCode = $IotCode
    PartitionId = $PartitionId
    IedFullPath = "/IED/$IotCode"
    dataReference = $ref
    MaxTemp = $MaxTemp
    MinTemp = $MinTemp
    AvgTemp = $AvgTemp
    MaxTempPosition = 11.0
    MinTempPosition = 3.0
    MaxTempChannel = 1
    MinTempChannel = 1
    timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
  }
}

function New-AlarmBody {
  param(
    [int]$PartitionId,
    [bool]$AlarmStatus,
    [bool]$FaultStatus
  )
  $partStr = "{0:D2}" -f $PartitionId
  $ref = "/TMP/$IotCode" + "_TMP_th$partStr"
  return @{
    topic = "$ref/Alarm"
    iotCode = $IotCode
    PartitionId = $PartitionId
    IedFullPath = "/IED/$IotCode"
    dataReference = $ref
    AlarmStatus = $AlarmStatus
    FaultStatus = $FaultStatus
    timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
  }
}

function Assert-Stage {
  param(
    [string]$Name,
    [bool]$Condition,
    [string]$PassMessage,
    [string]$FailMessage
  )
  if ($Condition) {
    $script:stagePass++
    Write-Host ("[PASS] {0} - {1}" -f $Name, $PassMessage) -ForegroundColor Green
  } else {
    $script:stageFail++
    Write-Host ("[FAIL] {0} - {1}" -f $Name, $FailMessage) -ForegroundColor Red
  }
}

function Query-AlarmsRaw {
  param(
    [string]$Status = "",
    [string]$AlarmTypeBig = "0",
    [string]$AreaName = "",
    [int]$PageNo = 1,
    [int]$PageSize = 200
  )
  $start = (Get-Date).AddHours(-24).ToString("yyyy-MM-dd HH:mm:ss")
  $end = (Get-Date).AddHours(1).ToString("yyyy-MM-dd HH:mm:ss")
  $q = @()
  $q += "pageNo=" + [System.Uri]::EscapeDataString([string]$PageNo)
  $q += "pageSize=" + [System.Uri]::EscapeDataString([string]$PageSize)
  if (-not [string]::IsNullOrWhiteSpace($Status)) { $q += "status=" + [System.Uri]::EscapeDataString([string]$Status) }
  if (-not [string]::IsNullOrWhiteSpace($AlarmTypeBig)) { $q += "alarmTypeBig=" + [System.Uri]::EscapeDataString([string]$AlarmTypeBig) }
  if (-not [string]::IsNullOrWhiteSpace($AreaName)) { $q += "areaName=" + [System.Uri]::EscapeDataString([string]$AreaName) }
  $q += "startTime=" + [System.Uri]::EscapeDataString([string]$start)
  $q += "endTime=" + [System.Uri]::EscapeDataString([string]$end)
  $ub = New-Object System.UriBuilder($alarmListUrl)
  $ub.Query = ($q -join "&")
  $url = $ub.Uri.AbsoluteUri
  Write-Host ("[QUERY_ALARM_LIST] {0}" -f $url) -ForegroundColor DarkGray
  return Invoke-JsonApi -Method "GET" -Url $url -TimeoutSec $HttpTimeoutSec
}

function Get-AlarmList {
  param(
    [string]$Status = "",
    [string]$AlarmTypeBig = "0",
    [string]$AreaName = "",
    [int]$PageNo = 1,
    [int]$PageSize = 200
  )
  $resp = Query-AlarmsRaw -Status $Status -AlarmTypeBig $AlarmTypeBig -AreaName $AreaName -PageNo $PageNo -PageSize $PageSize
  if (-not $resp.ok) { return @() }
  if ([string]::IsNullOrWhiteSpace($resp.bodyText)) { return @() }
  try {
    $obj = $resp.bodyText | ConvertFrom-Json
    if ($null -eq $obj -or $null -eq $obj.data -or $null -eq $obj.data.list) { return @() }
    if ($obj.data.list -is [System.Array]) { return $obj.data.list }
    return @($obj.data.list)
  } catch {
    return @()
  }
}

function Find-ThresholdAlarms {
  param([object[]]$Alarms)
  $result = @()
  foreach ($a in $Alarms) {
    if ($null -ne $a -and $a.alarmType -eq "TEMP_THRESHOLD") {
      $result += $a
    }
  }
  return $result
}

function Query-AlarmRules {
  $url = "$BaseUrl/shaft/alarm/alarm-rules?scopeType=GLOBAL&enabled=1"
  $resp = Invoke-JsonApi -Method "GET" -Url $url -TimeoutSec $HttpTimeoutSec
  if (-not $resp.ok -or [string]::IsNullOrWhiteSpace($resp.bodyText)) { return @() }
  try {
    $obj = $resp.bodyText | ConvertFrom-Json
    if ($null -eq $obj -or $null -eq $obj.data) { return @() }
    if ($obj.data -is [System.Array]) { return $obj.data }
    return @($obj.data)
  } catch {
    return @()
  }
}

Write-Host "start full e2e test..."
Write-Host ("baseUrl={0} iotCode={1} partitionCount={2} pressureTotal={3} concurrency={4}" -f $BaseUrl, $IotCode, $PartitionCount, $PressureTotal, $PressureConcurrency)
Write-Host ("[ENDPOINT] measureUrl={0}" -f $measureUrl) -ForegroundColor DarkGray
Write-Host ("[ENDPOINT] alarmUrl={0}" -f $alarmUrl) -ForegroundColor DarkGray
Write-Host ("[ENDPOINT] alarmListUrl={0}" -f $alarmListUrl) -ForegroundColor DarkGray
$rules = Query-AlarmRules
if ($rules.Count -gt 0) {
  $thresholdRule = $rules | Where-Object { $_.alarmType -eq "TEMP_THRESHOLD" } | Select-Object -First 1
  $offlineRule = $rules | Where-Object { $_.alarmType -eq "DEVICE_OFFLINE" } | Select-Object -First 1
  if ($null -ne $thresholdRule) {
    Write-Host ("[RULE] TEMP_THRESHOLD enabled={0} threshold={1}" -f $thresholdRule.enabled, $thresholdRule.thresholdValue) -ForegroundColor DarkGray
  } else {
    Write-Host "[RULE] TEMP_THRESHOLD not found in enabled GLOBAL rules (will fallback to app config)." -ForegroundColor DarkYellow
  }
  if ($null -ne $offlineRule) {
    Write-Host ("[RULE] DEVICE_OFFLINE enabled={0} threshold={1}" -f $offlineRule.enabled, $offlineRule.thresholdValue) -ForegroundColor DarkGray
    try {
      if ($null -ne $offlineRule.thresholdValue -and [double]$offlineRule.thresholdValue -gt 0) {
        $offlineThresholdSecFromRule = [int][double]$offlineRule.thresholdValue
      }
    } catch {
      $offlineThresholdSecFromRule = 0
    }
  } else {
    Write-Host "[RULE] DEVICE_OFFLINE not found in enabled GLOBAL rules (will fallback to app config)." -ForegroundColor DarkYellow
  }
}

Write-Stage "Stage 1 - baseline measure"
$r1 = Invoke-JsonApi -Method "POST" -Url $measureUrl -Body (New-MeasureBody -PartitionId 1 -MaxTemp 60 -MinTemp 50 -AvgTemp 55) -TimeoutSec $HttpTimeoutSec
Assert-Stage -Name "baseline_measure" -Condition $r1.ok -PassMessage "measure accepted" -FailMessage "measure failed"

Write-Stage "Stage 2 - threshold trigger"
$r2 = Invoke-JsonApi -Method "POST" -Url $measureUrl -Body (New-MeasureBody -PartitionId 1 -MaxTemp 88 -MinTemp 60 -AvgTemp 73) -TimeoutSec $HttpTimeoutSec
Assert-Stage -Name "temp_threshold_push" -Condition $r2.ok -PassMessage "high temp pushed" -FailMessage "high temp failed"
Start-Sleep -Seconds 1
$afterStage2 = Get-AlarmList -AlarmTypeBig "0" -PageNo 1 -PageSize 200
$stage2Threshold = Find-ThresholdAlarms -Alarms $afterStage2
if ($stage2Threshold.Count -gt 0) {
  Write-Host ("[DEBUG] stage2 threshold alarms found: {0}" -f $stage2Threshold.Count) -ForegroundColor DarkGray
} else {
  Write-Host "[DEBUG] stage2 no TEMP_THRESHOLD found. check partition bind/rule config." -ForegroundColor Yellow
}

Write-Stage "Stage 3 - temp diff trigger"
$r3 = Invoke-JsonApi -Method "POST" -Url $measureUrl -Body (New-MeasureBody -PartitionId 2 -MaxTemp 92 -MinTemp 60 -AvgTemp 76) -TimeoutSec $HttpTimeoutSec
Assert-Stage -Name "temp_diff_push" -Condition $r3.ok -PassMessage "diff pushed" -FailMessage "diff failed"

Write-Stage "Stage 4 - rise rate trigger"
$r4a = Invoke-JsonApi -Method "POST" -Url $measureUrl -Body (New-MeasureBody -PartitionId 3 -MaxTemp 52 -MinTemp 45 -AvgTemp 48) -TimeoutSec $HttpTimeoutSec
Start-Sleep -Seconds 1
$r4b = Invoke-JsonApi -Method "POST" -Url $measureUrl -Body (New-MeasureBody -PartitionId 3 -MaxTemp 72 -MinTemp 46 -AvgTemp 58) -TimeoutSec $HttpTimeoutSec
Assert-Stage -Name "rise_rate_push" -Condition ($r4a.ok -and $r4b.ok) -PassMessage "rise pair pushed" -FailMessage "rise pair failed"

Write-Stage "Stage 5 - alarm raw push"
$r5 = Invoke-JsonApi -Method "POST" -Url $alarmUrl -Body (New-AlarmBody -PartitionId 4 -AlarmStatus $true -FaultStatus $true) -TimeoutSec $HttpTimeoutSec
Assert-Stage -Name "alarm_raw_push" -Condition $r5.ok -PassMessage "alarm status accepted" -FailMessage "alarm status failed"

Write-Stage "Stage 6 - merge verify"
for ($i = 0; $i -lt 4; $i++) {
  $tmp = Invoke-JsonApi -Method "POST" -Url $measureUrl -Body (New-MeasureBody -PartitionId 1 -MaxTemp 89 -MinTemp 61 -AvgTemp 74) -TimeoutSec $HttpTimeoutSec
  if (-not $tmp.ok) {
    Write-Host ("[WARN] merge push failed idx={0}" -f $i) -ForegroundColor Yellow
  }
}
Start-Sleep -Seconds 1
$mergeOk = $false
$maxMergeCount = 0
for ($retry = 1; $retry -le 6; $retry++) {
  $alarmList = Get-AlarmList -AlarmTypeBig "0" -PageNo 1 -PageSize 200
  $thresholdAlarms = Find-ThresholdAlarms -Alarms $alarmList
  foreach ($a in $thresholdAlarms) {
    $mc = 0
    try { $mc = [int]$a.mergeCount } catch { $mc = 0 }
    if ($mc -gt $maxMergeCount) { $maxMergeCount = $mc }
    if ($mc -ge 2) {
      $mergeOk = $true
      break
    }
  }
  Write-Host ("[DEBUG] merge retry={0}/6 thresholdCount={1} maxMergeCount={2}" -f $retry, $thresholdAlarms.Count, $maxMergeCount) -ForegroundColor DarkGray
  if ($mergeOk) { break }
  Start-Sleep -Seconds 2
}
if (-not $mergeOk) {
  Write-Host "[DEBUG] merge_check failed. possible reasons: no threshold alarm generated / alarm moved out of pending / db unique merge key missing." -ForegroundColor Yellow
}
Assert-Stage -Name "merge_check" -Condition $mergeOk -PassMessage "mergeCount >= 2 found" -FailMessage ("merged alarm not found (maxMergeCount=" + $maxMergeCount + ")")

if (-not $SkipOfflineTest) {
  Write-Stage "Stage 7 - offline check"
  $effectiveOfflineWaitSec = $OfflineWaitSec
  if ($offlineThresholdSecFromRule -gt 0) {
    $minWait = $offlineThresholdSecFromRule + 60
    if ($effectiveOfflineWaitSec -lt $minWait) {
      $effectiveOfflineWaitSec = $minWait
    }
  }
  Write-Host ("waiting {0}s for inspection... (ruleThreshold={1}s, inputWait={2}s)" -f $effectiveOfflineWaitSec, $offlineThresholdSecFromRule, $OfflineWaitSec) -ForegroundColor Yellow
  for ($t = 1; $t -le $effectiveOfflineWaitSec; $t++) {
    if (($t % 5) -eq 0) {
      Write-Host ("offline-wait: {0}/{1}s" -f $t, $effectiveOfflineWaitSec) -ForegroundColor DarkYellow
    }
    Start-Sleep -Seconds 1
  }
  $offlineOk = $false
  for ($retry = 1; $retry -le 6; $retry++) {
    $offList = Get-AlarmList -AlarmTypeBig "0" -PageNo 1 -PageSize 200
    $offCount = 0
    foreach ($a in $offList) {
      if ($a.alarmType -eq "DEVICE_OFFLINE") {
        $offCount++
      }
    }
    Write-Host ("[DEBUG] offline retry={0}/6 offlineAlarmCount={1}" -f $retry, $offCount) -ForegroundColor DarkGray
    if ($offCount -gt 0) {
      $offlineOk = $true
      break
    }
    Start-Sleep -Seconds 5
  }
  Assert-Stage -Name "offline_check" -Condition $offlineOk -PassMessage "offline alarm found" -FailMessage "offline alarm not found"
}

Write-Stage "Stage 8 - pressure test"
$pOk = 0
$pFail = 0
$batchCount = [math]::Ceiling($PressureTotal / $PressureConcurrency)
$sw = [System.Diagnostics.Stopwatch]::StartNew()

for ($batch = 0; $batch -lt $batchCount; $batch++) {
  $batchSw = [System.Diagnostics.Stopwatch]::StartNew()
  $jobs = @()

  for ($i = 0; $i -lt $PressureConcurrency; $i++) {
    $index = $batch * $PressureConcurrency + $i + 1
    if ($index -gt $PressureTotal) { break }

    $jobs += Start-Job -ScriptBlock {
      param($index, $partitionCount, $iotCode, $measureUrl, $timeoutSec)
      try {
        $part = ($index % $partitionCount) + 1
        $partStr = "{0:D2}" -f $part
        $ref = "/TMP/$iotCode" + "_TMP_th$partStr"
        $bodyObj = @{
          topic = "$ref/Measure"
          iotCode = $iotCode
          PartitionId = $part
          IedFullPath = "/IED/$iotCode"
          dataReference = $ref
          MaxTemp = (65 + ($part % 20))
          MinTemp = (50 + ($part % 10))
          AvgTemp = (58 + ($part % 12))
          MaxTempPosition = 11.0
          MinTempPosition = 3.0
          MaxTempChannel = 1
          MinTempChannel = 1
          timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
        }
        $json = $bodyObj | ConvertTo-Json -Depth 8 -Compress
        $resp = Invoke-WebRequest -Uri $measureUrl -Method POST -ContentType "application/json" -Body $json -TimeoutSec $timeoutSec
        $respBody = $resp.Content
        $bizOk = $false
        if (-not [string]::IsNullOrWhiteSpace($respBody)) {
          try {
            $obj = $respBody | ConvertFrom-Json
            if (($null -ne $obj.success -and [bool]$obj.success) -or ($null -ne $obj.code -and [int]$obj.code -eq 200)) { $bizOk = $true }
          } catch {
            $bizOk = ($resp.StatusCode -eq 200)
          }
        } else {
          $bizOk = ($resp.StatusCode -eq 200)
        }
        return [pscustomobject]@{
          ok = (($resp.StatusCode -eq 200) -and $bizOk)
          index = $index
          status = [int]$resp.StatusCode
          body = $respBody
        }
      } catch {
        $detail = $_.Exception.Message
        try {
          if ($_.ErrorDetails -and $_.ErrorDetails.Message) { $detail = $_.ErrorDetails.Message }
        } catch { }
        return [pscustomobject]@{
          ok = $false
          index = $index
          status = 0
          body = $detail
        }
      }
    } -ArgumentList $index, $PartitionCount, $IotCode, $measureUrl, $HttpTimeoutSec
  }

  while ($true) {
    $running = (Get-Job -State Running | Where-Object { $jobs -contains $_ }).Count
    $done = (Get-Job | Where-Object { $jobs -contains $_ -and $_.State -ne "Running" }).Count
    Write-Host ("[HEARTBEAT] batch={0}/{1} running={2} done={3}" -f ($batch + 1), $batchCount, $running, $done) -ForegroundColor DarkCyan
    if ($running -eq 0) { break }
    Start-Sleep -Seconds 2
  }

  $results = Receive-Job -Job $jobs -Wait -AutoRemoveJob
  foreach ($r in $results) {
    if ($r.ok) {
      $pOk++
    } else {
      $pFail++
      if ($printedError -lt $MaxErrorPrint) {
        $printedError++
        $msg = $r.body
        if ([string]::IsNullOrWhiteSpace($msg)) { $msg = "request failed" }
        if ($msg.Length -gt 400) { $msg = $msg.Substring(0, 400) }
        Write-Host ("[PRESSURE_FAIL] idx={0} status={1} msg={2}" -f $r.index, $r.status, $msg) -ForegroundColor Red
      }
    }
  }

  $batchSw.Stop()
  $doneTotal = [math]::Min(($batch + 1) * $PressureConcurrency, $PressureTotal)
  $successRate = 0
  if (($pOk + $pFail) -gt 0) { $successRate = [math]::Round(($pOk * 100.0 / ($pOk + $pFail)), 2) }
  $qps = [math]::Round($doneTotal / [math]::Max(1, $sw.Elapsed.TotalSeconds), 2)
  Write-Host ("[PRESSURE_PROGRESS] batch={0}/{1} done={2}/{3} ok={4} fail={5} successRate={6}% batchCostMs={7} qps={8}" -f ($batch + 1), $batchCount, $doneTotal, $PressureTotal, $pOk, $pFail, $successRate, $batchSw.ElapsedMilliseconds, $qps) -ForegroundColor Yellow
}

$sw.Stop()
Assert-Stage -Name "pressure_test" -Condition ($pFail -eq 0) -PassMessage ("all {0} requests succeeded" -f $PressureTotal) -FailMessage ("failed={0}" -f $pFail)

Write-Stage "Stage 9 - filter query checks"
$q1 = Query-AlarmsRaw -AlarmTypeBig "0" -Status "0" -PageNo 1 -PageSize 50
Assert-Stage -Name "filter_status_alarmTypeBig" -Condition $q1.ok -PassMessage "query ok" -FailMessage "query failed"

$q2 = Query-AlarmsRaw -AlarmTypeBig "0" -AreaName "area" -PageNo 1 -PageSize 50
Assert-Stage -Name "filter_areaName_like" -Condition $q2.ok -PassMessage "areaName fuzzy query ok" -FailMessage "areaName fuzzy query failed"

Write-Host ""
Write-Host "================ SUMMARY ================" -ForegroundColor Cyan
Write-Host ("apiTotal={0} apiOk={1} apiFail={2}" -f $apiTotal, $apiOk, $apiFail)
Write-Host ("stagePass={0} stageFail={1}" -f $stagePass, $stageFail)
Write-Host ("pressureOk={0} pressureFail={1}" -f $pOk, $pFail)

if ($stageFail -eq 0) {
  Write-Host "[RESULT] PASS" -ForegroundColor Green
  exit 0
} else {
  Write-Host "[RESULT] FAIL" -ForegroundColor Red
  exit 1
}
