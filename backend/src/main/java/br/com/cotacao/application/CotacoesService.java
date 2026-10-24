package br.com.cotacao.application;
import java.time.Clock;
import java.util.*;
import java.math.*;
import br.com.cotacao.domain.Financeiro;
import br.com.cotacao.domain.EstadosCotacao;
import br.com.cotacao.infrastructure.CotacaoRepository.*;
import br.com.cotacao.infrastructure.CotacaoRepository;
import org.springframework.transaction.support.TransactionOperations;
import static br.com.cotacao.application.Cotacoes.*;
public class CotacoesService {
 private final CotacaoRepository repository;
 private final TransactionOperations transacoes;
 private final Identidade identidade;
 private final Clock clock;
 public CotacoesService(CotacaoRepository repository, TransactionOperations transacoes, Identidade identidade, Clock clock) {
  this.repository=Objects.requireNonNull(repository);this.transacoes=Objects.requireNonNull(transacoes);this.identidade=Objects.requireNonNull(identidade);this.clock=Objects.requireNonNull(clock);
 }
 public UUID criar(Entrada entrada) {
  return transacoes.execute(tx->{var usuario=usuario();comercial(usuario);var e=validar(entrada);var id=UUID.randomUUID();repository.criar(id,usuario.id(),e,clock.instant());return id;});
 }
 public void decidir(UUID id, Decisoes decisoes) {
  transacoes.executeWithoutResult(tx->{
   var u=usuario();aprovador(u);
   var registro=carregar(id,true);
   if(!registro.status().equals("PENDENTE_APROVACAO"))throw new Erro("ESTADO_INVALIDO");
   autorizarConsulta(u,registro);
   if(decisoes==null||decisoes.itens()==null)throw new Erro("DADOS_INVALIDOS");
   var propostas=new LinkedHashMap<UUID,DecisaoItem>();
   for(var decisao:decisoes.itens()){
    if(decisao==null||decisao.itemId()==null||decisao.aprovado()==null)throw new Erro("DADOS_INVALIDOS");
    if(propostas.putIfAbsent(decisao.itemId(),decisao)!=null)throw new Erro("DECISOES_INVALIDAS");
   }
   var pendentes=new HashSet<UUID>();
   for(var item:registro.itens()){
    if(item.status().equals("PENDENTE"))pendentes.add(item.id());
    else if(!Set.of("APROVADO","REPROVADO").contains(item.status()))throw new Erro("ESTADO_INVALIDO");
    else if(propostas.containsKey(item.id()))throw new Erro("ESTADO_INVALIDO");
   }
   if(pendentes.isEmpty())throw new Erro("ESTADO_INVALIDO");
   if(!pendentes.equals(propostas.keySet()))throw new Erro("DECISOES_INVALIDAS");
   var agora=clock.instant();
   var estados=new ArrayList<String>();
   for(var item:registro.itens()){
    var proposta=propostas.get(item.id());
    if(proposta==null){estados.add(item.status());continue;}
    String estado=proposta.aprovado()?"APROVADO":"REPROVADO";
    repository.decidirItem(id,item.id(),estado,proposta.comentario());
    repository.registrarDecisao(item.id(),u.id(),estado,proposta.comentario(),agora);
    estados.add(estado);
   }
   repository.concluir(id,EstadosCotacao.consolidar(estados),agora);
  });
 }
 public List<EventoAprovacao> historico(UUID id) {
  return transacoes.execute(tx->{
   var u=usuario();var r=carregar(id,false);autorizarConsulta(u,r);
   return repository.historico(id);
  });
 }
 public Pagina pendentes(int pagina, int tamanho) {
  return transacoes.execute(tx->{
   var u=usuario();aprovador(u);
   return listarAutorizada(u,new Filtro(pagina,tamanho,"PENDENTE_APROVACAO",null,null,null));
  });
 }
 public Pagina listar(Filtro filtro) {
  return transacoes.execute(tx->listarAutorizada(usuario(),filtro));
 }
 private Pagina listarAutorizada(Usuario u,Filtro filtro) {
   var estados=Set.of("RASCUNHO","PENDENTE_APROVACAO","APROVADA","APROVADA_PARCIAL","REPROVADA");
   if(filtro==null||filtro.pagina()<0||filtro.tamanho()!=20||filtro.status()!=null&&!estados.contains(filtro.status())
      ||filtro.inicio()!=null&&filtro.fim()!=null&&filtro.fim().isBefore(filtro.inicio()))throw new Erro("DADOS_INVALIDOS");
   java.time.Instant inicio,fim;
   try {
    inicio=filtro.inicio()==null?null:filtro.inicio().atStartOfDay(clock.getZone()).toInstant();
    fim=filtro.fim()==null?null:filtro.fim().plusDays(1).atStartOfDay(clock.getZone()).toInstant();
   } catch(java.time.DateTimeException ex){throw new Erro("DADOS_INVALIDOS");}
   var registros=repository.listar(u,filtro,inicio,fim);
   boolean comercial=u.perfil().equals("COMERCIAL");
   var resumos=registros.linhas().stream().map(r->new Resumo(r.id(),r.clienteId(),r.clienteNome(),r.solicitanteNome(),r.criadoEm(),r.status(),r.perfilAprovador(),r.precoTotal(),comercial&&r.status().equals("RASCUNHO"),comercial&&!r.status().equals("RASCUNHO"))).toList();
   return new Pagina(filtro.pagina(),20,registros.total(),registros.total()/20+(registros.total()%20==0?0:1),resumos);
 }
 public UUID duplicar(UUID id) {
  return transacoes.execute(tx->{
   var u=usuario();comercial(u);var original=carregar(id,false);proprio(u,original);
   if(original.status().equals("RASCUNHO"))throw new Erro("ESTADO_INVALIDO");
   var entrada=validar(original.entrada());var novo=UUID.randomUUID();
   repository.criar(novo,u.id(),entrada,clock.instant());return novo;
  });
 }
 public void editar(UUID id, Entrada entrada) {
  transacoes.executeWithoutResult(tx->{var u=usuario();comercial(u);var r=carregar(id,true);proprio(u,r);rascunho(r);repository.editar(id,validar(entrada),clock.instant());});
 }
 public void enviar(UUID id) {
  transacoes.executeWithoutResult(tx->{
   var u=usuario();comercial(u);var r=carregar(id,true);proprio(u,r);rascunho(r);validar(r.entrada());
   String perfil="GESTOR";
   for(var item:r.itens()) {
    var custos=repository.custos(item.produtoId());
    if(custos.comercial()==null||custos.real()==null)throw new Erro("CUSTO_AUSENTE");
    var margem=margem(custos.comercial(),item.preco(),r.entrada());var real=margem(custos.real(),item.preco(),r.entrada());
    String exigido=Financeiro.alcada(real);
    if(exigido.equals("GERENTE")||exigido.equals("SUPERVISOR")&&perfil.equals("GESTOR"))perfil=exigido;
    repository.snapshot(new Item(item.id(),item.produtoId(),item.quantidade(),item.preco(),custos.comercial(),custos.real(),margem,real,"PENDENTE"));
   }
   repository.enviar(id,perfil,clock.instant());
  });
 }
 public Visao consultar(UUID id) {
  return transacoes.execute(tx->{
   var u=usuario();var r=carregar(id,false);autorizarConsulta(u,r);
   if(u.perfil().equals("COMERCIAL")) {
    proprio(u,r);var itens=new ArrayList<ItemComercial>();
    for(var item:r.itens()) {
     BigDecimal margem=item.margem();
     if(r.status().equals("RASCUNHO")){var custo=repository.custos(item.produtoId()).comercial();margem=custo==null?null:margem(custo,item.preco(),r.entrada());}
     itens.add(new ItemComercial(item.id(),item.produtoId(),item.quantidade(),item.preco(),margem,item.status()));
    }
    var e=r.entrada();
    return new Comercial(id,e.clienteId(),e.faturamento(),e.vencimento(),e.jurosAnual(),e.areaCliente(),r.status(),r.perfilAprovador(),total(r.itens()),ponderada(itens.stream().map(ItemComercial::margem).toList(),r.itens()),List.copyOf(itens));
   }
   if(!u.perfil().equals(r.perfilAprovador()))throw new Erro("ACESSO_NEGADO");
   var itens=r.itens().stream().map(v->new ItemAprovador(v.id(),v.produtoId(),v.quantidade(),v.preco(),v.custoReal(),v.margemReal(),v.status())).toList();
   return new Aprovador(id,r.status(),r.perfilAprovador(),total(r.itens()),ponderada(itens.stream().map(ItemAprovador::margemReal).toList(),r.itens()),itens);
  });
 }
 private Usuario usuario() {
  var conta=identidade.contaAutenticada();if(conta==null||conta.isBlank())throw new Erro("NAO_AUTENTICADO");
  var u=repository.usuario(conta);if(u==null)throw new Erro("ACESSO_NEGADO");return u;
 }
 private void comercial(Usuario u){if(!u.perfil().equals("COMERCIAL"))throw new Erro("ACESSO_NEGADO");}
 private void aprovador(Usuario u){if(!Set.of("GESTOR","SUPERVISOR","GERENTE").contains(u.perfil()))throw new Erro("ACESSO_NEGADO");}
 private void autorizarConsulta(Usuario u,Registro r){
  if(u.perfil().equals("COMERCIAL"))proprio(u,r);
  else if(!u.perfil().equals(r.perfilAprovador()))throw new Erro("ACESSO_NEGADO");
 }
 private void proprio(Usuario u,Registro r){if(!u.id().equals(r.solicitanteId()))throw new Erro("ACESSO_NEGADO");}
 private void rascunho(Registro r){if(!r.status().equals("RASCUNHO"))throw new Erro("ESTADO_INVALIDO");}
 private Registro carregar(UUID id,boolean bloquear){if(id==null)throw new Erro("DADOS_INVALIDOS");var r=repository.carregar(id,bloquear);if(r==null)throw new Erro("COTACAO_INEXISTENTE");return r;}
 private boolean positivo(BigDecimal v){return v!=null&&v.signum()>0;}
 private Entrada validar(Entrada e) {
  if(e==null||e.clienteId()==null||e.faturamento()==null||e.vencimento()==null||e.vencimento().isBefore(e.faturamento())||e.jurosAnual()==null||e.jurosAnual().signum()<0||e.areaCliente()!=null&&!positivo(e.areaCliente())||e.itens()==null||e.itens().isEmpty())throw new Erro("DADOS_INVALIDOS");
  if(!repository.clienteExiste(e.clienteId()))throw new Erro("CLIENTE_INEXISTENTE");
  var itens=new ArrayList<ItemEntrada>();
  for(var item:e.itens()) {
   if(item==null||item.produtoId()==null||!positivo(item.quantidade())||!positivo(item.preco()))throw new Erro("DADOS_INVALIDOS");
   if(!repository.produtoAtivo(item.produtoId()))throw new Erro("PRODUTO_INDISPONIVEL");
   itens.add(new ItemEntrada(item.produtoId(),item.quantidade(),Financeiro.preco(item.preco())));
  }
  return new Entrada(e.clienteId(),e.faturamento(),e.vencimento(),e.jurosAnual(),e.areaCliente(),List.copyOf(itens));
 }
 private BigDecimal margem(BigDecimal custo,BigDecimal preco,Entrada e){return Financeiro.margem(custo,preco,e.jurosAnual(),e.faturamento(),e.vencimento());}
 private BigDecimal total(List<Item> itens){return itens.stream().map(i->i.preco().multiply(i.quantidade())).reduce(BigDecimal.ZERO,BigDecimal::add);}
 private BigDecimal ponderada(List<BigDecimal> margens,List<Item> itens) {
  BigDecimal soma=BigDecimal.ZERO,quantidade=BigDecimal.ZERO;
  for(int i=0;i<itens.size();i++){if(margens.get(i)==null)return null;var q=itens.get(i).quantidade();soma=soma.add(margens.get(i).multiply(q));quantidade=quantidade.add(q);}
  if(quantidade.signum()<=0)throw new Erro("DADOS_INVALIDOS");
  return soma.divide(quantidade,MathContext.DECIMAL128);
 }
}
