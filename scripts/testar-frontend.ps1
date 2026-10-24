param([switch]$Instalar,[switch]$Build)
$ErrorActionPreference='Stop'
Push-Location (Join-Path (Split-Path $PSScriptRoot -Parent) 'frontend')
try {
 if ($Instalar) { & npm.cmd ci --no-audit --no-fund; if ($LASTEXITCODE -ne 0) { throw 'Falha na instalação do frontend.' } }
 & npm.cmd test
 if ($LASTEXITCODE -ne 0) { throw 'Os testes do frontend falharam.' }
 if ($Build) { & npm.cmd run build; if ($LASTEXITCODE -ne 0) { throw 'O build do frontend falhou.' } }
} finally { Pop-Location }
