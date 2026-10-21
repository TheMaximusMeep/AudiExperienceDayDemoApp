# Shared helpers for the demo scripts. Dot-source it: . "$PSScriptRoot\_common.ps1"

$ErrorActionPreference = 'Stop'

$Package = 'technology.cariad.partnershowcase'
$AppDir = Join-Path (Split-Path $PSScriptRoot -Parent) 'demo-app'

# Vehicle HAL property ids (VehiclePropertyIds) and gear values (VehicleGear).
$GEAR_SELECTION = '0x11400400'
$PERF_VEHICLE_SPEED = '0x11600207'
$PARKING_BRAKE_ON = '0x11200402'
$GEAR_PARK = 4
$GEAR_DRIVE = 8

# The in-car App Store on Audi vehicles, and its "car parked" broadcast.
$AudiStore = 'com.example.carstore'
$ACTION_CAR_PARKED = 'com.example.carstore.action.CAR_PARKED'

function Step([string] $text) {
    Write-Host ''
    Write-Host "==> $text" -ForegroundColor Green
}

function Use-Jdk {
    if (-not $env:JAVA_HOME) {
        $studioJbr = 'C:\Tools\AndroidStudio\android-studio-2024.1.1.12-windows\android-studio\jbr'
        if (Test-Path $studioJbr) { $env:JAVA_HOME = $studioJbr }
    }
}

# One emulator or car at a time; pass -Serial when more are connected.
function Get-Serial([string] $serial) {
    if ($serial) { return $serial }
    $devices = @(adb devices | Select-String '\tdevice$' | ForEach-Object { ($_ -split '\t')[0] })
    if ($devices.Count -eq 0) { throw 'No emulator or car connected (adb devices is empty).' }
    if ($devices.Count -gt 1) { throw "More than one device connected: $($devices -join ', '). Pass -Serial." }
    return $devices[0]
}

function Invoke-Adb([string] $serial) {
    adb.exe -s $serial @args
    if ($LASTEXITCODE -ne 0) { throw "adb $($args -join ' ') failed" }
}

# The emulator keeps reporting its own values (speed 0, from Extended controls > Car data) several
# times a second. An event stamped 2000 s in the future wins: the car service drops every older
# event after it. That is the trick of "cmd car_service emulate-driving-state" too.
function Set-Vhal([string] $serial, [string] $property, [string] $value) {
    Invoke-Adb $serial shell cmd car_service inject-vhal-event $property $value -t 2000 | Out-Null
}
