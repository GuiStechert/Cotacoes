$ErrorActionPreference='Stop'
$raiz=Split-Path $PSScriptRoot -Parent
$caminho=Join-Path $raiz 'backend/target/cotacao-backend-0.1.0-SNAPSHOT.jar'
$arquivo=[System.IO.Compression.ZipFile]::OpenRead($caminho)
try {
 $entradas=@($arquivo.Entries | ForEach-Object { $_.FullName })
 foreach ($nome in @('index.html','3rdpartylicenses.txt','main.js','bootstrap.js','runtime.js','api.js','sessao.js','routes.js','app.js','lista.js','cotacao.js','aprovacao.js','regras.js','decimal.js','styles.css','app.html','lista.html','cotacao.html','aprovacao.html')) {
  $esperado="BOOT-INF/classes/static/$nome"
  if ($entradas -notcontains $esperado) { throw "Recurso obrigatório ausente do JAR: $esperado" }
 }
 if ($entradas -match '^BOOT-INF/classes/static/(main-|styles-|polyfills-)') { throw 'Artefato legado Angular encontrado no JAR. Limpe backend/target antes de empacotar.' }
 if ($entradas -match '(^|/)\.env$|frontend-qa|\.spec\.') { throw 'Arquivo de configuração local ou teste encontrado no pacote.' }
 Write-Output 'Pacote conferido: Vue CDN, módulos JavaScript, templates, CSS e licenças presentes; Angular e fixtures ausentes.'
} finally { $arquivo.Dispose() }
