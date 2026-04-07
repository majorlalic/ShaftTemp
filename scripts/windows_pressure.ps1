param(
  [string]$BaseUrl = "http://127.0.0.1:8080",
  [ValidateSet("measure", "alarm", "mix")]
  [string]$Mode = "measure",
  [int]$Total = 5000,
  [int]$Concurrency = 50,
  [int]$PartitionCount = 50,
  [string]$IotCode = "shaft-dev-01",
  [double]$MaxTemp = 82.0,
  [double]$MinTemp = 75.0,
  [double]$AvgTemp = 78.0,
  [int]$TimeoutSec = 10,
  [int]$MixMeasurePercent = 90,
  [int]$MaxErrorPrint = 20
)

if ($Total -lt 1) { throw "Total must be >= 1" }
if ($Concurrency -lt 1) { throw "Concurrency must be >= 1" }
if ($PartitionCount -lt 1) { throw "PartitionCount must be >= 1" }
if ($MixMeasurePercent -lt 0 -or $MixMeasurePercent -gt 100) { throw "MixMeasurePercent must be between 0 and 100" }
if ($MaxErrorPrint -lt 0) { throw "MaxErrorPrint must be >= 0" }

$measurePath = "$BaseUrl/shaft/iot/reports/measure"
$alarmPath = "$BaseUrl/shaft/iot/reports/alarm"

$ok = 0
$fail = 0
$measureCount = 0
$alarmCount = 0
$printedError = 0

$batchCount = [math]::Ceiling($Total / $Concurrency)
$sw = [System.Diagnostics.Stopwatch]::StartNew()

Write-Host "start pressure test..."
Write-Host "baseUrl=$BaseUrl mode=$Mode total=$Total concurrency=$Concurrency partitionCount=$PartitionCount timeoutSec=$TimeoutSec"
Write-Host "maxErrorPrint=$MaxErrorPrint"

for ($batch = 0; $batch -lt $batchCount; $batch++) {
  $batchSw = [System.Diagnostics.Stopwatch]::StartNew()
  $jobs = @()

  for ($i = 0; $i -lt $Concurrency; $i++) {
    $index = $batch * $Concurrency + $i + 1
    if ($index -gt $Total) { break }

    $jobs += Start-Job -ScriptBlock {
      param(
        $index,
        $mode,
        $mixMeasurePercent,
        $measurePath,
        $alarmPath,
        $iotCode,
        $partitionCount,
        $maxTemp,
        $minTemp,
        $avgTemp,
        $timeoutSec
      )

      $requestSw = [System.Diagnostics.Stopwatch]::StartNew()
      try {
        $part = ($index % $partitionCount) + 1
        $partStr = "{0:D2}" -f $part
        $ts = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
        $topicPrefix = "/TMP/$iotCode" + "_TMP_th$partStr"
        $ref = "/TMP/$iotCode" + "_TMP_th$partStr"

        $realMode = $mode
        if ($mode -eq "mix") {
          $rand = Get-Random -Minimum 1 -Maximum 101
          if ($rand -le $mixMeasurePercent) { $realMode = "measure" } else { $realMode = "alarm" }
        }

        if ($realMode -eq "measure") {
          $bodyObj = @{
            topic = "$topicPrefix/Measure"
            iotCode = $iotCode
            PartitionId = $part
            IedFullPath = "/IED/$iotCode"
            dataReference = $ref
            MaxTemp = $maxTemp
            MinTemp = $minTemp
            AvgTemp = $avgTemp
            MaxTempPosition = 11.0
            MinTempPosition = 3.0
            MaxTempChannel = 1
            MinTempChannel = 1
            timestamp = $ts
          }
          $uri = $measurePath
        } else {
          $bodyObj = @{
            topic = "$topicPrefix/Alarm"
            iotCode = $iotCode
            PartitionId = $part
            IedFullPath = "/IED/$iotCode"
            dataReference = $ref
            AlarmStatus = $true
            FaultStatus = $false
            timestamp = $ts
          }
          $uri = $alarmPath
        }

        $json = $bodyObj | ConvertTo-Json -Depth 5 -Compress
        $resp = Invoke-WebRequest -Uri $uri -Method POST -ContentType "application/json" -Body $json -TimeoutSec $timeoutSec
        $requestSw.Stop()
        return [pscustomobject]@{
          index = $index
          mode = $realMode
          partition = $part
          ok = ($resp.StatusCode -eq 200)
          statusCode = [int]$resp.StatusCode
          error = $null
          elapsedMs = $requestSw.ElapsedMilliseconds
        }
      }
      catch {
        $requestSw.Stop()
        $realModeForError = $mode
        if ($mode -eq "mix") { $realModeForError = "unknown" }
        $statusCode = 0
        if ($_.Exception -and $_.Exception.Response -and $_.Exception.Response.StatusCode) {
          try { $statusCode = [int]$_.Exception.Response.StatusCode.value__ } catch { $statusCode = 0 }
        }
        return [pscustomobject]@{
          index = $index
          mode = $realModeForError
          partition = $part
          ok = $false
          statusCode = $statusCode
          error = $_.Exception.Message
          elapsedMs = $requestSw.ElapsedMilliseconds
        }
      }
    } -ArgumentList $index, $Mode, $MixMeasurePercent, $measurePath, $alarmPath, $IotCode, $PartitionCount, $MaxTemp, $MinTemp, $AvgTemp, $TimeoutSec
  }

  $results = Receive-Job -Job $jobs -Wait -AutoRemoveJob
  foreach ($r in $results) {
    if ($r.ok) {
      $ok++
      if ($r.mode -eq "measure") { $measureCount++ }
      elseif ($r.mode -eq "alarm") { $alarmCount++ }
    } else {
      $fail++
      if ($r.mode -eq "measure") { $measureCount++ }
      elseif ($r.mode -eq "alarm") { $alarmCount++ }
      if ($printedError -lt $MaxErrorPrint) {
        $printedError++
        $errorMsg = $r.error
        if ([string]::IsNullOrEmpty($errorMsg)) { $errorMsg = "request failed without exception message" }
        Write-Host ("[ERROR] idx={0} mode={1} part={2} status={3} elapsedMs={4} msg={5}" -f $r.index, $r.mode, $r.partition, $r.statusCode, $r.elapsedMs, $errorMsg) -ForegroundColor Red
      }
    }
  }

  $batchSw.Stop()
  $processed = [math]::Min(($batch + 1) * $Concurrency, $Total)
  $progress = [math]::Round(($processed * 100.0 / $Total), 2)
  $successRate = 0
  if (($ok + $fail) -gt 0) {
    $successRate = [math]::Round(($ok * 100.0 / ($ok + $fail)), 2)
  }
  $currentQps = [math]::Round($processed / [math]::Max(1, $sw.Elapsed.TotalSeconds), 2)
  Write-Host ("[PROGRESS] batch={0}/{1} done={2}/{3}({4}%) ok={5} fail={6} successRate={7}% batchCostMs={8} qps={9}" -f ($batch + 1), $batchCount, $processed, $Total, $progress, $ok, $fail, $successRate, $batchSw.ElapsedMilliseconds, $currentQps) -ForegroundColor Cyan
}

$sw.Stop()
$qps = [math]::Round($Total / [math]::Max(1, $sw.Elapsed.TotalSeconds), 2)
$successRateFinal = 0
if (($ok + $fail) -gt 0) {
  $successRateFinal = [math]::Round(($ok * 100.0 / ($ok + $fail)), 2)
}

Write-Host ""
if ($fail -eq 0) {
  Write-Host "[RESULT] PASS" -ForegroundColor Green
} else {
  Write-Host "[RESULT] FAIL" -ForegroundColor Red
}
Write-Host ("elapsedSec={0}" -f [math]::Round($sw.Elapsed.TotalSeconds, 2))
Write-Host ("total={0} ok={1} fail={2} successRate={3}%" -f $Total, $ok, $fail, $successRateFinal)
Write-Host ("sentMeasure={0} sentAlarm={1}" -f $measureCount, $alarmCount)
Write-Host ("approxQps={0}" -f $qps)
if ($fail -gt 0 -and $printedError -ge $MaxErrorPrint) {
  Write-Host ("error log truncated, printed={0}, totalFail={1}" -f $printedError, $fail) -ForegroundColor Yellow
}
