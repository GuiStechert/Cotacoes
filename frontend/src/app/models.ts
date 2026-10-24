export type Perfil='COMERCIAL'|'GESTOR'|'SUPERVISOR'|'GERENTE';
export type Status='RASCUNHO'|'PENDENTE_APROVACAO'|'APROVADA'|'APROVADA_PARCIAL'|'REPROVADA';
export type ItemStatus='RASCUNHO'|'PENDENTE'|'APROVADO'|'REPROVADO';
export interface Usuario {id:string;nome:string;perfil:Perfil}
export interface Cliente {id:string;nome:string}
export interface Produto {id:string;nome:string;principioAtivo:string|null;embalagem:number|null;doseRecomendada:number|null;aplicacoesRecomendadas:number|null;unidadeMedida:string;margemRecomendada:number|null;ativo:boolean;custoReal?:number|null}
export interface ItemEntrada {produtoId:string;quantidade:number|null;preco:number|null}
export interface Entrada {clienteId:string;faturamento:string;vencimento:string;jurosAnual:number;areaCliente:number|null;itens:ItemEntrada[]}
export interface Item {id:string;produtoId:string;quantidade:number;preco:number;status:ItemStatus;margem?:number|null;margemReal?:number|null;custoReal?:number|null}
export interface Detalhe {id:string;clienteId?:string;faturamento?:string;vencimento?:string;jurosAnual?:number;areaCliente?:number|null;status:Status;perfilAprovador:Perfil|null;precoTotal:number;margemPonderada?:number|null;margemRealPonderada?:number|null;itens:Item[]}
export interface Resumo {id:string;clienteId:string;clienteNome:string;solicitanteNome:string;criadoEm:string;status:Status;perfilAprovador:Perfil|null;precoTotal:number;podeEditar:boolean;podeDuplicar:boolean}
export interface Pagina {pagina:number;tamanho:number;total:number;totalPaginas:number;conteudo:Resumo[]}
export interface Filtro {page:number;status?:string;clienteId?:string;inicio?:string;fim?:string}
export interface Evento {id:string;itemId:string;usuarioId:string;autorNome:string;criadoEm:string;decisao:'APROVADO'|'REPROVADO';comentario:string|null}
export interface Decisao {itemId:string;aprovado:boolean;comentario?:string}
export interface TokenCsrf {token:string;headerName:string;parameterName:string}
