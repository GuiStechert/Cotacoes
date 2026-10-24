import {TestBed} from '@angular/core/testing';import {ViewportScroller} from '@angular/common';import {Router} from '@angular/router';import {signal,ApplicationRef,ApplicationInitStatus} from '@angular/core';import {it,expect,vi} from 'vitest';
import {appConfig} from './app.config';import {Api} from './api';import {Sessao} from './sessao';import {App} from './app';
it('regressão: navegar de uma lista rolada para cotação retorna ao início',async()=>{
 const usuario={id:'u',nome:'Ana',perfil:'COMERCIAL'};const scroller={setOffset:vi.fn(),getScrollPosition:()=>[0,700],scrollToPosition:vi.fn(),scrollToAnchor:vi.fn(),setHistoryScrollRestoration:vi.fn()};
 const api={listar:vi.fn().mockResolvedValue({pagina:0,tamanho:20,total:0,totalPaginas:0,conteudo:[]}),clientes:vi.fn().mockResolvedValue([]),produtos:vi.fn().mockResolvedValue([])};
 TestBed.configureTestingModule({providers:[...appConfig.providers,{provide:ViewportScroller,useValue:scroller},{provide:Api,useValue:api},{provide:Sessao,useValue:{usuario:signal(usuario),token:signal(null),carregando:signal(false),erro:signal(''),aviso:signal(''),iniciar:vi.fn().mockResolvedValue(usuario),pode:()=>true}}]});
 const app=TestBed.inject(ApplicationRef);await TestBed.inject(ApplicationInitStatus).donePromise;
 const host=document.createElement('app-root');document.body.appendChild(host);const raiz=app.bootstrap(App,host);
 try{await app.whenStable();await vi.waitFor(()=>expect(scroller.scrollToPosition).toHaveBeenCalledWith([0,0]));scroller.scrollToPosition.mockClear();await TestBed.inject(Router).navigateByUrl('/cotacao/nova');await vi.waitFor(()=>expect(scroller.scrollToPosition).toHaveBeenCalledWith([0,0]));}finally{raiz.destroy();host.remove();}
});
