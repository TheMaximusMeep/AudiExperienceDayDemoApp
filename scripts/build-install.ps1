<#
.SYNOPSIS
  Live demo step "Build": compile the app, install it on the emulator (or car), start it.

.EXAMPLE
  .\scripts\build-install.ps1                 # version 1, installed like the store installs it
  .\scripts\build-install.ps1 -Version 2      # after the live change: installs as an update
  .\scripts\build-install.ps1 -Sideload       # plain "adb install" (header: "installed by adb")
#>
param(
    [int] $Version = 1,
    # Install as a plain adb sideload instead of attributing the install to the App Store.
    [switch] $Sideload,
    [string] $Serial
)
. "$PSScriptRoot\_common.ps1"

$serial = Get-Serial $Serial
$versionName = "1.$($Version - 1).0"
$apk = Join-Path $AppDir "app\build\outputs\apk\release\partner-showcase-$versionName-vc$Version-release.apk"

Step "Build release APK  v$versionName (versionCode $Version)"
Use-Jdk
Push-Location $AppDir
try {
    .\gradlew.bat -q --offline ":app:assembleRelease" "-Pshowcase.versionCode=$Version" "-Pshowcase.versionName=$versionName"
    if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed' }
} finally { Pop-Location }
'{0}  ({1:N1} MB)' -f (Split-Path $apk -Leaf), ((Get-Item $apk).Length / 1MB)

if ($Sideload) {
    Step "Install on $serial as a plain sideload"
    Invoke-Adb $serial install -r $apk
} else {
    # -i records the install as coming from the store, as it does in the car (the app's header
    # shows the installer). Production images can restrict "distraction optimized" to apps from
    # trusted installers; the userdebug emulator does not check.
    # The App Store on this device, if there is one.
    $packages = adb -s $serial shell pm list packages
    $installer = @($AudiStore, 'com.android.vending') |
        Where-Object { $packages -contains "package:$_" } | Select-Object -First 1
    if (-not $installer) { throw 'No App Store on this device to attribute the install to; use -Sideload.' }
    Step "Install on $serial (installer: $installer)"
    Invoke-Adb $serial install -r -i $installer $apk
}

Step 'Start it'
Invoke-Adb $serial shell am start -n "$Package/.MainActivity" | Out-Null
'Running. Next: .\scripts\drive.ps1, .\scripts\park.ps1'
