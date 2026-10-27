import {it,expect,vi,signal} from './harness.mjs';
import {Api} from '../public/api.js';
import {Sessao} from '../public/sessao.js';
import {resolverRota,criarNavegador} from '../public/routes.js';

it('HTTP preserva endpoints, métodos, corpos, codificação e CSRF sem impersonação',async()=>{
 const fetch=vi.fn().mockResolvedValue({ok:true,status:200,json:async()=>({id:'q'})});
 const api=new Api(fetch,()=>({headerName:'X-CSRF-TOKEN',token:'segredo'}));
 for(const method of ['usuario','csrf','clientes','produtos']){await api[method]();expect(fetch.mock.lastCall[0]).toBe('/api/'+method);expect(fetch.mock.lastCall[1].headers['X-CSRF-TOKEN']).toBeUndefined();}
 await api.listar({page:2,status:'APROVADA',clienteId:'c',inicio:'2026-01-01',fim:'2026-10-24'});
 expect(fetch.mock.lastCall[0]).toBe('/api/cotacoes?page=2&size=20&status=APROVADA&clienteId=c&inicio=2026-01-01&fim=2026-10-24');
 await api.listar({page:0,status:'APROVADA'},true);expect(fetch.mock.lastCall[0]).toBe('/api/aprovacoes/pendentes?page=0&size=20');
 for(const [action,url,method,body] of [
 [()=>api.criar({clienteId:'c'}),'/api/cotacoes','POST',{clienteId:'c'}],
 [()=>api.editar('q',{clienteId:'c'}),'/api/cotacoes/q','PUT',{clienteId:'c'}],
 [()=>api.enviar('q'),'/api/cotacoes/q/enviar','POST',{}],
 [()=>api.duplicar('q'),'/api/cotacoes/q/duplicar','POST',{}],
 [()=>api.decidir('q',[{itemId:'i',aprovado:false}]),'/api/cotacoes/q/decisoes','POST',{itens:[{itemId:'i',aprovado:false}]}]]){
  await action();const [actual,options]=fetch.mock.lastCall;expect(actual).toBe(url);expect(options.method).toBe(method);expect(JSON.parse(options.body)).toEqual(body);expect(options.credentials).toBe('same-origin');expect(options.headers['X-CSRF-TOKEN']).toBe('segredo');expect(options.headers['X-Usuario']).toBeUndefined();
 }
 await api.detalhe('a/b');expect(fetch.mock.lastCall[0]).toBe('/api/cotacoes/a%2Fb');
 await api.historico('q');expect(fetch.mock.lastCall[0]).toBe('/api/cotacoes/q/historico');
});
it('204 não lê JSON e erro 409 preserva código sem repetir a escrita',async()=>{
 const json=vi.fn();const fetch=vi.fn().mockResolvedValueOnce({ok:true,status:204,json}).mockResolvedValue({ok:false,status:409,json:async()=>({codigo:'ESTADO_INVALIDO'})});
 const api=new Api(fetch);await api.editar('q',{});expect(json).not.toHaveBeenCalled();
 await expect(api.enviar('q')).rejects.toMatchObject({status:409,error:{codigo:'ESTADO_INVALIDO'}});expect(fetch).toHaveBeenCalledTimes(2);
});
it('não encaminha CSRF ou requisições para origens externas',async()=>{
 const fetch=vi.fn();const api=new Api(fetch,()=>({headerName:'X-CSRF-TOKEN',token:'segredo'}));
 await expect(api.request('https://externo.test/api/cotacoes','POST',{})).rejects.toThrow('URL fora da API');expect(fetch).not.toHaveBeenCalled();
});
it('CSRF inválido impede a sessão e não mantém perfil autenticado',async()=>{
 for(const token of [null,{token:'t',headerName:'X-Externo'},{headerName:'X-CSRF-TOKEN',token:''}]){
  const api={usuario:vi.fn().mockResolvedValue({id:'u',nome:'Ana',perfil:'COMERCIAL'}),csrf:vi.fn().mockResolvedValue(token)};
  const s=new Sessao(api);expect(await s.iniciar()).toBeNull();expect(s.usuario()).toBeNull();expect(s.token()).toBeNull();
 }
});
it('sessão deduplica, identifica antes do CSRF e valida perfil/token',async()=>{
 const api={usuario:vi.fn().mockResolvedValue({id:'u',nome:'Ana',perfil:'COMERCIAL'}),csrf:vi.fn().mockResolvedValue({headerName:'X-CSRF-TOKEN',token:'t'})};
 const s=new Sessao(api);await Promise.all([s.iniciar(),s.iniciar()]);expect(api.usuario).toHaveBeenCalledTimes(1);expect(api.csrf).toHaveBeenCalledTimes(1);expect(api.usuario.mock.invocationCallOrder[0]).toBeLessThan(api.csrf.mock.invocationCallOrder[0]);expect(s.pode(['COMERCIAL'])).toBe(true);
 for(const perfil of ['ADMIN',null]){api.usuario.mockResolvedValue({id:'u',nome:'Ana',perfil});api.csrf.mockClear();expect(await s.iniciar(true)).toBeNull();expect(s.token()).toBeNull();expect(s.pode(['COMERCIAL'])).toBe(false);expect(api.csrf).not.toHaveBeenCalled();}
 api.usuario.mockRejectedValue({error:{codigo:'ACESSO_NEGADO'}});expect(await s.iniciar(true)).toBeNull();expect(s.erro()).toBe('Você não tem permissão para esta ação.');
});
it('rotas aplicam perfis, aceitam entrada direta e navegação volta ao topo',async()=>{
 for(const perfil of ['COMERCIAL','GESTOR','SUPERVISOR','GERENTE']){
  expect(resolverRota('/cotacoes',perfil).tela).toBe('lista');
  expect(resolverRota('/cotacao/nova',perfil).tela).toBe(perfil==='COMERCIAL'?'cotacao':'lista');
  expect(resolverRota('/aprovacao/q',perfil).tela).toBe(perfil==='COMERCIAL'?'lista':'aprovacao');
 }
 expect(resolverRota('/cotacoes',null)).toBeNull();expect(resolverRota('/inexistente','COMERCIAL').tela).toBe('lista');
 const win={history:{pushState:vi.fn(),replaceState:vi.fn()},scrollTo:vi.fn()};const mudou=vi.fn();const router=criarNavegador(win,mudou);await router.navigate(['/cotacao','q']);expect(win.history.pushState).toHaveBeenCalledWith({},'','/cotacao/q');expect(win.scrollTo).toHaveBeenCalledWith(0,0);expect(mudou).toHaveBeenCalledTimes(1);
});
