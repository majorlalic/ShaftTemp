param(
  [string]$BaseUrl = "http://localhost:8090/api",
  [string]$IotCode = "shaft-dev-001",
  [int]$PartitionCount = 50,
  [int]$PressureTotal = 3000,
  [int]$PressureConcurrency = 30,
  [int]$HttpTimeoutSec = 10,
  [int]$OfflineWaitSec = 75,
  [switch]$SkipOfflineTest = $false
)

$ErrorActionPreference = "Stop"

if ($PartitionCount -lt 1) { throw "PartitionCount must be >= 1" }
if ($PressureTotal -lt 1) { throw "PressureTotal must be >= 1" }
if ($PressureConcurrency -lt 1) { throw "PressureConcurrency must be >= 1" }

$measureUrl = "$BaseUrl/shaft/iot/reports/measure"
$alarmUrl = "$BaseUrl/shaft/iot/reports/alarm"
$alarmListUrl = "$BaseUrl/shaft/alarm/list"

$global:stats = [ordered]@{
  total = 0
  ok = 0
  fail = 0
  stagePass = 0
  stageFail = 0
}

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
  $global:stats.total++
  try {
    $bodyText = $null
    if ($null -ne $Body) {
      $bodyText = $Body | ConvertTo-Json -Depth 8 -Compress
    }
    $resp = Invoke-WebRequest -Uri $Url -Method $Method -ContentType "application/json" -Body $bodyText -TimeoutSec $TimeoutSec
    $content = $resp.Content
    $obj = $null
    try {
      if (-not [string]::IsNullOrWhiteSpace($content)) {
        $obj = $content | ConvertFrom-Json
      }
    } catch {
      $obj = $null
    }
    $bizOk = $false
    if ($null -ne $obj) {
      if ($null -ne $obj.success -and [bool]$obj.success) { $bizOk = $true }
      elseif ($null -ne $obj.code -and [int]$obj.code -eq 200) { $bizOk = $true }
      elseif ($null -eq $obj.success -and $null -eq $obj.code -and $resp.StatusCode -eq 200) { $bizOk = $true }
    } else {
      $bizOk = ($resp.StatusCode -eq 200)
    }
    if ($resp.StatusCode -eq 200 -and $bizOk) {
      $global:stats.ok++
    } else {
      $global:stats.fail++
      Write-Host ("[API_FAIL] {0} {1} http={2}" -f $Method, $Url, $resp.StatusCode) -ForegroundColor Red
      if (-not [string]::IsNullOrWhiteSpace($content)) {
        $print = if ($content.Length -gt 500) { $content.Substring(0, 500) } else { $content }
        Write-Host ("[API_FAIL_BODY] {0}" -f $print) -ForegroundColor DarkRed
      }
    }
    return [pscustomobject]@{
      ok = ($resp.StatusCode -eq 200 -and $bizOk)
      httpStatus = [int]$resp.StatusCode
      bodyText = $content
      body = $obj
    }
  } catch {
    $global:stats.fail++
    $msg = $_.Exception.Message
    Write-Host ("[API_ERROR] {0} {1} msg={2}" -f $Method, $Url, $msg) -ForegroundColor Red
    return [pscustomobject]@{
      ok = $false
      httpStatus = 0
      bodyText = $msg
      body = $null
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
    MaxTempPosition = 10.0 + $PartitionId
    MinTempPosition = 2.0 + $PartitionId
    MaxTempChannel = 1
    MinTempChannel = 2
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

function Query-Alarms {
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
  $q += "pageNo=$PageNo"
  $q += "pageSize=$PageSize"
  if (-not [string]::IsNullOrWhiteSpace($Status)) { $q += "status=$([uri]::EscapeDataString($Status))" }
  if (-not [string]::IsNullOrWhiteSpace($AlarmTypeBig)) { $q += "alarmTypeBig=$([uri]::EscapeDataString($AlarmTypeBig))" }
  if (-not [string]::IsNullOrWhiteSpace($AreaName)) { $q += "areaName=$([uri]::EscapeDataString($AreaName))" }
  $q += "startTime=$([uri]::EscapeDataString($start))"
  $q += "endTime=$([uri]::EscapeDataString($end))"
  $url = "$alarmListUrl?" + ($q -join "&")
  return Invoke-JsonApi -Method "GET" -Url $url -TimeoutSec $HttpTimeoutSec
}

function Assert-Stage {
  param(
    [string]$Name,
    [bool]$Condition,
    [string]$PassMessage,
    [string]$FailMessage
  )
  if ($Condition) {
    $global:stats.stagePass++
    Write-Host ("[PASS] {0} - {1}" -f $Name, $PassMessage) -ForegroundColor Green
  } else {
    $global:stats.stageFail++
    Write-Host ("[FAIL] {0} - {1}" -f $Name, $FailMessage) -ForegroundColor Red
  }
}

Write-Host "start full e2e test..."
Write-Host ("baseUrl={0} iotCode={1} partitionCount={2} pressureTotal={3} concurrency={4}" -f $BaseUrl, $IotCode, $PartitionCount, $PressureTotal, $PressureConcurrency)

Write-Stage "Stage 1 - Baseline measure"
$r1 = Invoke-JsonApi -Method "POST" -Url $measureUrl -Body (New-MeasureBody -PartitionId 1 -MaxTemp 60 -MinTemp 50 -AvgTemp 55) -TimeoutSec $HttpTimeoutSec
Assert-Stage -Name "baseline_measure" -Condition $r1.ok -PassMessage "measure accepted" -FailMessage "measure request failed"

Write-Stage "Stage 2 - TEMP_THRESHOLD trigger"
$r2 = Invoke-JsonApi -Method "POST" -Url $measureUrl -Body (New-MeasureBody -PartitionId 1 -MaxTemp 88 -MinTemp 60 -AvgTemp 73) -TimeoutSec $HttpTimeoutSec
Assert-Stage -Name "temp_threshold_push" -Condition $r2.ok -PassMessage "high temp pushed" -FailMessage "high temp push failed"

Write-Stage "Stage 3 - TEMP_DIFFERENCE trigger"
$r3 = Invoke-JsonApi -Method "POST" -Url $measureUrl -Body (New-MeasureBody -PartitionId 2 -MaxTemp 92 -MinTemp 60 -AvgTemp 76) -TimeoutSec $HttpTimeoutSec
Assert-Stage -Name "temp_difference_push" -Condition $r3.ok -PassMessage "diff temp pushed" -FailMessage "diff temp push failed"

Write-Stage "Stage 4 - TEMP_RISE_RATE trigger"
$r4a = Invoke-JsonApi -Method "POST" -Url $measureUrl -Body (New-MeasureBody -PartitionId 3 -MaxTemp 52 -MinTemp 45 -AvgTemp 48) -TimeoutSec $HttpTimeoutSec
Start-Sleep -Seconds 1
$r4b = Invoke-JsonApi -Method "POST" -Url $measureUrl -Body (New-MeasureBody -PartitionId 3 -MaxTemp 72 -MinTemp 46 -AvgTemp 58) -TimeoutSec $HttpTimeoutSec
Assert-Stage -Name "rise_rate_push" -Condition ($r4a.ok -and $r4b.ok) -PassMessage "base+rise pushed" -FailMessage "rise rate push failed"

Write-Stage "Stage 5 - Alarm status push path (alarm_raw)"
$r5 = Invoke-JsonApi -Method "POST" -Url $alarmUrl -Body (New-AlarmBody -PartitionId 4 -AlarmStatus $true -FaultStatus $true) -TimeoutSec $HttpTimeoutSec
Assert-Stage -Name "alarm_raw_push" -Condition $r5.ok -PassMessage "alarm status accepted" -FailMessage "alarm status push failed"
Write-Host "[INFO] 当前代码路径中 /reports/alarm 主要写 alarm_raw，不直接新建 alarm 主单。"

Write-Stage "Stage 6 - Merge behavior"
for ($i = 0; $i -lt 4; $i++) {
  $tmp = Invoke-JsonApi -Method "POST" -Url $measureUrl -Body (New-MeasureBody -PartitionId 1 -MaxTemp 89 -MinTemp 61 -AvgTemp 74) -TimeoutSec $HttpTimeoutSec
  if (-not $tmp.ok) {
    Write-Host ("[WARN] merge push index={0} failed" -f $i) -ForegroundColor Yellow
  }
}
Start-Sleep -Seconds 2
$listResp = Query-Alarms -AlarmTypeBig "0" -PageNo 1 -PageSize 200
$mergeOk = $false
if ($listResp.ok -and $null -ne $listResp.body -and $null -ne $listResp.body.data -and $null -ne $listResp.body.data.list) {
  $matched = $listResp.body.data.list | Where-Object { $_.alarmType -eq "TEMP_THRESHOLD" -and $_.mergeCount -ge 2 }
  if ($matched -and $matched.Count -gt 0) { $mergeOk = $true }
}
Assert-Stage -Name "merge_check" -Condition $mergeOk -PassMessage "mergeCount observed >= 2" -FailMessage "no merged threshold alarm found"

if (-not $SkipOfflineTest) {
  Write-Stage "Stage 7 - DEVICE_OFFLINE (inspection)"
  Write-Host ("waiting {0}s for offline inspection cycle..." -f $OfflineWaitSec) -ForegroundColor Yellow
  for ($t = 1; $t -le $OfflineWaitSec; $t++) {
    if (($t % 5) -eq 0) {
      Write-Host ("offline-wait progress: {0}/{1}s" -f $t, $OfflineWaitSec) -ForegroundColor DarkYellow
    }
    Start-Sleep -Seconds 1
  }
  $offResp = Query-Alarms -AlarmTypeBig "0" -PageNo 1 -PageSize 200
  $offlineOk = $false
  if ($offResp.ok -and $null -ne $offResp.body -and $null -ne $offResp.body.data -and $null -ne $offResp.body.data.list) {
    $off = $offResp.body.data.list | Where-Object { $_.alarmType -eq "DEVICE_OFFLINE" }
    if ($off -and $off.Count -gt 0) { $offlineOk = $true }
  }
  Assert-Stage -Name "offline_check" -Condition $offlineOk -PassMessage "offline alarm observed" -FailMessage "offline alarm not found in list"
}

Write-Stage "Stage 8 - Pressure test (measure)"
$batchCount = [math]::Ceiling($PressureTotal / $PressureConcurrency)
$pressureOk = 0
$pressureFail = 0
$pressureStart = [DateTime]::Now

for ($batch = 0; $batch -lt $batchCount; $batch++) {
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
        $body = @{
          topic = "$ref/Measure"
          iotCode = $iotCode
          PartitionId = $part
          IedFullPath = "/IED/$iotCode"
          dataReference = $ref
          MaxTemp = (65 + ($part % 20))
          MinTemp = (50 + ($part % 10))
          AvgTemp = (58 + ($part % 12))
          MaxTempPosition = 10.0 + $part
          MinTempPosition = 2.0 + $part
          MaxTempChannel = 1
          MinTempChannel = 2
          timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
        } | ConvertTo-Json -Depth 8 -Compress
        $resp = Invoke-WebRequest -Uri $measureUrl -Method POST -ContentType "application/json" -Body $body -TimeoutSec $timeoutSec
        $ok = $false
        try {
          $obj = $resp.Content | ConvertFrom-Json
          if (($null -ne $obj.success -and [bool]$obj.success) -or ($null -ne $obj.code -and [int]$obj.code -eq 200)) { $ok = $true }
        } catch {
          $ok = ($resp.StatusCode -eq 200)
        }
        return [pscustomobject]@{
          ok = $ok
          status = [int]$resp.StatusCode
          body = $resp.Content
          index = $index
        }
      } catch {
        return [pscustomobject]@{
          ok = $false
          status = 0
          body = $_.Exception.Message
          index = $index
        }
      }
    } -ArgumentList $index, $PartitionCount, $IotCode, $measureUrl, $HttpTimeoutSec
  }

  # heartbeat loop while waiting jobs
  while (($jobs | Where-Object { $_.State -eq "Running" }).Count -gt 0) {
    $done = ($jobs | Where-Object { $_.State -ne "Running" }).Count
    Write-Host ("[PRESSURE_HEARTBEAT] batch={0}/{1} running={2} done={3}" -f ($batch + 1), $batchCount, ($jobs.Count - $done), $done) -ForegroundColor DarkCyan
    Start-Sleep -Seconds 2
  }

  $results = Receive-Job -Job $jobs -Wait -AutoRemoveJob
  foreach ($r in $results) {
    if ($r.ok) {
      $pressureOk++
    } else {
      $pressureFail++
      if ($pressureFail -le 20) {
        $msg = if ($null -eq $r.body) { "" } else { [string]$r.body }
        if ($msg.Length -gt 300) { $msg = $msg.Substring(0, 300) }
        Write-Host ("[PRESSURE_FAIL] idx={0} status={1} msg={2}" -f $r.index, $r.status, $msg) -ForegroundColor Red
      }
    }
  }

  $doneTotal = [math]::Min(($batch + 1) * $PressureConcurrency, $PressureTotal)
  $elapsed = ([DateTime]::Now - $pressureStart).TotalSeconds
  $qps = [math]::Round($doneTotal / [math]::Max(1, $elapsed), 2)
  $sr = [math]::Round(($pressureOk * 100.0) / [math]::Max(1, ($pressureOk + $pressureFail)), 2)
  Write-Host ("[PRESSURE_PROGRESS] batch={0}/{1} done={2}/{3} ok={4} fail={5} successRate={6}% qps={7}" -f ($batch + 1), $batchCount, $doneTotal, $PressureTotal, $pressureOk, $pressureFail, $sr, $qps) -ForegroundColor Yellow
}

$pressurePass = ($pressureFail -eq 0)
Assert-Stage -Name "pressure_test" -Condition $pressurePass -PassMessage ("all {0} requests succeeded" -f $PressureTotal) -FailMessage ("failed={0}" -f $pressureFail)

Write-Stage "Stage 9 - Filter query checks"
$q1 = Query-Alarms -AlarmTypeBig "0" -Status "0" -PageNo 1 -PageSize 50
Assert-Stage -Name "filter_status_alarmTypeBig" -Condition $q1.ok -PassMessage "query ok" -FailMessage "query failed"

$q2 = Query-Alarms -AlarmTypeBig "0" -AreaName "区域" -PageNo 1 -PageSize 50
Assert-Stage -Name "filter_areaName_like" -Condition $q2.ok -PassMessage "areaName fuzzy query ok" -FailMessage "areaName fuzzy query failed"

Write-Host ""
Write-Host "================ SUMMARY ================" -ForegroundColor Cyan
Write-Host ("apiTotal={0} apiOk={1} apiFail={2}" -f $global:stats.total, $global:stats.ok, $global:stats.fail)
Write-Host ("stagePass={0} stageFail={1}" -f $global:stats.stagePass, $global:stats.stageFail)
if ($global:stats.stageFail -eq 0) {
  Write-Host "[RESULT] PASS" -ForegroundColor Green
  exit 0
} else {
  Write-Host "[RESULT] FAIL" -ForegroundColor Red
  exit 1
}
