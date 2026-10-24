param([switch]$AplicarEstrutura)
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
 $entry = Get-Content .env | Where-Object { $_ -match '^\s*DATABASE_URL\s*=' } | Select-Object -First 1
 $value = ($entry -replace '^\s*DATABASE_URL\s*=\s*','').Trim().Trim('"').Trim("'")
 # Captura até o último @ para suportar senhas legadas não escapadas.
 if ($value -notmatch '^postgres(?:ql)?://([^:]+):(.*)@([^/:]+)(?::(\d+))?/([^?]+)(?:\?.*)?$') {
  throw 'DATABASE_URL possui formato não suportado. Use uma URI PostgreSQL.'
 }
 $env:PGUSER = [uri]::UnescapeDataString($matches[1])
 $env:PGPASSWORD = [uri]::UnescapeDataString($matches[2])
 $env:PGHOST = $matches[3]
 $env:PGPORT = if ($matches[4]) { $matches[4] } else { '5432' }
 $env:PGDATABASE = [uri]::UnescapeDataString($matches[5])
 $env:PGCONNECT_TIMEOUT = '10'
 $result = & 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -X -w -t -A -c 'SELECT 1' 2>$null
 if ($LASTEXITCODE -ne 0 -or "$result".Trim() -ne '1') { throw 'Conexão não confirmada; detalhes sensíveis suprimidos.' }
 Write-Output 'DATABASE_URL: conexão somente leitura confirmada.'
 if ($AplicarEstrutura) {
  $arquivo = 'backend/src/main/resources/db/migration/V1__estrutura_inicial.sql'
  $checksum = (Get-FileHash $arquivo -Algorithm SHA256).Hash
  $sql = "CREATE SCHEMA cotacao; SET LOCAL search_path TO cotacao;`n"
  $sql += Get-Content $arquivo -Raw
  $sql += "`nCREATE TABLE schema_migrations(versao integer PRIMARY KEY, checksum text NOT NULL, aplicado_em timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP);"
  $sql += "`nINSERT INTO schema_migrations(versao,checksum) VALUES(1,'$checksum');"
  # CREATE SCHEMA sem IF NOT EXISTS impede sobrescrever instalação anterior.
  $sql | & 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -X -w --single-transaction -v ON_ERROR_STOP=1 -q 1>$null 2>$null
  if ($LASTEXITCODE -ne 0) { throw 'Migração não aplicada; transação revertida.' }
  Write-Output 'Migração V1 aplicada no schema cotacao com checksum SHA256.'
 }
 $estrutura = & 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -X -w -t -A -c "SELECT count(*) FROM information_schema.tables WHERE table_schema='cotacao' AND table_type='BASE TABLE'" 2>$null
 if ($LASTEXITCODE -ne 0) { throw 'Não foi possível conferir a estrutura.' }
 Write-Output "Tabelas no schema cotacao: $estrutura (inclui controle de migrações)."
} catch {
 Write-Output 'Falha na conexão ou na operação solicitada; detalhes sensíveis suprimidos. A migração usa transação e recusa schema já existente.'
 exit 1
} finally {
 Remove-Item Env:PGPASSWORD,Env:PGUSER,Env:PGHOST,Env:PGPORT,Env:PGDATABASE,Env:PGCONNECT_TIMEOUT -ErrorAction SilentlyContinue
 Pop-Location
}
