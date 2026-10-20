$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$configPath = Join-Path $env:LOCALAPPDATA 'CTI_Insights/config.json'
$environmentPath = Join-Path $env:LOCALAPPDATA 'CTI_Insights/analytics312-venv'
$python = Join-Path $environmentPath 'Scripts/python.exe'
if (-not (Test-Path -LiteralPath $python)) {
    & py.exe -3.12 -m venv $environmentPath
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao criar ambiente cientifico.' }
}
& $python -m pip install -r (Join-Path $projectRoot 'analytics-python/requirements.txt') --disable-pip-version-check
if ($LASTEXITCODE -ne 0) { throw 'Falha ao instalar dependencias cientificas.' }
$env:OPENBLAS_NUM_THREADS = '1'
$env:OMP_NUM_THREADS = '1'
& $python -c "import scipy.stats, matplotlib.pyplot, openpyxl, requests; print('Ambiente cientifico validado.')"
if ($LASTEXITCODE -ne 0) { throw 'Falha ao validar ambiente cientifico.' }
if (Test-Path -LiteralPath $configPath) {
    $config = Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
    $config | Add-Member -NotePropertyName PythonExecutable -NotePropertyValue $python -Force
    $config | ConvertTo-Json | Set-Content -LiteralPath $configPath -Encoding UTF8
}
Write-Output "Python configurado: $python"
