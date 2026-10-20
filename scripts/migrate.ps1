# Migrações incrementais PostgreSQL para uma base CTI existente.
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$projectPath = Split-Path -Parent $PSScriptRoot
$config = Get-Content (Join-Path $env:LOCALAPPDATA 'CTI_Insights/config.json') -Raw | ConvertFrom-Json
$psql = Join-Path $config.PostgresBin 'psql.exe'
$dump = Join-Path $config.PostgresBin 'pg_dump.exe'
$runtimePath = Join-Path $projectPath '.runtime'
$backupPath = Join-Path $runtimePath 'backups'
New-Item -ItemType Directory -Path $backupPath -Force | Out-Null
$secure = (Get-Content -LiteralPath $config.DatabasePasswordFile -Raw).Trim() | ConvertTo-SecureString
$previousPassword = $env:PGPASSWORD
try {
    $env:PGPASSWORD = [System.Net.NetworkCredential]::new('', $secure).Password
    $connection = @('-h', '127.0.0.1', '-p', [string]$config.DatabasePort, '-U', [string]$config.DatabaseUser, '-d', [string]$config.DatabaseName)
    $backup = Join-Path $backupPath ('pre-migration-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.dump')
    & $dump @connection -Fc -f $backup
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel criar o backup; migracao cancelada.' }
    $null = & $psql @connection -v ON_ERROR_STOP=1 -At -c 'CREATE TABLE IF NOT EXISTS app_schema_migrations (version varchar(160) primary key, checksum varchar(64) not null, applied_at timestamptz not null default now())'
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel preparar o historico de migracoes.' }
    foreach ($file in Get-ChildItem (Join-Path $projectPath 'deploy/migrations') -Filter '*.sql' | Sort-Object Name) {
        if ($file.Name -notmatch '^V[0-9_]+__[a-zA-Z0-9_]+\.sql$') { throw 'Nome de migracao invalido.' }
        $checksum = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        $version = $file.Name
        $existing = & $psql @connection -v ON_ERROR_STOP=1 -At -c ("SELECT checksum FROM app_schema_migrations WHERE version='" + $version + "'")
        if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel consultar o historico.' }
        if ($existing) {
            if ([string]$existing -ne $checksum) { throw "Migracao ja aplicada foi alterada: $version" }
            Write-Output "Ja aplicada: $version"
            continue
        }
        $temporary = Join-Path $runtimePath ('migration-' + [Guid]::NewGuid().ToString('N') + '.sql')
        try {
            $sql = "BEGIN;" + [Environment]::NewLine + [IO.File]::ReadAllText($file.FullName) +
                [Environment]::NewLine + "INSERT INTO app_schema_migrations(version,checksum) VALUES ('$version','$checksum');" +
                [Environment]::NewLine + "COMMIT;"
            [IO.File]::WriteAllText($temporary, $sql, [Text.UTF8Encoding]::new($false))
            & $psql @connection -v ON_ERROR_STOP=1 -f $temporary
            if ($LASTEXITCODE -ne 0) { throw "Falha na migracao: $version. A transacao foi revertida." }
            Write-Output "Aplicada: $version"
        } finally { Remove-Item -LiteralPath $temporary -ErrorAction SilentlyContinue }
    }
} finally { $env:PGPASSWORD = $previousPassword; $secure.Dispose() }
