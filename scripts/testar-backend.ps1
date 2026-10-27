param(
 [string]$JavaHome = 'C:\Program Files\Java\jdk-25.0.4.1',
 [string]$PostgresBin = 'C:\Program Files\PostgreSQL\18\bin',
 [switch]$BaixarDependencias,
 [switch]$Empacotar,
 [string]$Testes
)
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
 $env:JAVA_HOME = $JavaHome
 $maven = Join-Path (Get-Location) '.tools/apache-maven-3.9.11/bin/mvn.cmd'
 if (!(Test-Path $maven)) { throw 'Instale o Maven 3.9.11 em .tools/apache-maven-3.9.11.' }
 if (!(Test-Path '.tools/pg-test/PG_VERSION')) {
  & "$PostgresBin/initdb.exe" -D .tools/pg-test -U cotacao_test --auth=trust --encoding=UTF8 --locale=C
  if ($LASTEXITCODE -ne 0) { throw 'Falha ao criar PostgreSQL de teste.' }
 }
 & "$PostgresBin/pg_ctl.exe" -D .tools/pg-test -l .tools/pg-test.log -o '-h 127.0.0.1 -p 55432' -w start
 if ($LASTEXITCODE -ne 0) { throw 'Não foi possível iniciar o cluster de teste; confira a porta 55432.' }
 try {
  $opcoes = @('-o')
  if ($BaixarDependencias) { $opcoes = @() }
  if ($Testes) { $opcoes += "-Dtest=$Testes" }
  $alvo = if ($Empacotar) { 'package' } else { 'test' }
  $alvos = @($alvo)
  if ($Empacotar) { $alvos = @('clean',$alvo) }
  & $maven @opcoes -f backend/pom.xml '-Dmaven.repo.local=.tools/repository' @alvos
  $resultado = $LASTEXITCODE
 } finally {
  & "$PostgresBin/pg_ctl.exe" -D .tools/pg-test -m fast -w stop
 }
 if ($resultado -ne 0) { throw 'A suíte de testes falhou.' }
} finally { Pop-Location }
