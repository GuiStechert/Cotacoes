param([switch]$InstalarDependencias)
$ErrorActionPreference='Stop'
& (Join-Path $PSScriptRoot 'testar-frontend.ps1') -Build -Instalar:$InstalarDependencias
& (Join-Path $PSScriptRoot 'testar-backend.ps1') -Empacotar -BaixarDependencias:$InstalarDependencias
& (Join-Path $PSScriptRoot 'verificar-pacote.ps1')
