import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { test } from 'node:test'

test('local frontend health checks the compiled entry module as well as the HTML page', {
  skip: process.platform !== 'win32' && 'The local service manager requires Windows PowerShell',
}, () => {
  const scriptPath = fileURLToPath(new URL('../../scripts/local.ps1', import.meta.url))
  const command = String.raw`
    Set-StrictMode -Version Latest
    $ErrorActionPreference = 'Stop'
    $tokens = $null
    $parseErrors = $null
    $ast = [System.Management.Automation.Language.Parser]::ParseFile('${scriptPath.replaceAll("'", "''")}', [ref]$tokens, [ref]$parseErrors)
    if ($parseErrors.Count) { throw $parseErrors[0] }
    $healthFunction = $ast.Find({ param($node)
        $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and
        $node.Name -eq 'Test-FrontendHealth'
    }, $true)
    # Load only the health function, so these tests cannot start or stop local services.
    Invoke-Expression $healthFunction.Extent.Text
    $frontendUrl = 'http://127.0.0.1:5174'
    $results = @()
    foreach ($scenario in @('healthy', 'compile-error', 'html-fallback', 'index-error', 'offline')) {
        $script:scenario = $scenario
        $script:requests = @()
        function Invoke-WebRequest {
            param($Uri, [switch]$UseBasicParsing, $TimeoutSec)
            $script:requests += $Uri
            if ($script:scenario -eq 'offline') { throw 'Connection refused' }
            if ($Uri -eq $frontendUrl) {
                return @{ StatusCode = $(if ($script:scenario -eq 'index-error') { 503 } else { 200 }) }
            }
            if ($script:scenario -eq 'compile-error') { throw '500: Entry module failed to compile' }
            return @{
                StatusCode = 200
                Headers = @{ 'Content-Type' = $(if ($script:scenario -eq 'html-fallback') { 'text/html' } else { 'text/javascript; charset=utf-8' }) }
            }
        }
        $healthy = Test-FrontendHealth
        $results += @{ scenario = $scenario; healthy = $healthy; requests = @($script:requests) }
    }
    ConvertTo-Json -InputObject $results -Depth 4 -Compress
  `
  const result = spawnSync('powershell.exe', [
    '-NoProfile', '-NonInteractive', '-EncodedCommand', Buffer.from(command, 'utf16le').toString('base64'),
  ], { encoding: 'utf8', timeout: 15000, windowsHide: true })
  assert.ifError(result.error)
  assert.equal(result.status, 0, result.stderr)
  const results = JSON.parse(result.stdout.trim())
  assert.deepEqual(results.map(({ scenario, healthy }) => [scenario, healthy]), [
    ['healthy', true],
    ['compile-error', false],
    ['html-fallback', false],
    ['index-error', false],
    ['offline', false],
  ])
  for (const { scenario, requests } of results) {
    assert.deepEqual(requests, scenario === 'index-error' || scenario === 'offline'
      ? ['http://127.0.0.1:5174']
      : ['http://127.0.0.1:5174', 'http://127.0.0.1:5174/src/main.js'])
  }
})
