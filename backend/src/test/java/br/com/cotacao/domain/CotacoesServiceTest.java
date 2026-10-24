package br.com.cotacao.domain;
import br.com.cotacao.application.*;
import br.com.cotacao.infrastructure.CotacaoRepository;
import static br.com.cotacao.application.Cotacoes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
class CotacoesServiceTest {
 private JdbcTemplate db; private CotacoesService service; private Identidade identidade;
 private DriverManagerDataSource admin; private String schema;
 private final UUID comercial=UUID.randomUUID(),outro=UUID.randomUUID(),cliente=UUID.randomUUID(),p1=UUID.randomUUID(),p2=UUID.randomUUID();
 private static BigDecimal n(String s){return new BigDecimal(s);}
 private void sql(String s,Object... args){db.update(s,args);}
 private String valor(String s){return db.queryForObject(s,String.class);}
 @BeforeEach void preparar() throws Exception {
  admin=new DriverManagerDataSource("jdbc:postgresql://127.0.0.1:55432/postgres","cotacao_test","");
  schema="fluxo_"+UUID.randomUUID().toString().replace("-","");
  new JdbcTemplate(admin).execute("CREATE SCHEMA "+schema);
  var ds=new DriverManagerDataSource("jdbc:postgresql://127.0.0.1:55432/postgres?currentSchema="+schema,"cotacao_test","");
  db=new JdbcTemplate(ds); db.execute(Files.readString(Path.of("src/main/resources/db/migration/V1__estrutura_inicial.sql")));
  var tx=new TransactionTemplate(new DataSourceTransactionManager(ds)); tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
  identidade=mock(Identidade.class); when(identidade.contaAutenticada()).thenReturn("DOM\\comercial");
  service=new CotacoesService(new CotacaoRepository(db),tx,identidade,Clock.fixed(Instant.parse("2026-10-24T15:00:00Z"),ZoneOffset.UTC));
  usuario(comercial,"comercial","COMERCIAL"); usuario(outro,"outro","COMERCIAL");
  usuario(UUID.randomUUID(),"gestor","GESTOR"); usuario(UUID.randomUUID(),"supervisor","SUPERVISOR"); usuario(UUID.randomUUID(),"gerente","GERENTE");
  sql("INSERT INTO clientes(id,nome) VALUES(?,?)",cliente,"Cliente");
  for(var p:List.of(p1,p2))sql("INSERT INTO produtos(id,nome,unidade_medida) VALUES(?,?,?)",p,"Produto","L");
 }
 private void usuario(UUID id,String conta,String perfil){sql("INSERT INTO usuarios(id,email,conta_windows,nome,perfil) VALUES(?,?,?,?,?)",id,conta+"@empresa", "DOM\\"+conta,conta,perfil);}
 @AfterEach void limpar(){if(admin!=null&&schema!=null)new JdbcTemplate(admin).execute("DROP SCHEMA "+schema+" CASCADE");}
 private Entrada entrada(){return new Entrada(cliente,LocalDate.of(2026,1,1),LocalDate.of(2026,8,1),n("0.12"),null,List.of(new ItemEntrada(p1,n("2"),n("100.001")),new ItemEntrada(p2,n("1"),n("200"))));}
 private void custos(UUID p,String custo,String real){sql("INSERT INTO produto_custos(id,produto_id,custo) VALUES(?,?,?)",UUID.randomUUID(),p,n(custo));sql("INSERT INTO produto_custos_reais(id,produto_id,custo_real) VALUES(?,?,?)",UUID.randomUUID(),p,n(real));}
 private void custos(){custos(p1,"10","10");custos(p2,"30","10");}
 private void erro(String codigo,Runnable acao){assertEquals(codigo,assertThrows(Erro.class,acao::run).codigo());}
 @Test void criaRascunhoSemCustosEComPrecoArredondado(){var id=service.criar(entrada());assertNotNull(id);assertEquals("RASCUNHO",valor("SELECT status FROM cotacoes"));assertEquals("100.01",db.queryForObject("SELECT preco::text FROM cotacao_itens WHERE produto_id=?",String.class,p1));assertEquals("2026-10-24 11:00:00-04",valor("SELECT criado_em::text FROM cotacoes"));assertEquals(comercial.toString(),valor("SELECT solicitante_id::text FROM cotacoes"));}
 @Test void semCustosPreviewNaoInventaMargem(){var dto=(Comercial)service.consultar(service.criar(entrada()));assertNull(dto.margemPonderada());assertEquals(0,n("400.02").compareTo(dto.precoTotal()));}
 @Test void consultaRecalculaRascunhoMasNaoVazaCamposReais() throws Exception {
  custos();var id=service.criar(entrada());var antes=(Comercial)service.consultar(id);
  sql("INSERT INTO produto_custos(id,produto_id,custo,data_criacao) VALUES(?,?,?,?)",UUID.randomUUID(),p1,n("20"),java.sql.Timestamp.from(Instant.parse("2030-01-01T00:00:00Z")));
  var depois=(Comercial)service.consultar(id);assertTrue(depois.margemPonderada().compareTo(antes.margemPonderada())>0);
  var json=new ObjectMapper().findAndRegisterModules().writeValueAsString(depois);assertFalse(json.contains("custoReal"));assertFalse(json.contains("margemReal"));assertFalse(json.contains("custo_real"));
 }
 @Test void envioUsaMaiorAlcadaESnapshotsSemAprovacaoAutomatica(){custos();var id=service.criar(entrada());service.enviar(id);assertEquals("GERENTE",valor("SELECT perfil_aprovador FROM cotacoes"));assertEquals("PENDENTE_APROVACAO",valor("SELECT status FROM cotacoes"));assertEquals("2",valor("SELECT count(*) FROM cotacao_itens WHERE status='PENDENTE'"));assertEquals("0",valor("SELECT count(*) FROM historico_aprovacoes"));var antes=(Comercial)service.consultar(id);custos(p1,"999","999");assertEquals(antes,service.consultar(id));assertEquals("1",valor("SELECT versao FROM cotacoes"));}
 @Test void calculaMediaPorQuantidadeEPrecoTotal(){custos();var id=service.criar(entrada());service.enviar(id);var dto=(Comercial)service.consultar(id);var esperado=Financeiro.margem(n("10"),n("100.01"),n("0.12"),entrada().faturamento(),entrada().vencimento()).multiply(n("2")).add(n("0.162")).divide(n("3"),java.math.MathContext.DECIMAL128);assertEquals(0,esperado.compareTo(dto.margemPonderada()));assertEquals(0,n("400.02").compareTo(dto.precoTotal()));}
 @Test void aprovadorRecebeSomenteDtoDaSuaFila() throws Exception {custos();var id=service.criar(entrada());service.enviar(id);when(identidade.contaAutenticada()).thenReturn("DOM\\gestor");erro("ACESSO_NEGADO",()->service.consultar(id));when(identidade.contaAutenticada()).thenReturn("DOM\\gerente");var dto=assertInstanceOf(Aprovador.class,service.consultar(id));assertEquals(2,dto.itens().size());assertNotNull(dto.margemRealPonderada());var json=new ObjectMapper().findAndRegisterModules().writeValueAsString(dto);assertFalse(json.contains("margemPonderada"));}
 @Test void custoAusenteFazRollbackIntegral(){custos(p1,"10","10");var id=service.criar(entrada());erro("CUSTO_AUSENTE",()->service.enviar(id));assertEquals("RASCUNHO",valor("SELECT status FROM cotacoes"));assertEquals("0",valor("SELECT count(*) FROM cotacao_itens WHERE custo IS NOT NULL OR status<>'RASCUNHO'"));assertEquals("0",valor("SELECT versao FROM cotacoes"));}
 @Test void reenvioEEdicaoDeEnviadaSaoBloqueados(){custos();var id=service.criar(entrada());service.enviar(id);erro("ESTADO_INVALIDO",()->service.enviar(id));erro("ESTADO_INVALIDO",()->service.editar(id,entrada()));assertEquals("1",valor("SELECT versao FROM cotacoes"));}
 @Test void outroComercialNaoConsultaEditaOuEnvia(){var id=service.criar(entrada());when(identidade.contaAutenticada()).thenReturn("DOM\\outro");erro("ACESSO_NEGADO",()->service.consultar(id));erro("ACESSO_NEGADO",()->service.enviar(id));erro("ACESSO_NEGADO",()->service.editar(id,entrada()));}
 @Test void aprovadorNaoCriaEUsuarioInativoNaoAcessa(){when(identidade.contaAutenticada()).thenReturn("DOM\\gerente");erro("ACESSO_NEGADO",()->service.criar(entrada()));when(identidade.contaAutenticada()).thenReturn("DOM\\comercial");sql("UPDATE usuarios SET ativo=false WHERE id=?",comercial);erro("ACESSO_NEGADO",()->service.criar(entrada()));}
 @ParameterizedTest @NullAndEmptySource void identidadeAusente(String conta){when(identidade.contaAutenticada()).thenReturn(conta);erro("NAO_AUTENTICADO",()->service.criar(entrada()));}
 @Test void usuarioNaoCadastradoNegado(){when(identidade.contaAutenticada()).thenReturn("DOM\\desconhecido");erro("ACESSO_NEGADO",()->service.criar(entrada()));}
 @Test void validaEntradasSemPersistir(){erro("DADOS_INVALIDOS",()->service.criar(null));var e=entrada();for(var itens:List.<List<ItemEntrada>>of(List.of(),List.of(new ItemEntrada(p1,n("0"),n("1"))),List.of(new ItemEntrada(p1,n("1"),n("0")))))erro("DADOS_INVALIDOS",()->service.criar(new Entrada(cliente,e.faturamento(),e.vencimento(),e.jurosAnual(),null,itens)));erro("DADOS_INVALIDOS",()->service.criar(new Entrada(cliente,e.vencimento(),e.faturamento(),n("0.12"),null,e.itens())));erro("DADOS_INVALIDOS",()->service.criar(new Entrada(cliente,e.faturamento(),e.vencimento(),n("-1"),null,e.itens())));erro("DADOS_INVALIDOS",()->service.criar(new Entrada(cliente,e.faturamento(),e.vencimento(),n("0"),n("0"),e.itens())));assertEquals("0",valor("SELECT count(*) FROM cotacoes"));}
 @Test void referenciasInvalidasNaoPersistem(){var e=entrada();erro("CLIENTE_INEXISTENTE",()->service.criar(new Entrada(UUID.randomUUID(),e.faturamento(),e.vencimento(),e.jurosAnual(),null,e.itens())));sql("UPDATE produtos SET ativo=false WHERE id=?",p1);erro("PRODUTO_INDISPONIVEL",()->service.criar(e));assertEquals("0",valor("SELECT count(*) FROM cotacoes"));}
 @Test void editaSomenteRascunhoProprio(){var id=service.criar(entrada());var e=entrada();service.editar(id,new Entrada(cliente,e.faturamento(),e.vencimento(),e.jurosAnual(),n("50"),List.of(new ItemEntrada(p1,n("3"),n("99.999")))));assertEquals("1",valor("SELECT count(*) FROM cotacao_itens"));assertEquals("50",valor("SELECT area_cliente FROM cotacoes"));assertEquals("100.00",valor("SELECT preco FROM cotacao_itens"));assertEquals("1",valor("SELECT versao FROM cotacoes"));}
 @Test void inexistenteENulo(){erro("COTACAO_INEXISTENTE",()->service.consultar(UUID.randomUUID()));erro("DADOS_INVALIDOS",()->service.enviar(null));}
 @Test void duasSubmissoesNaoDuplicamSnapshot() throws Exception {custos();var id=service.criar(entrada());var pool=Executors.newFixedThreadPool(2);try {var inicio=new CountDownLatch(1);Callable<Boolean> tentativa=()->{inicio.await();try{service.enviar(id);return true;}catch(RuntimeException ex){if(ex instanceof Erro err)assertEquals("ESTADO_INVALIDO",err.codigo());else assertInstanceOf(org.springframework.dao.ConcurrencyFailureException.class,ex);return false;}};var a=pool.submit(tentativa);var b=pool.submit(tentativa);inicio.countDown();assertNotEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));assertEquals("1",valor("SELECT versao FROM cotacoes"));assertEquals("2",valor("SELECT count(*) FROM cotacao_itens WHERE status='PENDENTE'"));}finally{pool.shutdownNow();}}
}
