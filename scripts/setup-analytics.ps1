param([string]$PythonExecutable = 'python')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$configPath = Join-Path $env:LOCALAPPDATA 'CTI_Insights/config.json'
$environmentPath = Join-Path $projectRoot '.venv-analytics'
$python = Join-Path $environmentPath 'Scripts/python.exe'
$environmentWorks = $false
if (Test-Path -LiteralPath $python) {
    & $python -c "import sys; sys.exit(0)"
    $environmentWorks = $LASTEXITCODE -eq 0
}
if (-not $environmentWorks) {
    & $PythonExecutable -m venv $environmentPath
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao criar ambiente cientifico.' }
}
& $python -m pip install -r (Join-Path $projectRoot 'analytics-python/requirements.txt') --disable-pip-version-check
if ($LASTEXITCODE -ne 0) { throw 'Falha ao instalar dependencias cientificas.' }
$env:OPENBLAS_NUM_THREADS = '1'
$env:OMP_NUM_THREADS = '1'
& $python -u -c "import faulthandler; faulthandler.dump_traceback_later(120, exit=True); import pandas, scipy.stats, matplotlib.pyplot, openpyxl, requests, fastapi, uvicorn, sqlalchemy, psycopg; faulthandler.cancel_dump_traceback_later(); print('Ambiente cientifico validado.')"
if ($LASTEXITCODE -ne 0) { throw 'Falha ao validar ambiente cientifico.' }
if (Test-Path -LiteralPath $configPath) {
    $config = Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
    $config | Add-Member -NotePropertyName PythonExecutable -NotePropertyValue $python -Force
    $config | ConvertTo-Json | Set-Content -LiteralPath $configPath -Encoding UTF8
}
Write-Output "Python configurado: $python"
