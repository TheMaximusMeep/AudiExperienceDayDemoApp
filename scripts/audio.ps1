<#
.SYNOPSIS
  Another audio source speaks in the cabin: the voice assistant, a navigation prompt or a call.
  It asks the car for audio focus for 5 seconds; the trailer pauses (or gets quieter) and comes
  back on its own when the focus returns.

.EXAMPLE
  .\scripts\audio.ps1                      # the voice assistant: trailer pauses, then resumes
  .\scripts\audio.ps1 -Source navigation   # a navigation prompt: media may duck
  .\scripts\audio.ps1 -Source call
#>
param(
    [ValidateSet('assistant', 'navigation', 'call')] [string] $Source = 'assistant',
    [string] $Serial
)
. "$PSScriptRoot\_common.ps1"

$serial = Get-Serial $Serial
Step "Audio focus: $Source speaks for 5 seconds"
Invoke-Adb $serial shell am broadcast -a technology.cariad.partnershowcase.DEMO_AUDIO_SOURCE -p $Package --es source $Source | Out-Null
'Done.'
