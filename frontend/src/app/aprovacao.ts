import {Component,signal,inject,DestroyRef,OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';import {FormsModule} from '@angular/forms';import {ActivatedRoute,RouterLink} from '@angular/router';import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Detalhe,Produto,Evento} from './models';
import {Api} from './api';import {Sessao} from './sessao';import {moeda,numero,percentual,mensagem,ROTULOS} from './regras';
@Component({selector:'app-aprovacao',standalone:true,imports:[CommonModule,FormsModule,RouterLink],templateUrl:'./aprovacao.html'})
export class Aprovacao implements OnInit {
 private api=inject(Api);private sessao=inject(Sessao);private route=inject(ActivatedRoute);private destroy=inject(DestroyRef);private rotaId:string|null=null;
 moeda=moeda;numero=numero;percentual=percentual;rotulos=ROTULOS;
 readonly cotacao=signal<Detalhe|null>(null);readonly produtos=signal<Produto[]>([]);readonly historico=signal<Evento[]>([]);
 readonly erro=signal('');readonly ocupado=signal(false);readonly confirmando=signal(false);
 private estadoEscolhas=signal<Record<string,{aprovado:boolean;comentario:string}>>({});
 get escolhas(){return this.estadoEscolhas();}
 set escolhas(valor:Record<string,{aprovado:boolean;comentario:string}>){this.estadoEscolhas.set(valor);}
 ngOnInit(){this.route.paramMap.pipe(takeUntilDestroyed(this.destroy)).subscribe(p=>{const id=p.get('id');if(id&&id!==this.rotaId)void this.carregar(id);});}
 get podeDecidir(){const c=this.cotacao();return !!c&&c.status==='PENDENTE_APROVACAO'&&c.perfilAprovador===this.sessao.usuario()?.perfil&&this.sessao.pode(['GESTOR','SUPERVISOR','GERENTE'])&&c.itens.some(i=>i.status==='PENDENTE');}
 async carregar(id:string){this.rotaId=id;this.ocupado.set(true);this.erro.set('');this.confirmando.set(false);try{const [cotacao,produtos,historico]=await Promise.all([this.api.detalhe(id),this.api.produtos(),this.api.historico(id)]);this.cotacao.set(cotacao);this.produtos.set(produtos);this.historico.set(historico);this.escolhas={};for(const item of cotacao.itens)if(item.status==='PENDENTE')this.escolhas[item.id]={aprovado:true,comentario:''};}catch(e){this.erro.set(mensagem(e));}finally{this.ocupado.set(false);}}
 alterar(id:string,aprovado:boolean){if(this.podeDecidir&&!this.ocupado()&&this.escolhas[id]){this.escolhas={...this.escolhas,[id]:{...this.escolhas[id],aprovado}};this.confirmando.set(false);}}
 abrirConfirmacao(){if(this.podeDecidir&&!this.ocupado())this.confirmando.set(true);}
 async confirmar(){
  if(!this.confirmando()||!this.podeDecidir||this.ocupado())return;
  const c=this.cotacao()!;const itens=c.itens.filter(i=>i.status==='PENDENTE').map(i=>{const d=this.escolhas[i.id];return {itemId:i.id,aprovado:d.aprovado,...(!d.aprovado&&d.comentario.trim()?{comentario:d.comentario.trim()}:{})};});
  this.ocupado.set(true);this.erro.set('');try{await this.api.decidir(c.id,itens);await this.carregar(c.id);this.sessao.aviso.set('Decisões registradas.');}catch(e){const texto=mensagem(e);const codigo=(e as {error?:{codigo?:string}})?.error?.codigo;if(codigo==='CONFLITO_CONCORRENCIA'||codigo==='ESTADO_INVALIDO')await this.carregar(c.id);this.erro.set(texto);}finally{this.ocupado.set(false);this.confirmando.set(false);}
 }
 nome(produto:string){return this.produtos().find(p=>p.id===produto)?.nome||produto;}
 comentario(id:string){return this.historico().find(e=>e.itemId===id)?.comentario;}
 get aprovados(){return Object.values(this.escolhas).filter(e=>e.aprovado).length;}
 get reprovados(){return Object.values(this.escolhas).filter(e=>!e.aprovado).length;}
}
