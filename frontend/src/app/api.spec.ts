import {TestBed} from '@angular/core/testing';
import {provideHttpClient,HttpClient,withInterceptors} from '@angular/common/http';
import {provideHttpClientTesting,HttpTestingController} from '@angular/common/http/testing';
import {provideRouter,Router,UrlTree} from '@angular/router';
import {signal} from '@angular/core';
import {describe,it,expect,beforeEach,afterEach,vi} from 'vitest';
import {Api} from './api';import {Sessao} from './sessao';import {csrfInterceptor,perfilGuard} from './acesso';
import {Usuario,Entrada} from './models';
const usuario:Usuario={id:'u',nome:'Ana',perfil:'COMERCIAL'};
const token={token:'token-teste',headerName:'X-CSRF-TOKEN',parameterName:'_csrf'};
describe('Api HTTP',()=>{
 let api:Api;let http:HttpTestingController;
 beforeEach(()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});api=TestBed.inject(Api);http=TestBed.inject(HttpTestingController);});
 afterEach(()=>http.verify());
 it.each(['usuario','csrf','clientes','produtos'] as const)('GET %s',async metodo=>{const promessa=api[metodo]();const req=http.expectOne('/api/'+metodo);expect(req.request.method).toBe('GET');req.flush(metodo==='usuario'?usuario:metodo==='csrf'?token:[]);await promessa;});
 it('lista paginação e filtros no servidor',async()=>{const p=api.listar({page:2,status:'APROVADA',clienteId:'c',inicio:'2026-01-01',fim:'2026-10-24'});const r=http.expectOne(req=>req.url==='/api/cotacoes');expect(r.request.params.get('size')).toBe('20');expect(r.request.params.get('page')).toBe('2');expect(r.request.params.get('clienteId')).toBe('c');expect(r.request.params.get('fim')).toBe('2026-10-24');r.flush({conteudo:[]});await p;});
 it('modo fila usa endpoint de aprovação',async()=>{const p=api.listar({page:0},true);const r=http.expectOne(req=>req.url==='/api/aprovacoes/pendentes');expect(r.request.params.get('size')).toBe('20');r.flush({conteudo:[]});await p;});
 it('criação, edição, envio, cópia e decisões preservam contratos',async()=>{const e={}as Entrada;for(const [url,method,acao,body] of [
  ['/api/cotacoes','POST',()=>api.criar(e),e],['/api/cotacoes/q','PUT',()=>api.editar('q',e),e],['/api/cotacoes/q/enviar','POST',()=>api.enviar('q'),{}],['/api/cotacoes/q/duplicar','POST',()=>api.duplicar('q'),{}],['/api/cotacoes/q/decisoes','POST',()=>api.decidir('q',[{itemId:'i',aprovado:false}]),{itens:[{itemId:'i',aprovado:false}]}]]as const){const p=acao();const r=http.expectOne(url);expect(r.request.method).toBe(method);expect(r.request.body).toEqual(body);expect(r.request.headers.has('X-Usuario')).toBe(false);r.flush({id:'q'});await p;}});
 it('detalhe e histórico por ID',async()=>{const p=api.detalhe('q');http.expectOne('/api/cotacoes/q').flush({id:'q'});await p;const h=api.historico('q');http.expectOne('/api/cotacoes/q/historico').flush([]);await h;});
 it('não repete escrita em falha',async()=>{const p=api.enviar('q');const rejeicao=expect(p).rejects.toMatchObject({status:409});http.expectOne('/api/cotacoes/q/enviar').flush({codigo:'ESTADO_INVALIDO'},{status:409,statusText:'Conflict'});await rejeicao;http.expectNone('/api/cotacoes/q/enviar');});
});
describe('Sessão e guard',()=>{
 let api:{usuario:ReturnType<typeof vi.fn>;csrf:ReturnType<typeof vi.fn>};let sessao:Sessao;
 beforeEach(()=>{api={usuario:vi.fn().mockResolvedValue(usuario),csrf:vi.fn().mockResolvedValue(token)};TestBed.configureTestingModule({providers:[{provide:Api,useValue:api},provideRouter([])]});sessao=TestBed.inject(Sessao);});
 it('identifica antes de obter CSRF e deduplica inicialização',async()=>{await Promise.all([sessao.iniciar(),sessao.iniciar()]);expect(api.usuario).toHaveBeenCalledTimes(1);expect(api.csrf).toHaveBeenCalledTimes(1);expect(api.usuario.mock.invocationCallOrder[0]).toBeLessThan(api.csrf.mock.invocationCallOrder[0]);expect(sessao.usuario()).toEqual(usuario);expect(sessao.token()).toEqual(token);});
 it('falha não inventa perfil nem consulta CSRF',async()=>{api.usuario.mockRejectedValue({error:{codigo:'ACESSO_NEGADO'}});expect(await sessao.iniciar()).toBeNull();expect(sessao.usuario()).toBeNull();expect(sessao.token()).toBeNull();expect(sessao.erro()).toBe('Você não tem permissão para esta ação.');expect(api.csrf).not.toHaveBeenCalled();});
 it('perfil desconhecido bloqueia sessão',async()=>{api.usuario.mockResolvedValue({...usuario,perfil:'ADMIN'});expect(await sessao.iniciar()).toBeNull();expect(sessao.pode(['COMERCIAL'])).toBe(false);});
 it('guard permite Comercial e redireciona aprovação',async()=>{const executar=(perfis:Usuario['perfil'][])=>TestBed.runInInjectionContext(()=>perfilGuard(perfis)({}as never,{}as never));expect(await executar(['COMERCIAL'])).toBe(true);const resultado=await executar(['GERENTE']);expect(resultado).toBeInstanceOf(UrlTree);expect(TestBed.inject(Router).serializeUrl(resultado as UrlTree)).toBe('/cotacoes');});
});
describe('CSRF restrito à API da mesma origem',()=>{
 let http:HttpTestingController;let cliente:HttpClient;
 beforeEach(()=>{TestBed.configureTestingModule({providers:[provideHttpClient(withInterceptors([csrfInterceptor])),provideHttpClientTesting(),{provide:Sessao,useValue:{token:signal(token)}}]});http=TestBed.inject(HttpTestingController);cliente=TestBed.inject(HttpClient);});
 afterEach(()=>http.verify());
 it('anexa token e credenciais na escrita interna',()=>{cliente.post('/api/cotacoes',{}).subscribe();const r=http.expectOne('/api/cotacoes');expect(r.request.headers.get('X-CSRF-TOKEN')).toBe(token.token);expect(r.request.withCredentials).toBe(true);r.flush({});});
 it('não vaza token para origem externa nem GET',()=>{for(const url of ['https://externo.test/api','/api/usuario']){cliente.get(url).subscribe();const r=http.expectOne(url);expect(r.request.headers.has('X-CSRF-TOKEN')).toBe(false);r.flush({});}});
});
