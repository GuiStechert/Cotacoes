import {Component,signal,inject,DestroyRef,OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';import {FormsModule} from '@angular/forms';import {ActivatedRoute,Router,RouterLink} from '@angular/router';import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Cliente,Produto,Detalhe,Entrada,Evento} from './models';
import {Api} from './api';import {Sessao} from './sessao';import {validar,total,moeda,numero,percentual,quantidadeRecomendada,precoFinal,mensagem,ROTULOS} from './regras';import Decimal from 'decimal.js';
@Component({selector:'app-cotacao',standalone:true,imports:[CommonModule,FormsModule,RouterLink],templateUrl:'./cotacao.html'})
export class Cotacao implements OnInit {
 private api=inject(Api);private sessao=inject(Sessao);private router=inject(Router);private route=inject(ActivatedRoute);private destroy=inject(DestroyRef);private rotaId:string|null=null;
 moeda=moeda;numero=numero;percentual=percentual;rotulos=ROTULOS;
 readonly cotacao=signal<Detalhe|null>(null);readonly id=signal<string|null>(null);readonly clientes=signal<Cliente[]>([]);readonly produtos=signal<Produto[]>([]);readonly historico=signal<Evento[]>([]);
 readonly erro=signal('');readonly ocupado=signal(false);readonly confirmando=signal(false);readonly alterado=signal(false);readonly erros=signal<string[]>([]);
 form:Entrada={clienteId:'',faturamento:'',vencimento:'',jurosAnual:0,areaCliente:null,itens:[]};jurosPercentuais:number|null=null;
 ngOnInit(){this.route.paramMap.pipe(takeUntilDestroyed(this.destroy)).subscribe(p=>{const id=p.get('id');if(id&&id!==this.rotaId)void this.carregar(id);});}
 get editavel(){return this.sessao.pode(['COMERCIAL'])&&(!this.id()||this.cotacao()?.status==='RASCUNHO');}
 get totalEstimado(){return this.alterado()||!this.cotacao()?total(this.form.itens):this.cotacao()!.precoTotal;}
 get valida(){return this.validacoes().length===0;}
 private entrada():Entrada{return {clienteId:this.form.clienteId,faturamento:this.form.faturamento,vencimento:this.form.vencimento,jurosAnual:typeof this.jurosPercentuais==='number'&&Number.isFinite(this.jurosPercentuais)?new Decimal(this.jurosPercentuais).div(100).toNumber():NaN,areaCliente:this.form.areaCliente,itens:this.form.itens.map(i=>({produtoId:i.produtoId,quantidade:i.quantidade,preco:i.preco}))};}
 private validacoes(){const e=validar(this.entrada());if(this.form.itens.some(i=>i.produtoId&&!this.produtos().find(p=>p.id===i.produtoId&&p.ativo)))e.push('Há produto indisponível no cadastro.');return e;}
 async carregar(id:string){
  this.rotaId=id;this.id.set(id==='nova'?null:id);this.ocupado.set(true);this.erro.set('');this.erros.set([]);this.confirmando.set(false);
  try{const [clientes,produtos,detalhe,historico]=await Promise.all([this.api.clientes(),this.api.produtos(),id==='nova'?Promise.resolve(null):this.api.detalhe(id),id==='nova'?Promise.resolve([]):this.api.historico(id)]);this.clientes.set(clientes);this.produtos.set(produtos);this.historico.set(historico);this.cotacao.set(detalhe);
   if(detalhe)this.aplicar(detalhe);else{this.form={clienteId:'',faturamento:'',vencimento:'',jurosAnual:0,areaCliente:null,itens:[{produtoId:'',quantidade:null,preco:null}]};this.jurosPercentuais=null;this.alterado.set(false);}
  }catch(e){this.erro.set(mensagem(e));}finally{this.ocupado.set(false);}
 }
 private aplicar(d:Detalhe){this.cotacao.set(d);this.form={clienteId:d.clienteId||'',faturamento:d.faturamento||'',vencimento:d.vencimento||'',jurosAnual:d.jurosAnual??0,areaCliente:d.areaCliente??null,itens:d.itens.map(i=>({produtoId:i.produtoId,quantidade:i.quantidade,preco:i.preco}))};this.jurosPercentuais=new Decimal(d.jurosAnual??0).times(100).toNumber();this.alterado.set(false);}
 marcarAlterado(){if(this.editavel){this.alterado.set(true);this.confirmando.set(false);}}
 adicionar(){if(this.editavel){this.form.itens.push({produtoId:'',quantidade:null,preco:null});this.marcarAlterado();}}
 remover(index:number){if(this.editavel){this.form.itens.splice(index,1);this.marcarAlterado();}}
 podeRecomendar(index:number){const p=this.produtos().find(p=>p.id===this.form.itens[index]?.produtoId);if(!this.editavel||!p)return false;try{quantidadeRecomendada(this.form.areaCliente,p);return true;}catch{return false;}}
 recomendar(index:number){if(!this.podeRecomendar(index))return;this.form.itens[index].quantidade=quantidadeRecomendada(this.form.areaCliente,this.produtos().find(p=>p.id===this.form.itens[index].produtoId)!);this.marcarAlterado();}
 arredondarPreco(index:number){if(!this.editavel)return;try{this.form.itens[index].preco=precoFinal(this.form.itens[index].preco);this.marcarAlterado();}catch{/* O campo obrigatório permanece inválido. */}}
 private async persistir(){const entrada=this.entrada();let id=this.id();if(id)await this.api.editar(id,entrada);else{id=(await this.api.criar(entrada)).id;this.id.set(id);}this.aplicar(await this.api.detalhe(id));return id;}
 async salvar(){if(this.ocupado()||!this.editavel)return;this.erros.set(this.validacoes());if(this.erros().length)return;this.ocupado.set(true);this.erro.set('');try{const id=await this.persistir();this.sessao.aviso.set('Rascunho salvo.');await this.router.navigate(['/cotacao',id],{replaceUrl:true});}catch(e){this.erro.set(mensagem(e));}finally{this.ocupado.set(false);}}
 abrirEnvio(){if(!this.editavel||this.ocupado())return;this.erros.set(this.validacoes());this.confirmando.set(!this.erros().length);}
 async confirmarEnvio(){if(!this.confirmando()||!this.editavel||this.ocupado())return;this.erros.set(this.validacoes());if(this.erros().length)return;this.ocupado.set(true);this.erro.set('');try{const id=await this.persistir();await this.api.enviar(id);this.aplicar(await this.api.detalhe(id));this.historico.set(await this.api.historico(id));this.sessao.aviso.set('Cotação enviada para aprovação.');await this.router.navigate(['/cotacao',id],{replaceUrl:true});}catch(e){this.erro.set(mensagem(e));}finally{this.confirmando.set(false);this.ocupado.set(false);}}
}
