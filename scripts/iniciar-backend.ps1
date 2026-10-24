param([string]$JavaHome='C:\Program Files\Java\jdk-25.0.4.1',[switch]$WindowsSso)
$ErrorActionPreference='Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
$anterior=[Environment]::GetEnvironmentVariable('DATABASE_URL','Process')
try {
 $jar='backend/target/cotacao-backend-0.1.0-SNAPSHOT.jar'
 if (!(Test-Path $jar)) { throw 'Execute scripts/testar-backend.ps1 -Empacotar antes de iniciar.' }
 if (!$anterior) {
  $linha=Get-Content .env | Where-Object { $_ -match '^\s*DATABASE_URL\s*=' } | Select-Object -First 1
  if (!$linha) { throw 'DATABASE_URL não definida no ambiente nem no .env.' }
  $env:DATABASE_URL=($linha -replace '^\s*DATABASE_URL\s*=\s*','').Trim().Trim('"').Trim("'")
 }
 $argumentos=@('-jar',$jar)
 if ($WindowsSso) { $argumentos += '--cotacao.auth.windows.enabled=true' }
 & "$JavaHome/bin/java.exe" @argumentos
 if ($LASTEXITCODE -ne 0) { throw 'O backend encerrou com erro.' }
} finally {
 if ($null -eq $anterior) { Remove-Item Env:DATABASE_URL -ErrorAction SilentlyContinue }
 else { $env:DATABASE_URL=$anterior }
 Pop-Location
}
