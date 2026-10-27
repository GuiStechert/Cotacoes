import {Lista} from './lista.js';
import {Cotacao} from './cotacao.js';
import {Aprovacao} from './aprovacao.js';
export const ROUTES=[
 {path:'cotacoes',component:Lista,canActivate:['COMERCIAL','GESTOR','SUPERVISOR','GERENTE'],tela:'lista'},
 {path:'cotacao/:id',component:Cotacao,canActivate:['COMERCIAL'],tela:'cotacao'},
 {path:'aprovacao/:id',component:Aprovacao,canActivate:['GESTOR','SUPERVISOR','GERENTE'],tela:'aprovacao'}
];
export function resolverRota(path,perfil){
 if(!ROUTES[0].canActivate.includes(perfil))return null;
 const url=new URL(path,'http://localhost');
 const match=url.pathname.match(/^\/(cotacao|aprovacao)\/([^/]+)$/);
 const route=match?ROUTES.find(r=>r.tela===match[1]):ROUTES[0];
 if(!route.canActivate.includes(perfil))return {...ROUTES[0],id:null,fila:false};
 return {...route,id:match?decodeURIComponent(match[2]):null,fila:perfil!=='COMERCIAL'&&url.searchParams.get('fila')==='1'};
}
export function criarNavegador(win,mudou){return {async navigate(parts,options={}){const path=parts.map((p,i)=>i?encodeURIComponent(p):p).join('/');win.history[options.replaceUrl?'replaceState':'pushState']({},'',path);win.scrollTo(0,0);await mudou();}};}
