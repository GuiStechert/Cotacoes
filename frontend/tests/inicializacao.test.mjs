import {it,expect,vi,window} from './harness.mjs';
import {readFileSync} from 'node:fs';
import {iniciarAplicacao} from '../public/bootstrap.js';
const template=async name=>readFileSync(new URL(`../public/${name}.html`,import.meta.url),'utf8');
it('entrada direta inicia sessão, monta lista, navega sem recarga e volta ao topo',async()=>{
 window.history.replaceState({},'','/cotacoes');window.scrollTo=vi.fn();window.document.body.innerHTML='<div id="app"></div>';
 const api={usuario:vi.fn().mockResolvedValue({id:'u',nome:'Ana Silva',perfil:'COMERCIAL'}),csrf:vi.fn().mockResolvedValue({headerName:'X-CSRF-TOKEN',token:'t'}),listar:vi.fn().mockResolvedValue({pagina:0,total:0,totalPaginas:0,conteudo:[]}),clientes:vi.fn().mockResolvedValue([]),produtos:vi.fn().mockResolvedValue([])};
 const app=await iniciarAplicacao({window,api,template});
 await Vue.nextTick();expect(window.document.body.textContent).toContain('Nenhuma cotação encontrada');
 window.document.querySelector('a[href="/cotacao/nova"]').click();
 await new Promise(resolve=>setImmediate(resolve));await Vue.nextTick();
 expect(window.location.pathname).toBe('/cotacao/nova');expect(window.document.querySelector('[data-action="salvar"]')).not.toBeNull();expect(window.scrollTo).toHaveBeenCalledWith(0,0);expect(api.produtos).toHaveBeenCalledTimes(1);
 window.history.replaceState({},'','/cotacoes');window.dispatchEvent(new window.PopStateEvent('popstate'));await new Promise(resolve=>setImmediate(resolve));await Vue.nextTick();expect(api.listar).toHaveBeenCalledTimes(2);
 app.finalizar();
});
it('falha de sessão bloqueia consultas e não monta uma quarta tela',async()=>{
 window.document.body.innerHTML='<div id="app"></div>';
 const api={usuario:vi.fn().mockRejectedValue({error:{codigo:'ACESSO_NEGADO'}}),csrf:vi.fn(),listar:vi.fn()};
 const app=await iniciarAplicacao({window,api,template});expect(window.document.body.textContent).toContain('Você não tem permissão');expect(api.csrf).not.toHaveBeenCalled();expect(api.listar).not.toHaveBeenCalled();app.finalizar();
});
