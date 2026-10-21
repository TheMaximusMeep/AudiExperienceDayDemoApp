<#
.SYNOPSIS
  Back to the start of the demo: car parked, app uninstalled (so version 1 installs fresh).
#>
param([string] $Serial)
. "$PSScriptRoot\_common.ps1"

$serial = Get-Serial $Serial
Step 'Car: parked'
Set-Vhal $serial $PERF_VEHICLE_SPEED 0
Set-Vhal $serial $GEAR_SELECTION $GEAR_PARK
Set-Vhal $serial $PARKING_BRAKE_ON true

Step "Uninstall $Package"
adb -s $serial uninstall $Package | Out-Null
'Clean. Start with .\scripts\build-install.ps1'
