import {describe,it,expect} from 'vitest';
import {quantidadeRecomendada,precoFinal,validar,moeda,percentual,total,mensagem,numero} from './regras';
import {Entrada,Produto} from './models';
const produto:Produto={id:'p',nome:'Produto',principioAtivo:null,embalagem:20,doseRecomendada:0.1231,aplicacoesRecomendadas:1,unidadeMedida:'L',margemRecomendada:0.1,ativo:true};
const entrada=():Entrada=>({clienteId:'c',faturamento:'2026-01-01',vencimento:'2026-08-01',jurosAnual:0.12,areaCliente:null,itens:[{produtoId:'p',quantidade:1.23,preco:100.001}]});
describe('contratos decimais e validação',()=>{
 it('quantidade usa a mesma unidade sem embalagem e arredonda para cima',()=>{expect(quantidadeRecomendada(10,produto)).toBe(1.24);expect(quantidadeRecomendada(100,{...produto,doseRecomendada:0.1,aplicacoesRecomendadas:3})).toBe(30);});
 it.each([null,0,-1,NaN,Infinity,'10',{},true])('rejeita área inválida %s',area=>{expect(()=>quantidadeRecomendada(area,produto)).toThrow('Parâmetros de quantidade inválidos');});
 it('exige parâmetros positivos e produto ativo',()=>{for(const p of [{...produto,doseRecomendada:null},{...produto,aplicacoesRecomendadas:0},{...produto,ativo:false}])expect(()=>quantidadeRecomendada(10,p)).toThrow('Parâmetros de quantidade inválidos');});
 it.each([[100.001,100.01],[100.990,100.99],[0.001,0.01]])('preço %s vira %s', (p,esperado)=>expect(precoFinal(p)).toBe(esperado));
 it('preço inválido não é convertido silenciosamente',()=>{for(const p of [0,-1,null,'1',true,NaN,Infinity])expect(()=>precoFinal(p)).toThrow('Preço inválido');});
 it('total usa preço final e quantidade decimal',()=>{expect(total([{produtoId:'p',quantidade:2,preco:100.001},{produtoId:'p',quantidade:0.1,preco:0.1}])).toBe(200.03);});
 it('formatação pt-BR e percentual para baixo',()=>{expect(moeda(1234.5)).toBe('R$ 1.234,50');expect(percentual(0.089999)).toBe('8,99%');expect(percentual(0.10)).toBe('10,00%');expect(percentual(null)).toBe('—');expect(numero(1.2)).toBe('1,20');});
 it('valida entrada e aceita a quantidade com duas casas',()=>expect(validar(entrada())).toEqual([]));
 it('rejeita quantidade de três casas sem arredondar a digitada',()=>{const e=entrada();e.itens[0].quantidade=1.231;expect(validar(e)).toContain('Informe quantidades positivas com até duas casas decimais.');});
 it('valida campos, datas reais e itens vazios',()=>{for(const e of [{...entrada(),clienteId:''},{...entrada(),faturamento:'2026-02-29'},{...entrada(),vencimento:'2025-12-31'},{...entrada(),jurosAnual:-1},{...entrada(),areaCliente:0},{...entrada(),itens:[]}])expect(validar(e).length).toBeGreaterThan(0);});
 it('nulos, tipos e valores não finitos não entram no comando',()=>{for(const v of [null,NaN,Infinity,0,-1,'2',false]){const e=entrada();e.itens[0].quantidade=v as number;expect(validar(e).length).toBeGreaterThan(0);}expect(validar(null as unknown as Entrada).length).toBeGreaterThan(0);});
 it('erro da API é traduzido sem refletir SQL ou mensagens arbitrárias',()=>{expect(mensagem({error:{codigo:'ACESSO_NEGADO'}})).toBe('Você não tem permissão para esta ação.');expect(mensagem({error:{codigo:'SQL segredo',message:'senha'}})).toBe('Não foi possível concluir a operação. Tente novamente.');});
});
