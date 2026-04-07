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
  [int]$MixMeasurePercent = 90
)

if ($Total -lt 1) { throw "Total must be >= 1" }
if ($Concurrency -lt 1) { throw "Concurrency must be >= 1" }
if ($PartitionCount -lt 1) { throw "PartitionCount must be >= 1" }
if ($MixMeasurePercent -lt 0 -or $MixMeasurePercent -gt 100) { throw "MixMeasurePercent must be between 0 and 100" }

$measurePath = "$BaseUrl/shaft/iot/reports/measure"
$alarmPath = "$BaseUrl/shaft/iot/reports/alarm"

$ok = 0
$fail = 0
$measureCount = 0
$alarmCount = 0

$batchCount = [math]::Ceiling($Total / $Concurrency)
$sw = [System.Diagnostics.Stopwatch]::StartNew()

Write-Host "start pressure test..."
Write-Host "baseUrl=$BaseUrl mode=$Mode total=$Total concurrency=$Concurrency partitionCount=$PartitionCount timeoutSec=$TimeoutSec"

for ($batch = 0; $batch -lt $batchCount; $batch++) {
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
        if ($resp.StatusCode -eq 200) {
          return "OK:$realMode"
        }
        return "FAIL:$realMode"
      }
      catch {
        if ($mode -eq "mix") { return "FAIL:unknown" }
        return "FAIL:$mode"
      }
    } -ArgumentList $index, $Mode, $MixMeasurePercent, $measurePath, $alarmPath, $IotCode, $PartitionCount, $MaxTemp, $MinTemp, $AvgTemp, $TimeoutSec
  }

  $results = Receive-Job -Job $jobs -Wait -AutoRemoveJob
  foreach ($r in $results) {
    if ($r -like "OK:*") {
      $ok++
      if ($r -eq "OK:measure") { $measureCount++ }
      elseif ($r -eq "OK:alarm") { $alarmCount++ }
    } else {
      $fail++
      if ($r -eq "FAIL:measure") { $measureCount++ }
      elseif ($r -eq "FAIL:alarm") { $alarmCount++ }
    }
  }

  Write-Host ("batch={0}/{1} ok={2} fail={3}" -f ($batch + 1), $batchCount, $ok, $fail)
}

$sw.Stop()
$qps = [math]::Round($Total / [math]::Max(1, $sw.Elapsed.TotalSeconds), 2)

Write-Host ""
Write-Host "done."
Write-Host ("elapsedSec={0}" -f [math]::Round($sw.Elapsed.TotalSeconds, 2))
Write-Host ("total={0} ok={1} fail={2}" -f $Total, $ok, $fail)
Write-Host ("sentMeasure={0} sentAlarm={1}" -f $measureCount, $alarmCount)
Write-Host ("approxQps={0}" -f $qps)

