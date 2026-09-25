param(
    [ValidateSet('start', 'stop', 'status')]
    [string]$Action = 'start'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$configRoot = Join-Path $env:LOCALAPPDATA 'CTI_Insights'
$configPath = Join-Path $configRoot 'config.json'
$statePath = Join-Path $configRoot 'runtimeprocesses.json'
$logRoot = Join-Path $configRoot 'runtime'

if (-not (Test-Path -LiteralPath $configPath -PathType Leaf)) {
    throw "Configuracao local nao encontrada: $configPath"
}
$config = Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
foreach ($field in @('JavaHome', 'PostgresBin', 'PostgresData', 'DatabasePort', 'DatabaseUser', 'DatabaseName', 'DatabasePasswordFile', 'BackendPort', 'FrontendPort')) {
    if (-not $config.PSObject.Properties[$field] -or [string]::IsNullOrWhiteSpace([string]$config.$field)) {
        throw "Campo ausente na configuracao local: $field"
    }
}
foreach ($field in @('DatabasePort', 'BackendPort', 'FrontendPort')) {
    if ([int]$config.$field -lt 1 -or [int]$config.$field -gt 65535) {
        throw "Porta invalida na configuracao local: $field"
    }
}

$javaExecutable = [System.IO.Path]::GetFullPath((Join-Path $config.JavaHome 'bin\java.exe'))
$pgCtl = Join-Path $config.PostgresBin 'pg_ctl.exe'
$pythonExecutable = Join-Path $projectRoot '.venv\Scripts\python.exe'
$pythonScript = Join-Path $projectRoot 'python\analyze.py'
$backendJar = Join-Path $projectRoot 'backend\target\cti-insights-api-0.1.0.jar'
$viteScript = Join-Path $projectRoot 'node_modules\vite\bin\vite.js'
$nodeCommand = Get-Command node.exe -ErrorAction SilentlyContinue
$nodeExecutable = if ($nodeCommand) { [System.IO.Path]::GetFullPath($nodeCommand.Source) } else { $null }
$apiUrl = "http://127.0.0.1:$($config.BackendPort)"
$frontendUrl = "http://127.0.0.1:$($config.FrontendPort)"
$state = @{ Backend = $null; Frontend = $null }
if (Test-Path -LiteralPath $statePath -PathType Leaf) {
    $savedState = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
    foreach ($name in @('Backend', 'Frontend')) {
        if ($savedState.PSObject.Properties[$name]) { $state[$name] = $savedState.$name }
    }
}

function Save-State {
    $temporaryPath = "$statePath.tmp"
    $state | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $temporaryPath -Encoding UTF8
    Move-Item -LiteralPath $temporaryPath -Destination $statePath -Force
}

function Quote-Argument([string]$Value) {
    if ($Value.Contains('"')) { throw 'Argumento de processo contem aspas invalidas.' }
    # Windows doubles trailing backslashes inside a quoted argument.
    return '"' + ($Value -replace '(\\+)$', '$1$1') + '"'
}

function Get-OwnedProcess([string]$Name) {
    $entry = $state[$Name]
    if (-not $entry) { return $null }
    foreach ($field in @('ProcessId', 'StartTimeUtcTicks', 'Executable')) {
        if (-not $entry.PSObject.Properties[$field]) { return $null }
    }
    $running = Get-Process -Id $entry.ProcessId -ErrorAction SilentlyContinue
    if (-not $running) { return $null }
    try {
        $expectedExecutable = if ($Name -eq 'Backend') { $javaExecutable } else { $nodeExecutable }
        $runningExecutable = [System.IO.Path]::GetFullPath($running.Path)
        $savedExecutable = [System.IO.Path]::GetFullPath([string]$entry.Executable)
        if (-not $expectedExecutable -or
            $running.StartTime.ToUniversalTime().Ticks -ne [long]$entry.StartTimeUtcTicks -or
            $runningExecutable -ine $savedExecutable -or
            $runningExecutable -ine $expectedExecutable) { return $null }
        return $running
    } catch { return $null }
}

function Test-PortBusy([int]$Port) {
    $listeners = [System.Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners()
    return @($listeners | Where-Object { $_.Port -eq $Port }).Count -gt 0
}

function Test-PostgresRunning {
    if (-not (Test-Path -LiteralPath $pgCtl -PathType Leaf)) { return $false }
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $null = & $pgCtl -D $config.PostgresData status 2>&1
        return $LASTEXITCODE -eq 0
    } finally { $ErrorActionPreference = $previousPreference }
}

function Invoke-PostgresControl([string[]]$Arguments) {
    $argumentLine = ($Arguments | ForEach-Object { Quote-Argument $_ }) -join ' '
    $control = Start-Process -FilePath $pgCtl -ArgumentList $argumentLine -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $logRoot 'postgres-control.stdout.log') `
        -RedirectStandardError (Join-Path $logRoot 'postgres-control.stderr.log')
    # Cache the native handle before the short-lived controller exits.
    $null = $control.Handle
    # Wait only for pg_ctl, not the persistent PostgreSQL process it launches.
    if (-not $control.WaitForExit(45000)) {
        throw "O controle do PostgreSQL excedeu 45 segundos. Consulte os logs em $logRoot"
    }
    if ($control.ExitCode -ne 0) {
        throw "PostgreSQL retornou erro $($control.ExitCode). Consulte os logs em $logRoot"
    }
}

function Start-ManagedProcess([string]$Name, [string]$Executable, [string[]]$Arguments, [hashtable]$Environment) {
    $previousEnvironment = @{}
    try {
        foreach ($key in $Environment.Keys) {
            $previousEnvironment[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
            [Environment]::SetEnvironmentVariable($key, [string]$Environment[$key], 'Process')
        }
        $argumentLine = ($Arguments | ForEach-Object { Quote-Argument $_ }) -join ' '
        $running = Start-Process -FilePath $Executable -ArgumentList $argumentLine -WorkingDirectory $projectRoot `
            -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput (Join-Path $logRoot "$Name.stdout.log") `
            -RedirectStandardError (Join-Path $logRoot "$Name.stderr.log")
        $state[$Name] = [pscustomobject]@{
            ProcessId = $running.Id
            StartTimeUtcTicks = $running.StartTime.ToUniversalTime().Ticks.ToString()
            Executable = $Executable
        }
        Save-State
    } finally {
        foreach ($key in $previousEnvironment.Keys) {
            [Environment]::SetEnvironmentVariable($key, $previousEnvironment[$key], 'Process')
        }
    }
}

function Test-ApiHealth {
    try {
        $health = Invoke-RestMethod -Uri "$apiUrl/actuator/health" -TimeoutSec 5
        return $health.status -eq 'UP'
    } catch { return $false }
}

function Test-FrontendHealth {
    try {
        $response = Invoke-WebRequest -Uri $frontendUrl -UseBasicParsing -TimeoutSec 5
        if ($response.StatusCode -ne 200) { return $false }
        # Vite can serve index.html even when the application entry module fails to compile.
        $entry = Invoke-WebRequest -Uri "$frontendUrl/src/main.js" -UseBasicParsing -TimeoutSec 5
        return $entry.StatusCode -eq 200 -and
            $entry.Headers['Content-Type'] -match '^(application|text)/(javascript|ecmascript)(;|$)'
    } catch { return $false }
}

if ($Action -eq 'status') {
    $backend = Get-OwnedProcess 'Backend'
    $frontend = Get-OwnedProcess 'Frontend'
    [pscustomobject]@{
        PostgreSQL = if (Test-PostgresRunning) { 'Em execucao' } else { 'Parado' }
        Backend = if ($backend) { "PID $($backend.Id)" } else { 'Parado ou nao gerenciado' }
        BackendSaudavel = if ($backend) { Test-ApiHealth } else { $false }
        Frontend = if ($frontend) { "PID $($frontend.Id)" } else { 'Parado ou nao gerenciado' }
        FrontendSaudavel = if ($frontend) { Test-FrontendHealth } else { $false }
        Url = $frontendUrl
        Logs = $logRoot
    } | Format-List
    return
}

New-Item -ItemType Directory -Path $logRoot -Force | Out-Null

if ($Action -eq 'stop') {
    foreach ($name in @('Frontend', 'Backend')) {
        $running = Get-OwnedProcess $name
        if ($running) {
            Stop-Process -InputObject $running -Force
            $running.WaitForExit(10000) | Out-Null
            Write-Output "$name encerrado."
        }
        $state[$name] = $null
    }
    Save-State
    if (Test-PostgresRunning) {
        Invoke-PostgresControl @('-D', $config.PostgresData, '-w', '-t', '30', 'stop', '-m', 'fast')
    }
    Write-Output 'Servicos locais do CTI encerrados; dados preservados.'
    return
}

foreach ($requiredPath in @($javaExecutable, $pgCtl, $pythonExecutable, $pythonScript, $backendJar, $viteScript, $config.DatabasePasswordFile)) {
    if (-not (Test-Path -LiteralPath $requiredPath -PathType Leaf)) {
        throw "Arquivo necessario nao encontrado: $requiredPath"
    }
}
if (-not $nodeExecutable) { throw 'Node.js nao encontrado no PATH.' }
if (-not (Test-Path -LiteralPath $config.PostgresData -PathType Container)) {
    throw "Diretorio do banco nao encontrado: $($config.PostgresData)"
}

$postgresRunning = Test-PostgresRunning
foreach ($item in @(
    @{ Name = 'Backend'; Port = [int]$config.BackendPort },
    @{ Name = 'Frontend'; Port = [int]$config.FrontendPort }
)) {
    if (-not (Get-OwnedProcess $item.Name) -and (Test-PortBusy $item.Port)) {
        throw "Porta $($item.Port) ocupada por outro processo. Nenhum processo existente foi encerrado."
    }
}
if (-not $postgresRunning -and (Test-PortBusy ([int]$config.DatabasePort))) {
    throw "Porta do PostgreSQL $($config.DatabasePort) ocupada por outro processo."
}

if (-not $postgresRunning) {
    Invoke-PostgresControl @('-D', $config.PostgresData, '-l', (Join-Path $logRoot 'postgres.log'), '-w', '-t', '30', 'start')
}

if (-not (Get-OwnedProcess 'Backend')) {
    $securePassword = (Get-Content -LiteralPath $config.DatabasePasswordFile -Raw).Trim() | ConvertTo-SecureString
    $backendEnvironment = @{
        PORT = [string]$config.BackendPort
        SERVER_ADDRESS = '127.0.0.1'
        DEBUG = 'false'
        DATABASE_URL = "jdbc:postgresql://127.0.0.1:$($config.DatabasePort)/$($config.DatabaseName)"
        DATABASE_USER = [string]$config.DatabaseUser
        DATABASE_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
        PYTHON_EXECUTABLE = $pythonExecutable
        PYTHON_SCRIPT = $pythonScript
        PYTHONIOENCODING = 'utf-8'
    }
    try {
        Start-ManagedProcess 'Backend' $javaExecutable @('-jar', $backendJar) $backendEnvironment
    } finally {
        $backendEnvironment['DATABASE_PASSWORD'] = $null
        $securePassword.Dispose()
    }
}
if (-not (Get-OwnedProcess 'Frontend')) {
    Start-ManagedProcess 'Frontend' $nodeExecutable @($viteScript, '--host', '127.0.0.1', '--port', [string]$config.FrontendPort, '--strictPort') @{
        API_PROXY_TARGET = $apiUrl
        VITE_API_URL = '/api/v1'
    }
}

$deadline = [DateTime]::UtcNow.AddSeconds(90)
do {
    if (-not (Get-OwnedProcess 'Backend') -or -not (Get-OwnedProcess 'Frontend')) {
        throw "Um servico encerrou durante a inicializacao. Consulte os logs em $logRoot"
    }
    if ((Test-ApiHealth) -and (Test-FrontendHealth)) {
        Write-Output "CTI pronto: $frontendUrl"
        Write-Output "API: $apiUrl | Logs: $logRoot"
        return
    }
    Start-Sleep -Seconds 1
} while ([DateTime]::UtcNow -lt $deadline)

throw "Os servicos nao ficaram saudaveis em 90 segundos. Consulte os logs em $logRoot"
