import {readFile,readdir} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import {execFileSync} from 'node:child_process';
const root=new URL('../public/',import.meta.url);
for(const name of await readdir(root)){
 const path=new URL(name,root);
 if(name.endsWith('.js')){
  execFileSync(process.execPath,['--check',fileURLToPath(path)],{stdio:'pipe'});
  const source=await readFile(path,'utf8');
  for(const [,relative] of source.matchAll(/from ['"](.+?)['"]/g))await readFile(new URL(relative,path));
 }
}
for(const file of ['index.html','app.html','lista.html','cotacao.html','aprovacao.html','styles.css','3rdpartylicenses.txt'])await readFile(new URL(file,root));
console.log('Arquivos estáticos verificados: JavaScript nativo e Vue CDN, sem compilação.');
