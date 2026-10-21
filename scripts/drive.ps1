<#
.SYNOPSIS
  Pretend the car drives off: gear D, parking brake off, speed in km/h.
  The car turns this into UX restrictions; the app reacts (video stops, keyboard closes, text
  gets shorter). Park again with .\scripts\park.ps1.

.EXAMPLE
  .\scripts\drive.ps1            # 50 km/h: "moving", fully restricted
  .\scripts\drive.ps1 -Kmh 0     # standing in D: "idling", no video only
#>
param(
    [double] $Kmh = 50,
    [string] $Serial
)
. "$PSScriptRoot\_common.ps1"

$serial = Get-Serial $Serial
if ($serial -like 'emulator-*') {
    # The emulator's GPS stands at 0/0 by default; put the car at the Audi Forum Ingolstadt.
    adb -s $serial emu geo fix 11.4133578 48.7835303 | Out-Null
}
Step "Drive: gear D, $Kmh km/h"
$metersPerSecond = [string]::Format([cultureinfo]::InvariantCulture, '{0:0.##}', $Kmh / 3.6)
Set-Vhal $serial $PARKING_BRAKE_ON false
Set-Vhal $serial $GEAR_SELECTION $GEAR_DRIVE
Set-Vhal $serial $PERF_VEHICLE_SPEED $metersPerSecond
'Driving. Park again with .\scripts\park.ps1'
