$ErrorActionPreference='Stop'
$raiz=Split-Path $PSScriptRoot -Parent
$caminho=Join-Path $raiz 'backend/target/cotacao-backend-0.1.0-SNAPSHOT.jar'
$arquivo=[System.IO.Compression.ZipFile]::OpenRead($caminho)
try {
 $entradas=@($arquivo.Entries | ForEach-Object { $_.FullName })
 foreach ($esperado in @('BOOT-INF/classes/static/index.html','BOOT-INF/classes/static/3rdpartylicenses.txt')) {
  if ($entradas -notcontains $esperado) { throw "Recurso obrigatório ausente do JAR: $esperado" }
 }
 if (!($entradas -match '^BOOT-INF/classes/static/main-[^/]+\.js$')) { throw 'Bundle Angular ausente do JAR.' }
 if (!($entradas -match '^BOOT-INF/classes/static/styles-[^/]+\.css$')) { throw 'CSS Angular ausente do JAR.' }
 if ($entradas -match '(^|/)\.env$|frontend-qa|\.spec\.') { throw 'Arquivo de configuração local ou teste encontrado no pacote.' }
 Write-Output 'Pacote conferido: Angular, CSS e licenças presentes; credenciais e fixtures locais ausentes.'
} finally { $arquivo.Dispose() }
