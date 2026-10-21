<#
.SYNOPSIS
  Plays the store's backend: grant or revoke the "Partner+" licence for this vehicle.
  The app hears it and checks again, like after a purchase in the store.

.EXAMPLE
  .\scripts\licence.ps1            # grant
  .\scripts\licence.ps1 -Revoke
#>
param(
    [switch] $Revoke,
    [string] $Serial
)
. "$PSScriptRoot\_common.ps1"

$serial = Get-Serial $Serial
$active = if ($Revoke) { 'false' } else { 'true' }
Step "Store: Partner+ licence active = $active"
Invoke-Adb $serial shell am broadcast -a technology.cariad.partnershowcase.DEMO_LICENCE -p $Package --ez active $active | Out-Null
'Done.'
