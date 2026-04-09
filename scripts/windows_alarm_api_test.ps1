param(
  [string]$BaseUrl = "http://localhost:8090/api",
  [string]$IotCode = "shaft-dev-001",
  [int]$TotalMeasure = 200,
  [int]$PartitionCount = 50,
  [int]$TimeoutSec = 10,
  [int]$PageNo = 1,
  [int]$PageSize = 20,
  [string]$AreaName = "",
  [string]$Status = "",
  [string]$AlarmTypeBig = "0",
  [string]$StartTime = "",
  [string]$EndTime = ""
)

if ($TotalMeasure -lt 1) { throw "TotalMeasure must be >= 1" }
if ($PartitionCount -lt 1) { throw "PartitionCount must be >= 1" }

$measureUrl = "$BaseUrl/shaft/iot/reports/measure"
$alarmListUrl = "$BaseUrl/shaft/alarm/list"

$ok = 0
$fail = 0
$errorPrint = 0
$maxErrorPrint = 10

Write-Host "=== STEP1: send measure reports ===" -ForegroundColor Cyan
Write-Host "baseUrl=$BaseUrl iotCode=$IotCode total=$TotalMeasure partitionCount=$PartitionCount"

for ($i = 1; $i -le $TotalMeasure; $i++) {
  $part = ($i % $PartitionCount) + 1
  $partStr = "{0:D2}" -f $part
  $ts = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
  $ref = "/TMP/$IotCode" + "_TMP_th$partStr"
  $topic = "$ref/Measure"

  $bodyObj = @{
    topic = $topic
    iotCode = $IotCode
    PartitionId = $part
    IedFullPath = "/IED/$IotCode"
    dataReference = $ref
    MaxTemp = 76.0 + ($part % 5)
    MinTemp = 60.0 + ($part % 3)
    AvgTemp = 68.0 + ($part % 4)
    MaxTempPosition = 10.0 + $part
    MinTempPosition = 2.0 + $part
    MaxTempChannel = 1
    MinTempChannel = 2
    timestamp = $ts
  }

  try {
    $resp = Invoke-WebRequest -Uri $measureUrl -Method POST -ContentType "application/json" -Body ($bodyObj | ConvertTo-Json -Depth 5 -Compress) -TimeoutSec $TimeoutSec
    $isOk = $false
    if ($resp.StatusCode -eq 200) {
      try {
        $obj = $resp.Content | ConvertFrom-Json
        if (($null -ne $obj.success -and [bool]$obj.success) -or ($null -ne $obj.code -and [int]$obj.code -eq 200)) {
          $isOk = $true
        }
      } catch {
        $isOk = $false
      }
    }
    if ($isOk) { $ok++ } else { $fail++ }
    if (-not $isOk -and $errorPrint -lt $maxErrorPrint) {
      $errorPrint++
      Write-Host ("[MEASURE_FAIL] idx={0} status={1} body={2}" -f $i, $resp.StatusCode, $resp.Content) -ForegroundColor Red
    }
  } catch {
    $fail++
    if ($errorPrint -lt $maxErrorPrint) {
      $errorPrint++
      Write-Host ("[MEASURE_ERROR] idx={0} msg={1}" -f $i, $_.Exception.Message) -ForegroundColor Red
    }
  }

  if (($i % 20) -eq 0) {
    Write-Host ("progress {0}/{1} ok={2} fail={3}" -f $i, $TotalMeasure, $ok, $fail) -ForegroundColor Yellow
  }
}

Write-Host ("measure done: ok={0} fail={1}" -f $ok, $fail) -ForegroundColor Green

Write-Host ""
Write-Host "=== STEP2: query alarm list ===" -ForegroundColor Cyan

if ([string]::IsNullOrWhiteSpace($StartTime)) {
  $StartTime = (Get-Date).AddHours(-24).ToString("yyyy-MM-dd HH:mm:ss")
}
if ([string]::IsNullOrWhiteSpace($EndTime)) {
  $EndTime = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
}

$query = @()
$query += "pageNo=$PageNo"
$query += "pageSize=$PageSize"
if (-not [string]::IsNullOrWhiteSpace($AreaName)) { $query += "areaName=$([uri]::EscapeDataString($AreaName))" }
if (-not [string]::IsNullOrWhiteSpace($Status)) { $query += "status=$([uri]::EscapeDataString($Status))" }
if (-not [string]::IsNullOrWhiteSpace($AlarmTypeBig)) { $query += "alarmTypeBig=$([uri]::EscapeDataString($AlarmTypeBig))" }
$query += "startTime=$([uri]::EscapeDataString($StartTime))"
$query += "endTime=$([uri]::EscapeDataString($EndTime))"

$finalUrl = "$alarmListUrl?" + ($query -join "&")
Write-Host "request: $finalUrl"

try {
  $resp = Invoke-WebRequest -Uri $finalUrl -Method GET -TimeoutSec $TimeoutSec
  Write-Host ("httpStatus={0}" -f $resp.StatusCode)
  $obj = $resp.Content | ConvertFrom-Json
  Write-Host ("bizCode={0} success={1} message={2}" -f $obj.code, $obj.success, $obj.message)
  if ($null -ne $obj.data) {
    Write-Host ("pageNo={0} total={1}" -f $obj.data.pageNo, $obj.data.total)
    if ($null -ne $obj.data.list -and $obj.data.list.Count -gt 0) {
      $first = $obj.data.list[0]
      Write-Host ("first.id={0} alarmType={1} alarmTypeBig={2} alarmTypeBigName={3} statusCode={4} areaName={5}" -f $first.id, $first.alarmType, $first.alarmTypeBig, $first.alarmTypeBigName, $first.statusCode, $first.areaName)
    } else {
      Write-Host "list is empty"
    }
  }
} catch {
  Write-Host ("[ALARM_LIST_ERROR] {0}" -f $_.Exception.Message) -ForegroundColor Red
  exit 1
}

Write-Host ""
Write-Host "[DONE] windows_alarm_api_test.ps1 finished" -ForegroundColor Green
