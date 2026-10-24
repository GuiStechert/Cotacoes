import {Entrada,Produto} from './models';
import Decimal from 'decimal.js';
const D=Decimal.clone({precision:40});
const positivo=(v:unknown):v is number=>typeof v==='number'&&Number.isFinite(v)&&v>0;
export function quantidadeRecomendada(area:unknown,produto:Produto):number {
 if(!positivo(area)||!produto.ativo||!positivo(produto.doseRecomendada)||!positivo(produto.aplicacoesRecomendadas))throw new Error('Parâmetros de quantidade inválidos');
 return new D(area).times(produto.doseRecomendada).times(produto.aplicacoesRecomendadas).toDecimalPlaces(2,D.ROUND_CEIL).toNumber();
}
export function precoFinal(preco:unknown):number {if(!positivo(preco))throw new Error('Preço inválido');return new D(preco).toDecimalPlaces(2,D.ROUND_CEIL).toNumber();}
const dataValida=(s:unknown):s is string=>typeof s==='string'&&/^\d{4}-\d{2}-\d{2}$/.test(s)&&Number.isFinite(Date.parse(s))&&new Date(s).toISOString().slice(0,10)===s;
export function validar(e:Entrada):string[] {
 if(!e)return ['Preencha os dados da cotação.'];const erros:string[]=[];
 if(!e.clienteId)erros.push('Selecione um cliente.');
 if(!dataValida(e.faturamento)||!dataValida(e.vencimento)||e.vencimento<e.faturamento)erros.push('Informe datas válidas e vencimento não anterior ao faturamento.');
 if(typeof e.jurosAnual!=='number'||!Number.isFinite(e.jurosAnual)||e.jurosAnual<0)erros.push('Informe juros anuais não negativos.');
 if(e.areaCliente!==null&&!positivo(e.areaCliente))erros.push('Informe uma área positiva ou deixe em branco.');
 if(!Array.isArray(e.itens)||!e.itens.length)erros.push('Inclua ao menos um produto.');
 else {
  if(e.itens.some(i=>!i||!i.produtoId))erros.push('Selecione o produto de cada item.');
  if(e.itens.some(i=>!i||!positivo(i.quantidade)||new D(i.quantidade).decimalPlaces()>2))erros.push('Informe quantidades positivas com até duas casas decimais.');
  if(e.itens.some(i=>!i||!positivo(i.preco)))erros.push('Informe preços unitários positivos.');
 }
 return erros;
}
function formatar(valor:number|null|undefined,modo:Decimal.Rounding=Decimal.ROUND_HALF_UP):string {
 if(valor===null||valor===undefined||!Number.isFinite(valor))return '—';
 const [inteiro,decimal]=new D(valor).toDecimalPlaces(2,modo).toFixed(2).split('.');return inteiro.replace(/\B(?=(\d{3})+(?!\d))/g,'.')+','+decimal;
}
export function moeda(valor:number|null|undefined):string {const v=formatar(valor);return v==='—'?v:'R$ '+v;}
export function percentual(valor:number|null|undefined):string {return valor===null||valor===undefined?'—':formatar(new D(valor).times(100).toNumber(),Decimal.ROUND_FLOOR)+'%';}
export function numero(valor:number|null|undefined):string {return formatar(valor);}
export function total(itens:Entrada['itens']):number {return itens.reduce((s,i)=>positivo(i.quantidade)&&positivo(i.preco)?s.plus(new D(precoFinal(i.preco)).times(i.quantidade)):s,new D(0)).toNumber();}
export function mensagem(erro:unknown):string {
 const mensagens:Record<string,string>={NAO_AUTENTICADO:'Não foi possível autenticar sua conta. Tente novamente.',ACESSO_NEGADO:'Você não tem permissão para esta ação.',CUSTO_AUSENTE:'Há produto sem custo cadastrado. Solicite a atualização do cadastro.',DADOS_INVALIDOS:'Confira os campos e tente novamente.',CLIENTE_INEXISTENTE:'O cliente não está disponível no cadastro.',PRODUTO_INDISPONIVEL:'Há produto indisponível para novas cotações.',COTACAO_INEXISTENTE:'Esta cotação não foi encontrada.',ESTADO_INVALIDO:'A cotação mudou ou já foi concluída. Atualize a consulta.',CONFLITO_CONCORRENCIA:'Outra pessoa atualizou esta cotação. Recarregue antes de continuar.',DECISOES_INVALIDAS:'Os itens da cotação mudaram. Atualize a consulta.'};
 const codigo=(erro as {error?:{codigo?:string}}|null)?.error?.codigo;
 return codigo&&mensagens[codigo]||'Não foi possível concluir a operação. Tente novamente.';
}
export const ROTULOS:Record<string,string>={RASCUNHO:'Rascunho',PENDENTE_APROVACAO:'Pendente de aprovação',APROVADA:'Aprovada',APROVADA_PARCIAL:'Aprovada parcialmente',REPROVADA:'Reprovada',COMERCIAL:'Comercial',GESTOR:'Gestor',SUPERVISOR:'Supervisor',GERENTE:'Gerente',PENDENTE:'Pendente',APROVADO:'Aprovado',REPROVADO:'Reprovado'};
