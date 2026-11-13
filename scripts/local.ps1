param(
    [ValidateSet('start', 'stop', 'status')]
    [string]$Action = 'start',
    # Sobe banco, Python e backend sem o front (para quem já roda "npm run dev").
    [switch]$SemFrontend,
    [switch]$BancoLocal
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
$pythonExecutable = if ($config.PSObject.Properties['PythonExecutable'] -and $config.PythonExecutable) { [System.IO.Path]::GetFullPath($config.PythonExecutable) } else { Join-Path $projectRoot '.venv\Scripts\python.exe' }
$analyticsPython = Join-Path $projectRoot '.venv-analytics\Scripts\python.exe'
if (Test-Path -LiteralPath $analyticsPython -PathType Leaf) { $pythonExecutable = $analyticsPython }
$azureSettings = $null
if ($Action -eq 'start' -and -not $BancoLocal -and (Test-Path -LiteralPath (Join-Path $projectRoot 'analytics-python/.env'))) {
    # A saida inclui a senha e fica apenas nesta variavel; nunca imprimir.
    $databaseJson = & $pythonExecutable (Join-Path $PSScriptRoot 'azure-database.py') --settings-json
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao ler configuracao do banco.' }
    $databaseSettings = ($databaseJson -join "`n") | ConvertFrom-Json
    if ($databaseSettings.DB_HOST -like '*.postgres.database.azure.com') {
        if ($databaseSettings.DB_SSLMODE -notin @('require','verify-full')) { throw 'Azure exige DB_SSLMODE=require ou verify-full.' }
        foreach ($field in @('DB_NAME','DB_USER','DB_PASSWORD','DB_PORT')) {
            if ([string]::IsNullOrWhiteSpace($databaseSettings.$field)) { throw "Configure $field no analytics-python/.env." }
        }
        $azureSettings = $databaseSettings
        Write-Output "Banco selecionado: Azure $($azureSettings.DB_HOST)/$($azureSettings.DB_NAME)"
    }
    $databaseJson = $null
}
$pythonScript = Join-Path $projectRoot 'python\analyze.py'
$backendJar = Join-Path $projectRoot 'backend\target\cti-insights-api-0.1.0.jar'
$viteScript = Join-Path $projectRoot 'node_modules\vite\bin\vite.js'
$nodeCommand = Get-Command node.exe -ErrorAction SilentlyContinue
$nodeExecutable = if ($nodeCommand) { [System.IO.Path]::GetFullPath($nodeCommand.Source) } else { $null }
$apiUrl = "http://127.0.0.1:$($config.BackendPort)"
$frontendUrl = "http://127.0.0.1:$($config.FrontendPort)"
# Aula 12: FastAPI (analytics-python/api.py) recebe o POST /etl do Spring.
$analyticsPort = if ($config.PSObject.Properties['AnalyticsPort'] -and $config.AnalyticsPort) { [int]$config.AnalyticsPort } else { 8000 }
$analyticsUrl = "http://127.0.0.1:$analyticsPort"
$analyticsDir = Join-Path $projectRoot 'analytics-python'
$state = @{ Backend = $null; Frontend = $null; Analytics = $null }
if (Test-Path -LiteralPath $statePath -PathType Leaf) {
    $savedState = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
    foreach ($name in @('Backend', 'Frontend', 'Analytics')) {
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
        $expectedExecutable = switch ($Name) { 'Backend' { $javaExecutable } 'Analytics' { $pythonExecutable } default { $nodeExecutable } }
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

function Get-JwtSecret {
    if (-not [string]::IsNullOrWhiteSpace($env:JWT_SECRET_BASE64)) { return $env:JWT_SECRET_BASE64 }
    return Get-LocalSecret 'jwt-secret.dpapi'
}

# Chave X-ETL-Key combinada entre Spring e Python; gerada uma vez e guardada com DPAPI.
function Get-EtlKey {
    if (-not [string]::IsNullOrWhiteSpace($env:ETL_API_KEY)) { return $env:ETL_API_KEY }
    return Get-LocalSecret 'etl-api-key.dpapi'
}

function Stop-ProcessTree($Process) {
    # O python.exe de um venv no Windows abre o interpretador real como processo filho.
    Get-CimInstance Win32_Process -Filter "ParentProcessId = $($Process.Id)" -ErrorAction SilentlyContinue |
        ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
    Stop-Process -InputObject $Process -Force
}

function Test-AnalyticsHealth {
    try { return (Invoke-RestMethod -Uri "$analyticsUrl/saude" -TimeoutSec 5).status -eq 'ok' } catch { return $false }
}

function Get-LocalSecret([string]$FileName) {
    $secretPath = Join-Path $configRoot $FileName
    if (-not (Test-Path -LiteralPath $secretPath -PathType Leaf)) {
        $bytes = New-Object byte[] 32
        $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
        $secure = ConvertTo-SecureString ([Convert]::ToBase64String($bytes)) -AsPlainText -Force
        try { $secure | ConvertFrom-SecureString | Set-Content -LiteralPath $secretPath -Encoding UTF8 }
        finally { $secure.Dispose(); [Array]::Clear($bytes, 0, $bytes.Length) }
    }
    $savedSecret = (Get-Content -LiteralPath $secretPath -Raw).Trim() | ConvertTo-SecureString
    try { return [System.Net.NetworkCredential]::new('', $savedSecret).Password }
    finally { $savedSecret.Dispose() }
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
    $analytics = Get-OwnedProcess 'Analytics'
    [pscustomobject]@{
        AnalyticsPython = if ($analytics) { "PID $($analytics.Id)" } else { 'Parado ou nao gerenciado' }
        AnalyticsSaudavel = Test-AnalyticsHealth
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
    foreach ($name in @('Frontend', 'Backend', 'Analytics')) {
        $running = Get-OwnedProcess $name
        if ($running) {
            Stop-ProcessTree $running
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

$requiredPaths = @($javaExecutable, $pythonExecutable, $pythonScript, $backendJar, $viteScript)
if (-not $azureSettings) { $requiredPaths += @($pgCtl, $config.DatabasePasswordFile) }
foreach ($requiredPath in $requiredPaths) {
    if (-not (Test-Path -LiteralPath $requiredPath -PathType Leaf)) {
        throw "Arquivo necessario nao encontrado: $requiredPath"
    }
}
if (-not $nodeExecutable) { throw 'Node.js nao encontrado no PATH.' }
if (-not $azureSettings -and -not (Test-Path -LiteralPath $config.PostgresData -PathType Container)) {
    throw "Diretorio do banco nao encontrado: $($config.PostgresData)"
}

$postgresRunning = if ($azureSettings) { $true } else { Test-PostgresRunning }
foreach ($item in @(
    @{ Name = 'Backend'; Port = [int]$config.BackendPort },
    @{ Name = 'Frontend'; Port = [int]$config.FrontendPort },
    @{ Name = 'Analytics'; Port = $analyticsPort }
)) {
    if ($SemFrontend -and $item.Name -eq 'Frontend') { continue }
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

$etlKey = Get-EtlKey
if (-not (Get-OwnedProcess 'Analytics')) {
    $securePassword = $null
    if (-not $azureSettings) { $securePassword = (Get-Content -LiteralPath $config.DatabasePasswordFile -Raw).Trim() | ConvertTo-SecureString }
    $analyticsEnvironment = @{
        DB_HOST = '127.0.0.1'
        DB_PORT = [string]$config.DatabasePort
        DB_NAME = [string]$config.DatabaseName
        DB_USER = [string]$config.DatabaseUser
        DB_PASSWORD = $(if ($azureSettings) { $azureSettings.DB_PASSWORD } else { [System.Net.NetworkCredential]::new('', $securePassword).Password })
        DB_SSLMODE = 'disable'
        ETL_API_KEY = $etlKey
        PYTHONIOENCODING = 'utf-8'
    }
    if ($azureSettings) {
        foreach ($field in @('DB_HOST','DB_PORT','DB_NAME','DB_USER','DB_PASSWORD','DB_SSLMODE')) { $analyticsEnvironment[$field] = $azureSettings.$field }
    }
    try {
        Start-ManagedProcess 'Analytics' $pythonExecutable @('-m', 'uvicorn', 'api:app', '--app-dir', $analyticsDir, '--host', '127.0.0.1', '--port', [string]$analyticsPort) $analyticsEnvironment
    } finally {
        $analyticsEnvironment['DB_PASSWORD'] = $null
        if ($securePassword) { $securePassword.Dispose() }
    }
}

if (-not (Get-OwnedProcess 'Backend')) {
    $securePassword = $null
    if (-not $azureSettings) { $securePassword = (Get-Content -LiteralPath $config.DatabasePasswordFile -Raw).Trim() | ConvertTo-SecureString }
    $backendEnvironment = @{
        PORT = [string]$config.BackendPort
        SERVER_ADDRESS = '127.0.0.1'
        DEBUG = 'false'
        JWT_SECRET_BASE64 = Get-JwtSecret
        DATABASE_URL = "jdbc:postgresql://127.0.0.1:$($config.DatabasePort)/$($config.DatabaseName)"
        DATABASE_USER = [string]$config.DatabaseUser
        DATABASE_PASSWORD = $(if ($azureSettings) { $azureSettings.DB_PASSWORD } else { [System.Net.NetworkCredential]::new('', $securePassword).Password })
        PYTHON_MODE = $(if ($config.PSObject.Properties['PythonMode']) { $config.PythonMode } else { 'native' })
        PYTHON_EXECUTABLE = $pythonExecutable
        PYTHON_SCRIPT = $pythonScript
        PYTHONIOENCODING = 'utf-8'
        ETL_URL = "$analyticsUrl/etl"
        ETL_API_KEY = $etlKey
    }
    if ($azureSettings) {
        $backendEnvironment['DATABASE_URL'] = "jdbc:postgresql://$($azureSettings.DB_HOST):$($azureSettings.DB_PORT)/$($azureSettings.DB_NAME)?sslmode=$($azureSettings.DB_SSLMODE)"
        $backendEnvironment['DATABASE_USER'] = $azureSettings.DB_USER
        $backendEnvironment['DATABASE_PASSWORD'] = $azureSettings.DB_PASSWORD
    }
    try {
        Start-ManagedProcess 'Backend' $javaExecutable @('-jar', $backendJar) $backendEnvironment
    } finally {
        $backendEnvironment['DATABASE_PASSWORD'] = $null
        $backendEnvironment['JWT_SECRET_BASE64'] = $null
        if ($securePassword) { $securePassword.Dispose() }
    }
}
if (-not $SemFrontend -and -not (Get-OwnedProcess 'Frontend')) {
    Start-ManagedProcess 'Frontend' $nodeExecutable @($viteScript, '--host', '127.0.0.1', '--port', [string]$config.FrontendPort, '--strictPort') @{
        API_PROXY_TARGET = $apiUrl
        VITE_API_URL = '/api/v1'
    }
}

$deadline = [DateTime]::UtcNow.AddSeconds(90)
do {
    if (-not (Get-OwnedProcess 'Backend') -or (-not $SemFrontend -and -not (Get-OwnedProcess 'Frontend'))) {
        throw "Um servico encerrou durante a inicializacao. Consulte os logs em $logRoot"
    }
    if ((Test-ApiHealth) -and ($SemFrontend -or (Test-FrontendHealth))) {
        if ($SemFrontend) { Write-Output 'Front nao iniciado (-SemFrontend): use o seu "npm run dev".' }
        else { Write-Output "CTI pronto: $frontendUrl" }
        Write-Output "API: $apiUrl | Python (ETL): $analyticsUrl/docs | Logs: $logRoot"
        # O Python carrega pandas/scipy na subida e pode demorar; o Spring confere /saude a cada envio.
        if (-not (Test-AnalyticsHealth)) { Write-Output 'Python (ETL) ainda iniciando; confira com: npm run status' }
        return
    }
    Start-Sleep -Seconds 1
} while ([DateTime]::UtcNow -lt $deadline)

throw "Os servicos nao ficaram saudaveis em 90 segundos. Consulte os logs em $logRoot"
