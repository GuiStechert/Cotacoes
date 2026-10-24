import {Component,signal,inject,DestroyRef,OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {RouterLink,Router,ActivatedRoute} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Pagina,Cliente,Filtro} from './models';
import {Api} from './api';import {Sessao} from './sessao';import {moeda,mensagem,ROTULOS} from './regras';
@Component({selector:'app-lista',standalone:true,imports:[CommonModule,FormsModule,RouterLink],templateUrl:'./lista.html'})
export class Lista implements OnInit {
 private api=inject(Api);readonly sessao=inject(Sessao);private router=inject(Router);private route=inject(ActivatedRoute);private destroy=inject(DestroyRef);private inicializada=false;
 moeda=moeda;rotulos=ROTULOS;
 readonly pagina=signal<Pagina>({pagina:0,tamanho:20,total:0,totalPaginas:0,conteudo:[]});readonly clientes=signal<Cliente[]>([]);
 readonly erro=signal('');readonly carregando=signal(false);readonly fila=signal(false);readonly duplicando=signal<string|null>(null);
 filtro:Filtro={page:0};
 ngOnInit(){this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroy)).subscribe(p=>{const fila=p.get('fila')==='1'&&this.sessao.pode(['GESTOR','SUPERVISOR','GERENTE']);if(!this.inicializada||fila!==this.fila()){this.fila.set(fila);this.filtro={page:0};void this.carregar();}});}
 async carregar():Promise<void>{
  this.inicializada=true;this.carregando.set(true);this.erro.set('');
  try{const [pagina,clientes]=await Promise.all([this.api.listar({...this.filtro},this.fila()),this.api.clientes()]);this.pagina.set(pagina);this.clientes.set(clientes);}catch(e){this.erro.set(mensagem(e));}finally{this.carregando.set(false);}
 }
 async filtrar(){if(this.filtro.inicio&&this.filtro.fim&&this.filtro.fim<this.filtro.inicio){this.erro.set('Confira o período informado.');return;}this.filtro.page=0;await this.carregar();}
 async anterior(){if(this.carregando()||this.pagina().pagina<=0)return;this.filtro.page=this.pagina().pagina-1;await this.carregar();}
 async proxima(){if(this.carregando()||this.pagina().pagina+1>=this.pagina().totalPaginas)return;this.filtro.page=this.pagina().pagina+1;await this.carregar();}
 async duplicar(id:string){
  const r=this.pagina().conteudo.find(r=>r.id===id);
  if(this.duplicando()||!r?.podeDuplicar||r.status==='RASCUNHO'||!this.sessao.pode(['COMERCIAL']))return;
  this.duplicando.set(id);this.erro.set('');try{const copia=await this.api.duplicar(id);this.sessao.aviso.set('Cópia criada como rascunho.');await this.router.navigate(['/cotacao',copia.id]);}catch(e){this.erro.set(mensagem(e));}finally{this.duplicando.set(null);}
 }
 get inicio(){return this.pagina().total?this.pagina().pagina*20+1:0;}
 get fim(){return Math.min((this.pagina().pagina+1)*20,this.pagina().total);}
}
