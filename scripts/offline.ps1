<#
.SYNOPSIS
  A tunnel or an underground car park: the car loses its internet connection. The app says so,
  keeps showing what it has, and recovers on its own. Run with -Back to come out of the tunnel.

.EXAMPLE
  .\scripts\offline.ps1
  .\scripts\offline.ps1 -Back
#>
param(
    [switch] $Back,
    [string] $Serial
)
. "$PSScriptRoot\_common.ps1"

$serial = Get-Serial $Serial
if ($Back) {
    Step 'Out of the tunnel: online again'
    Invoke-Adb $serial shell cmd connectivity airplane-mode disable | Out-Null
} else {
    Step 'Into the tunnel: offline'
    Invoke-Adb $serial shell cmd connectivity airplane-mode enable | Out-Null
}
'Done.'
