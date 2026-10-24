package br.com.cotacao.domain;

import br.com.cotacao.support.CenarioBanco;
import static br.com.cotacao.application.Cotacoes.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AprovacaoServiceTest {
 private CenarioBanco c;
 private UUID id,segundoProduto;
 private List<UUID> itens;
 @BeforeEach void preparar() throws Exception {
  c=new CenarioBanco();segundoProduto=UUID.randomUUID();
  c.db.update("INSERT INTO produtos(id,nome,unidade_medida) VALUES(?,'Segundo','L')",segundoProduto);
  for(var p:List.of(c.produto,segundoProduto))c.db.update("INSERT INTO produto_custos(id,produto_id,custo) VALUES(?,?,10)",UUID.randomUUID(),p);
  c.db.update("INSERT INTO produto_custos_reais(id,produto_id,custo_real) VALUES(?,?,20),(?,?,1)",UUID.randomUUID(),c.produto,UUID.randomUUID(),segundoProduto);
  for(var perfil:List.of("GESTOR","SUPERVISOR"))c.db.update("INSERT INTO usuarios(id,email,conta_windows,nome,perfil) VALUES(?,?,?,?,?)",UUID.randomUUID(),perfil+"@empresa","DOM\\"+perfil.toLowerCase(),perfil,perfil);
  var entrada=new Entrada(c.cliente,LocalDate.of(2026,1,1),LocalDate.of(2026,8,1),new BigDecimal("0.12"),null,
   List.of(new ItemEntrada(c.produto,BigDecimal.ONE,new BigDecimal("100")),new ItemEntrada(segundoProduto,new BigDecimal("2"),new BigDecimal("100"))));
  id=c.service.criar(entrada);c.service.enviar(id);
  itens=c.db.query("SELECT id FROM cotacao_itens WHERE cotacao_id=? ORDER BY id",(r,i)->r.getObject(1,UUID.class),id);
  c.como("gerente");
 }
 @AfterEach void limpar(){if(c!=null)c.close();}
 private Decisoes todas(boolean aprovado){return new Decisoes(itens.stream().map(i->new DecisaoItem(i,aprovado,null)).toList());}
 private void erro(String codigo,Runnable acao){assertEquals(codigo,assertThrows(Erro.class,acao::run).codigo());}
 private String snapshots(){return c.valor("SELECT jsonb_agg(jsonb_build_object('id',id,'produto',produto_id,'quantidade',quantidade,'preco',preco,'custo',custo,'custo_real',custo_real,'margem',margem,'margem_real',margem_real) ORDER BY id)::text FROM cotacao_itens WHERE cotacao_id=?",id);}
 @ParameterizedTest @CsvSource({"true,true,APROVADA","false,false,REPROVADA","true,false,APROVADA_PARCIAL"})
 void confirmaTodosEConsolidaSemRecalcularSnapshots(boolean primeiro,boolean segundo,String esperado){
  var antes=snapshots();
  c.service.decidir(id,new Decisoes(List.of(new DecisaoItem(itens.get(0),primeiro,null),new DecisaoItem(itens.get(1),segundo,"Comentário opcional"))));
  assertEquals(esperado,c.valor("SELECT status FROM cotacoes WHERE id=?",id));assertEquals("2",c.valor("SELECT versao FROM cotacoes WHERE id=?",id));
  assertEquals("0",c.valor("SELECT count(*) FROM cotacao_itens WHERE cotacao_id=? AND status='PENDENTE'",id));assertEquals(antes,snapshots());
  assertEquals(primeiro?"APROVADO":"REPROVADO",c.valor("SELECT status FROM cotacao_itens WHERE id=?",itens.get(0)));
  assertEquals(segundo?"APROVADO":"REPROVADO",c.valor("SELECT status FROM cotacao_itens WHERE id=?",itens.get(1)));
  assertEquals("2",c.valor("SELECT count(*) FROM historico_aprovacoes"));assertEquals("2",c.valor("SELECT count(*) FROM historico_aprovacoes WHERE usuario_id=? AND criado_em=?",c.gerente,java.sql.Timestamp.from(Instant.parse("2026-10-24T15:00:00Z"))));
  assertEquals("Comentário opcional",c.valor("SELECT comentario FROM historico_aprovacoes WHERE item_id=?",itens.get(1)));
 }
 @Test void abrirDetalheHistoricoEFilaNaoDecide(){assertInstanceOf(Aprovador.class,c.service.consultar(id));assertTrue(c.service.historico(id).isEmpty());assertEquals(1,c.service.pendentes(0,20).total());assertEquals("2",c.valor("SELECT count(*) FROM cotacao_itens WHERE status='PENDENTE'"));assertEquals("0",c.valor("SELECT count(*) FROM historico_aprovacoes"));}
 @Test void gerenteDecideInclusiveItemDaFaixaGestor(){assertEquals("GERENTE",c.valor("SELECT perfil_aprovador FROM cotacoes WHERE id=?",id));assertEquals("1",c.valor("SELECT count(*) FROM cotacao_itens WHERE margem_real>=0.10"));c.service.decidir(id,todas(true));assertEquals("APROVADA",c.valor("SELECT status FROM cotacoes WHERE id=?",id));}
 @Test void reprovarSemComentarioEhValido(){c.service.decidir(id,todas(false));assertEquals("2",c.valor("SELECT count(*) FROM cotacao_itens WHERE status='REPROVADO' AND comentario IS NULL"));assertEquals("2",c.valor("SELECT count(*) FROM historico_aprovacoes WHERE decisao='REPROVADO' AND comentario IS NULL"));}
 @Test void comercialEAlcadasDiferentesNaoDecidem(){for(var conta:List.of("comercial","outro","gestor","supervisor")){c.como(conta);erro("ACESSO_NEGADO",()->c.service.decidir(id,todas(true)));}assertEquals("0",c.valor("SELECT count(*) FROM historico_aprovacoes"));}
 @Test void gerenteNaoAssumeCotacaoAtribuidaAoGestor(){c.db.update("UPDATE cotacoes SET perfil_aprovador='GESTOR' WHERE id=?",id);erro("ACESSO_NEGADO",()->c.service.decidir(id,todas(true)));c.como("gestor");c.service.decidir(id,todas(true));assertEquals("APROVADA",c.valor("SELECT status FROM cotacoes WHERE id=?",id));}
 @Test void supervisorDecideSuaFila(){c.db.update("UPDATE cotacoes SET perfil_aprovador='SUPERVISOR' WHERE id=?",id);c.como("supervisor");c.service.decidir(id,todas(true));assertEquals("APROVADA",c.valor("SELECT status FROM cotacoes WHERE id=?",id));}
 @Test void corpoInvalidoBooleanoAusenteENulosNaoDecidem(){
  erro("DADOS_INVALIDOS",()->c.service.decidir(id,null));erro("DADOS_INVALIDOS",()->c.service.decidir(id,new Decisoes(null)));
  erro("DECISOES_INVALIDAS",()->c.service.decidir(id,new Decisoes(List.of())));
  erro("DADOS_INVALIDOS",()->c.service.decidir(id,new Decisoes(Arrays.asList((DecisaoItem)null))));
  erro("DADOS_INVALIDOS",()->c.service.decidir(id,new Decisoes(List.of(new DecisaoItem(null,true,null)))));
  erro("DADOS_INVALIDOS",()->c.service.decidir(id,new Decisoes(List.of(new DecisaoItem(itens.getFirst(),null,null)))));
  assertEquals("0",c.valor("SELECT count(*) FROM historico_aprovacoes"));
 }
 @Test void conjuntoIncompletoRepetidoOuExternoRejeitado(){
  erro("DECISOES_INVALIDAS",()->c.service.decidir(id,new Decisoes(List.of(new DecisaoItem(itens.getFirst(),true,null)))));
  erro("DECISOES_INVALIDAS",()->c.service.decidir(id,new Decisoes(List.of(new DecisaoItem(itens.getFirst(),true,null),new DecisaoItem(itens.getFirst(),false,null)))));
  var outra=UUID.randomUUID();c.cotacao(outra,c.comercial,"PENDENTE_APROVACAO","GERENTE",Instant.parse("2026-10-24T15:00:00Z"));
  var externo=UUID.fromString(c.valor("SELECT id FROM cotacao_itens WHERE cotacao_id=?",outra));
  erro("DECISOES_INVALIDAS",()->c.service.decidir(id,new Decisoes(List.of(new DecisaoItem(itens.getFirst(),true,null),new DecisaoItem(externo,false,null)))));
  assertEquals("2",c.valor("SELECT count(*) FROM cotacao_itens WHERE cotacao_id=? AND status='PENDENTE'",id));
 }
 @Test void itemJaDecididoNaoPodeSerReescrito(){c.db.update("UPDATE cotacao_itens SET status='APROVADO' WHERE id=?",itens.getFirst());erro("ESTADO_INVALIDO",()->c.service.decidir(id,todas(false)));assertEquals("APROVADO",c.valor("SELECT status FROM cotacao_itens WHERE id=?",itens.getFirst()));assertEquals("0",c.valor("SELECT count(*) FROM historico_aprovacoes"));}
 @Test void decideSomentePendentesPreservandoDecisaoAnterior(){
  var primeiro=itens.getFirst();c.db.update("UPDATE cotacao_itens SET status='APROVADO',comentario='Anterior' WHERE id=?",primeiro);
  c.db.update("INSERT INTO historico_aprovacoes(id,item_id,usuario_id,decisao,comentario) VALUES(?,?,?,'APROVADO','Anterior')",UUID.randomUUID(),primeiro,c.gerente);
  c.service.decidir(id,new Decisoes(List.of(new DecisaoItem(itens.getLast(),false,null))));
  assertEquals("APROVADA_PARCIAL",c.valor("SELECT status FROM cotacoes WHERE id=?",id));assertEquals("Anterior",c.valor("SELECT comentario FROM cotacao_itens WHERE id=?",primeiro));assertEquals("2",c.valor("SELECT count(*) FROM historico_aprovacoes"));
 }
 @Test void estadoIncompativelOuNenhumPendenteRejeitado(){
  c.db.update("UPDATE cotacao_itens SET status='APROVADO'");erro("ESTADO_INVALIDO",()->c.service.decidir(id,todas(true)));
  c.db.update("UPDATE cotacoes SET status='APROVADA' WHERE id=?",id);erro("ESTADO_INVALIDO",()->c.service.decidir(id,todas(true)));
  c.db.update("UPDATE cotacoes SET status='RASCUNHO',perfil_aprovador=NULL WHERE id=?",id);erro("ESTADO_INVALIDO",()->c.service.decidir(id,todas(true)));
 }
 @Test void falhaNaSegundaAuditoriaReverteItensHistoricoECabecalho(){
  c.db.execute("CREATE FUNCTION falhar_auditoria() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF EXISTS(SELECT 1 FROM historico_aprovacoes) THEN RAISE EXCEPTION 'falha de teste' USING ERRCODE='23514'; END IF; RETURN NEW; END; $$");
  c.db.execute("CREATE TRIGGER falha_teste BEFORE INSERT ON historico_aprovacoes FOR EACH ROW EXECUTE FUNCTION falhar_auditoria()");
  assertThrows(DataIntegrityViolationException.class,()->c.service.decidir(id,todas(true)));
  assertEquals("0",c.valor("SELECT count(*) FROM historico_aprovacoes"));assertEquals("2",c.valor("SELECT count(*) FROM cotacao_itens WHERE status='PENDENTE'"));assertEquals("PENDENTE_APROVACAO",c.valor("SELECT status FROM cotacoes WHERE id=?",id));assertEquals("1",c.valor("SELECT versao FROM cotacoes WHERE id=?",id));
 }
 @Test void historicoMostraAutorHorarioDecisaoEComentarioSemPermissaoCruzada(){
  c.service.decidir(id,new Decisoes(List.of(new DecisaoItem(itens.getFirst(),true,null),new DecisaoItem(itens.getLast(),false,"Revisar preço"))));
  var eventos=c.service.historico(id);assertEquals(2,eventos.size());assertTrue(eventos.stream().allMatch(e->e.usuarioId().equals(c.gerente)&&e.autorNome().equals("gerente")&&e.criadoEm().equals(Instant.parse("2026-10-24T15:00:00Z"))));
  assertEquals("Revisar preço",eventos.stream().filter(e->e.itemId().equals(itens.getLast())).findFirst().orElseThrow().comentario());
  c.como("comercial");assertEquals(eventos,c.service.historico(id));c.como("outro");erro("ACESSO_NEGADO",()->c.service.historico(id));c.como("gestor");erro("ACESSO_NEGADO",()->c.service.historico(id));
 }
 @Test void auditoriaPersistidaEhImutavel(){c.service.decidir(id,todas(true));assertThrows(DataAccessException.class,()->c.db.update("UPDATE historico_aprovacoes SET comentario='alterado'"));assertThrows(DataAccessException.class,()->c.db.update("DELETE FROM historico_aprovacoes"));assertThrows(DataAccessException.class,()->c.db.execute("TRUNCATE historico_aprovacoes"));assertEquals("2",c.valor("SELECT count(*) FROM historico_aprovacoes"));}
 @Test void filaSomentePendenteDaAlcadaComPaginacao(){
  for(int i=0;i<21;i++)c.cotacao(UUID.randomUUID(),c.comercial,"PENDENTE_APROVACAO","GERENTE",Instant.parse("2026-10-24T15:00:00Z"));
  c.cotacao(UUID.randomUUID(),c.comercial,"APROVADA","GERENTE",Instant.parse("2026-10-24T15:00:00Z"));c.cotacao(UUID.randomUUID(),c.comercial,"PENDENTE_APROVACAO","GESTOR",Instant.parse("2026-10-24T15:00:00Z"));
  var pagina=c.service.pendentes(0,20);assertEquals(22,pagina.total());assertEquals(20,pagina.conteudo().size());assertEquals(2,c.service.pendentes(1,20).conteudo().size());assertTrue(pagina.conteudo().stream().allMatch(r->r.status().equals("PENDENTE_APROVACAO")&&r.perfilAprovador().equals("GERENTE")&&!r.podeEditar()&&!r.podeDuplicar()));
  erro("DADOS_INVALIDOS",()->c.service.pendentes(-1,20));erro("DADOS_INVALIDOS",()->c.service.pendentes(0,10));c.como("comercial");erro("ACESSO_NEGADO",()->c.service.pendentes(0,20));
 }
 @Test void autenticacaoAtivaReferenciasENulos(){
  erro("DADOS_INVALIDOS",()->c.service.decidir(null,todas(true)));erro("COTACAO_INEXISTENTE",()->c.service.decidir(UUID.randomUUID(),todas(true)));erro("DADOS_INVALIDOS",()->c.service.historico(null));erro("COTACAO_INEXISTENTE",()->c.service.historico(UUID.randomUUID()));
  when(c.identidade.contaAutenticada()).thenReturn(null);erro("NAO_AUTENTICADO",()->c.service.decidir(id,todas(true)));erro("NAO_AUTENTICADO",()->c.service.historico(id));erro("NAO_AUTENTICADO",()->c.service.pendentes(0,20));
  c.como("gerente");c.db.update("UPDATE usuarios SET ativo=false WHERE id=?",c.gerente);erro("ACESSO_NEGADO",()->c.service.decidir(id,todas(true)));
 }
 @Test void confirmacaoRepetidaNaoDuplicaHistorico(){c.service.decidir(id,todas(true));erro("ESTADO_INVALIDO",()->c.service.decidir(id,todas(false)));assertEquals("2",c.valor("SELECT count(*) FROM historico_aprovacoes"));assertEquals("2",c.valor("SELECT versao FROM cotacoes WHERE id=?",id));}
 @Test void confirmacoesSimultaneasNaoPersistemDecisoesConflitantes() throws Exception {
  var pool=Executors.newFixedThreadPool(2);var inicio=new CountDownLatch(1);
  try {
   Callable<Boolean> aprovar=()->tentar(inicio,true);Callable<Boolean> reprovar=()->tentar(inicio,false);
   var a=pool.submit(aprovar);var b=pool.submit(reprovar);inicio.countDown();assertNotEquals(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));
   var status=c.valor("SELECT status FROM cotacoes WHERE id=?",id);assertTrue(Set.of("APROVADA","REPROVADA").contains(status));
   assertEquals("1",c.valor("SELECT count(DISTINCT decisao) FROM historico_aprovacoes"));assertEquals("2",c.valor("SELECT count(*) FROM historico_aprovacoes"));assertEquals("2",c.valor("SELECT versao FROM cotacoes WHERE id=?",id));
  } finally {pool.shutdownNow();}
 }
 private boolean tentar(CountDownLatch inicio,boolean aprovado) throws Exception {
  inicio.await();try{c.service.decidir(id,todas(aprovado));return true;}catch(Erro e){assertEquals("ESTADO_INVALIDO",e.codigo());return false;}catch(ConcurrencyFailureException e){return false;}
 }
}
