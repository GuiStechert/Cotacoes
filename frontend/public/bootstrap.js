import {Api} from './api.js';
import {Sessao} from './sessao.js';
import {App} from './app.js';
import {controlador} from './runtime.js';
import {resolverRota,criarNavegador} from './routes.js';
export async function iniciarAplicacao(options={}){
 const win=options.window||window;
 const template=options.template|| (async name=>{const response=await win.fetch('/'+name+'.html',{credentials:'same-origin'});if(!response.ok)throw new Error('Template indisponível');return response.text();});
 const templates=Object.fromEntries(await Promise.all(['app','lista','cotacao','aprovacao'].map(async name=>[name,await template(name)])));
 const api=options.api||new Api(win.fetch.bind(win),()=>sessao.token());
 const sessao=controlador(new Sessao(api));
 let version=0;
 const router=criarNavegador(win,carregarRota);
 const shell=controlador(new App(api,sessao,router));shell.tela=null;shell.rotaChave='';
 const app=Vue.createApp({setup:()=>shell,template:templates.app});
 app.mount(win.document.querySelector('#app'));
 async function carregarRota(){
  const request=++version;
  const route=resolverRota(win.location.pathname+win.location.search,sessao.usuario()?.perfil);
  shell.tela=null;if(!route)return;
  const state=controlador(new route.component(api,sessao,router));
  shell.rotaChave=String(request);
  // Mount before loading so pending/error states remain visible.
  shell.tela=Vue.markRaw({setup:()=>state,template:templates[route.tela]});
  if(route.tela==='lista'){state.fila.set(route.fila);await state.carregar();}else await state.carregar(route.id);
 }
 const clicar=event=>{
  if(event.defaultPrevented||event.button!==0||event.ctrlKey||event.metaKey||event.shiftKey||event.altKey)return;
  const anchor=event.target.closest('a[href]');if(!anchor||anchor.target||anchor.hasAttribute('download'))return;
  const url=new URL(anchor.href,win.location.href);
  if(url.origin!==win.location.origin||!/^\/(cotacoes|cotacao\/[^/]+|aprovacao\/[^/]+)$/.test(url.pathname)||url.hash)return;
  event.preventDefault();void router.navigate([url.pathname+url.search]);
 };
 const popstate=()=>{win.scrollTo(0,0);void carregarRota();};
 win.document.addEventListener('click',clicar);win.addEventListener('popstate',popstate);
 await sessao.iniciar();await carregarRota();await Vue.nextTick();
 return {sessao,router,finalizar(){version++;win.document.removeEventListener('click',clicar);win.removeEventListener('popstate',popstate);app.unmount();}};
}
