import {it,expect} from './harness.mjs';
import {readFileSync} from 'node:fs';
import {createHash} from 'node:crypto';
it('distribuição usa Vue CDN fixado com SRI e dependências sem Angular ou esbuild',()=>{
 const html=readFileSync(new URL('../public/index.html',import.meta.url),'utf8');
 const fixture=readFileSync(new URL('./vue.global.prod.cjs',import.meta.url));
 expect(html).toContain('https://cdn.jsdelivr.net/npm/vue@3.5.22/dist/vue.global.prod.js');
 expect(html).toContain('sha384-'+createHash('sha384').update(fixture).digest('base64'));
 const lock=readFileSync(new URL('../package-lock.json',import.meta.url),'utf8');expect(lock).not.toContain('node_modules/esbuild');expect(lock).not.toContain('node_modules/@angular/');
 const pom=readFileSync(new URL('../../backend/pom.xml',import.meta.url),'utf8');expect(pom).toContain('../frontend/public');expect(pom).not.toContain('frontend/dist');
});
