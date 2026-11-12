[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Python,
    [Parameter(Mandatory)][string]$Codex
)
$ErrorActionPreference = 'Stop'
$taskName = 'CamNecT-AutoTagBot-v1'
$homeDir = Join-Path $env:LOCALAPPDATA 'CamNecT\AutoTagBot'
$release = Join-Path $homeDir '1.0'
$state = Join-Path $homeDir 'state-4084-v1'
$runtime = (Get-Item -LiteralPath $Python).FullName
$pythonw = (Get-Item -LiteralPath (Join-Path (Split-Path $runtime) 'pythonw.exe')).FullName
$judge = (Get-Item -LiteralPath $Codex).FullName
$principal = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
New-Item -ItemType Directory -Path $release,$state -Force | Out-Null
$files = @('worker.py','votes.py','source4084.py','source-libs.zip','score.py')
$files += Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'frozen') -Recurse -File |
    Where-Object { $_.Extension -ne '.pyc' -and $_.Name -ne '.gitattributes' } |
    ForEach-Object { $_.FullName.Substring($PSScriptRoot.Length + 1) }
foreach ($file in $files) {
    $original = Join-Path $PSScriptRoot $file
    $destination = Join-Path $release $file
    if (Test-Path -LiteralPath $destination) {
        if ((Get-FileHash -LiteralPath $original).Hash -ne (Get-FileHash -LiteralPath $destination).Hash) {
            throw "Installed 1.0 file differs: $file"
        }
    } else {
        New-Item -ItemType Directory -Force -Path (Split-Path $destination) | Out-Null
        Copy-Item -LiteralPath $original -Destination $destination
    }
}
$worker = Join-Path $release 'worker.py'
$ledger = Join-Path $state 'candidates.sqlite3'
# A failed first network request may have created only an empty database.
$initialized = $false
if (Test-Path -LiteralPath $ledger) {
    $null = & $runtime -c "import sqlite3,sys; d=sqlite3.connect(sys.argv[1]); sys.exit(0 if d.execute('SELECT 1 FROM meta WHERE key=?',('started',)).fetchone() else 1)" $ledger
    $initialized = $LASTEXITCODE -eq 0
}
if (!$initialized) {
    & $runtime -X utf8 $worker init --state $state
    if ($LASTEXITCODE -ne 0) { throw '4084 baseline could not be initialized' }
}
$arguments = '-X utf8 "{0}" poll --state "{1}" --codex "{2}"' -f $worker,$state,$judge
$action = New-ScheduledTaskAction -Execute $pythonw -Argument $arguments -WorkingDirectory $release
$timer = New-ScheduledTaskTrigger -Once -At (Get-Date).AddMinutes(1) -RepetitionInterval (New-TimeSpan -Minutes 30)
$login = New-ScheduledTaskTrigger -AtLogOn -User $principal
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -MultipleInstances IgnoreNew -ExecutionTimeLimit (New-TimeSpan -Hours 2)
$user = New-ScheduledTaskPrincipal -UserId $principal -LogonType Interactive -RunLevel Limited
Register-ScheduledTask -TaskName $taskName -Action $action -Trigger @($timer,$login) -Settings $settings -Principal $user -Description 'Read public 4084 notices; collect three Astra votes into the private local candidate ledger.' -Force | Out-Null
Start-ScheduledTask -TaskName $taskName
Get-ScheduledTask -TaskName $taskName | Select-Object TaskName,State
