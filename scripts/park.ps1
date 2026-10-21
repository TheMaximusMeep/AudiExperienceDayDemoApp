<#
.SYNOPSIS
  Park the car: speed 0, gear P, parking brake on, and the store's "car parked" event.

  On an image with the App Store (an Audi emulator image, the car) the store's own
  CustomBroadcastService sees the drive and the P and sends the event itself; this script only
  waits for it. Without a store (vanilla AAOS emulator), or if the store stays silent, the script
  plays the store and sends the very same broadcast, with the Audi Forum as position.

.EXAMPLE
  .\scripts\drive.ps1; .\scripts\park.ps1     # real store event (needs the drive before)
  .\scripts\park.ps1 -Simulate                 # always the simulated event
  .\scripts\park.ps1 -NoEvent                  # only park
#>
param(
    [switch] $NoEvent,
    [switch] $Simulate,
    [string] $Serial
)
. "$PSScriptRoot\_common.ps1"

$serial = Get-Serial $Serial
$hasStore = (adb -s $serial shell pm list packages $AudiStore) -match $AudiStore
if ($hasStore -and -not $NoEvent -and -not $Simulate) { adb -s $serial logcat -c }

Step 'Park: 0 km/h, gear P, parking brake on'
Set-Vhal $serial $PERF_VEHICLE_SPEED 0
Set-Vhal $serial $GEAR_SELECTION $GEAR_PARK
Set-Vhal $serial $PARKING_BRAKE_ON true
if ($NoEvent) { 'Parked.'; return }

if ($hasStore -and -not $Simulate) {
    Step 'Waiting for the App Store''s own "car parked" event'
    for ($i = 0; $i -lt 10; $i++) {
        Start-Sleep -Milliseconds 700
        $sent = adb -s $serial logcat -d | Select-String "Sending parking broadcast to $Package"
        if ($sent) { 'The store sent it. Parked.'; return }
    }
    'The store stayed silent (no drive before?), so the script plays the store.'
}

Step 'App Store -> partner app: ACTION_CAR_PARKED (simulated)'
# Audi Forum Ingolstadt — the store's own fallback position.
Invoke-Adb $serial shell am broadcast -a $ACTION_CAR_PARKED -p $Package `
    --es latitude 48.7835303 --es longitude 11.4133578 --es senderpackagename $AudiStore | Out-Null
'Parked.'
