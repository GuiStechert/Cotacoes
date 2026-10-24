package br.com.cotacao.domain;
import br.com.cotacao.support.CenarioBanco;
import static br.com.cotacao.application.Cotacoes.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

class ListagemDuplicacaoTest {
 private CenarioBanco c;
 private final Instant agora=Instant.parse("2026-10-24T15:00:00Z");
 @BeforeEach void preparar() throws Exception { c=new CenarioBanco(); }
 @AfterEach void limpar() { if(c!=null)c.close(); }
 private Filtro filtro(int pagina) { return new Filtro(pagina,20,null,null,null,null); }
 private void erro(String codigo,Runnable acao) { assertEquals(codigo,assertThrows(Erro.class,acao::run).codigo()); }
 @Test void paginaNoBancoComTotalOrdenacaoEstavelEAcoes() {
  var ids=new ArrayList<UUID>();
  for(int i=1;i<=25;i++){var id=UUID.fromString("00000000-0000-0000-0000-"+String.format("%012d",i));ids.add(id);c.cotacao(id,c.comercial,"RASCUNHO",null,agora);}
  c.cotacao(UUID.randomUUID(),c.outro,"RASCUNHO",null,agora.plusSeconds(1));
  var primeira=c.service.listar(filtro(0));var segunda=c.service.listar(filtro(1));
  assertEquals(25,primeira.total());assertEquals(2,primeira.totalPaginas());assertEquals(20,primeira.tamanho());assertEquals(20,primeira.conteudo().size());assertEquals(5,segunda.conteudo().size());assertEquals(1,segunda.pagina());
  Collections.reverse(ids);assertEquals(ids.subList(0,20),primeira.conteudo().stream().map(Resumo::id).toList());assertEquals(ids.subList(20,25),segunda.conteudo().stream().map(Resumo::id).toList());
  assertEquals(new BigDecimal("20.02"),primeira.conteudo().getFirst().precoTotal());assertEquals("Cliente",primeira.conteudo().getFirst().clienteNome());assertEquals("comercial",primeira.conteudo().getFirst().solicitanteNome());
  assertTrue(primeira.conteudo().stream().allMatch(r->r.podeEditar()&&!r.podeDuplicar()));assertTrue(c.service.listar(filtro(3)).conteudo().isEmpty());
 }
 @Test void filtrosPorEstadoClienteEPeriodoInclusivoNoFusoLocal() {
  var inicioDia=Instant.parse("2026-10-24T04:00:00Z");var fimDia=Instant.parse("2026-10-25T03:59:59Z");
  var a=UUID.randomUUID();var b=UUID.randomUUID();
  c.cotacao(a,c.comercial,"PENDENTE_APROVACAO","GERENTE",inicioDia);c.cotacao(b,c.comercial,"PENDENTE_APROVACAO","GERENTE",fimDia);
  c.cotacao(UUID.randomUUID(),c.comercial,"PENDENTE_APROVACAO","GERENTE",inicioDia.minusSeconds(1));c.cotacao(UUID.randomUUID(),c.comercial,"RASCUNHO",null,agora);
  var f=new Filtro(0,20,"PENDENTE_APROVACAO",c.cliente,LocalDate.of(2026,10,24),LocalDate.of(2026,10,24));
  var pagina=c.service.listar(f);assertEquals(List.of(b,a),pagina.conteudo().stream().map(Resumo::id).toList());assertEquals(2,pagina.total());assertTrue(pagina.conteudo().stream().allMatch(r->!r.podeEditar()&&r.podeDuplicar()));
  assertEquals(0,c.service.listar(new Filtro(0,20,null,UUID.randomUUID(),null,null)).total());
 }
 @Test void aprovadorListaSomenteFilaSemAcoesComerciais() {
  var id=UUID.randomUUID();c.cotacao(id,c.comercial,"PENDENTE_APROVACAO","GERENTE",agora);c.cotacao(UUID.randomUUID(),c.comercial,"PENDENTE_APROVACAO","GESTOR",agora);c.cotacao(UUID.randomUUID(),c.comercial,"RASCUNHO",null,agora);
  c.como("gerente");var p=c.service.listar(filtro(0));assertEquals(1,p.total());assertEquals(id,p.conteudo().getFirst().id());assertFalse(p.conteudo().getFirst().podeEditar());assertFalse(p.conteudo().getFirst().podeDuplicar());
 }
 @Test void filtrosInvalidos() {
  for(var f:List.of(new Filtro(-1,20,null,null,null,null),new Filtro(0,21,null,null,null,null),new Filtro(0,0,null,null,null,null),new Filtro(0,20,"ADMIN",null,null,null),new Filtro(0,20,null,null,LocalDate.of(2026,2,1),LocalDate.of(2026,1,1))))erro("DADOS_INVALIDOS",()->c.service.listar(f));
  erro("DADOS_INVALIDOS",()->c.service.listar(null));assertEquals(0,c.service.listar(filtro(Integer.MAX_VALUE)).total());
 }
 @Test void duplicaComNovosIdsSemSnapshotsComentariosOuAuditoria() {
  var original=UUID.randomUUID();c.cotacao(original,c.comercial,"APROVADA_PARCIAL","GERENTE",agora);
  c.db.update("UPDATE cotacao_itens SET custo=10,custo_real=8,margem=1.08,margem_real=0.864,status='REPROVADO',comentario='Revisar' WHERE cotacao_id=?",original);
  var item=c.valor("SELECT id::text FROM cotacao_itens WHERE cotacao_id=?",original);
  c.db.update("INSERT INTO historico_aprovacoes(id,item_id,usuario_id,decisao,comentario) VALUES(?,?,?,'REPROVADO','Revisar')",UUID.randomUUID(),UUID.fromString(item),c.gerente);
  var antes=c.valor("SELECT row_to_json(cotacoes)::text FROM cotacoes WHERE id=?",original);
  var copia=c.service.duplicar(original);assertNotEquals(original,copia);
  assertEquals("RASCUNHO",c.valor("SELECT status FROM cotacoes WHERE id=?",copia));assertNull(c.valor("SELECT perfil_aprovador FROM cotacoes WHERE id=?",copia));assertEquals("50",c.valor("SELECT area_cliente FROM cotacoes WHERE id=?",copia));
  assertNotEquals(item,c.valor("SELECT id::text FROM cotacao_itens WHERE cotacao_id=?",copia));assertEquals("0",c.valor("SELECT count(*) FROM cotacao_itens WHERE cotacao_id=? AND (status<>'RASCUNHO' OR custo IS NOT NULL OR custo_real IS NOT NULL OR margem IS NOT NULL OR margem_real IS NOT NULL OR comentario IS NOT NULL)",copia));
  assertEquals("1",c.valor("SELECT count(*) FROM historico_aprovacoes"));assertEquals(antes,c.valor("SELECT row_to_json(cotacoes)::text FROM cotacoes WHERE id=?",original));assertEquals("Revisar",c.valor("SELECT comentario FROM cotacao_itens WHERE cotacao_id=?",original));
  c.db.update("INSERT INTO produto_custos(id,produto_id,custo) VALUES(?,?,99)",UUID.randomUUID(),c.produto);
  assertTrue(((Comercial)c.service.consultar(copia)).margemPonderada().compareTo(BigDecimal.ONE)>0);
 }
 @Test void bloqueiaDuplicacaoRascunhoOutroDonoEAprovador() {
  var rascunho=UUID.randomUUID();var enviada=UUID.randomUUID();c.cotacao(rascunho,c.comercial,"RASCUNHO",null,agora);c.cotacao(enviada,c.comercial,"APROVADA","GERENTE",agora);
  erro("ESTADO_INVALIDO",()->c.service.duplicar(rascunho));c.como("outro");erro("ACESSO_NEGADO",()->c.service.duplicar(enviada));c.como("gerente");erro("ACESSO_NEGADO",()->c.service.duplicar(enviada));assertEquals("2",c.valor("SELECT count(*) FROM cotacoes"));
 }
 @Test void duplicacaoInvalidaNaoCriaCabecalho() {
  var id=UUID.randomUUID();c.cotacao(id,c.comercial,"REPROVADA","GERENTE",agora);c.db.update("UPDATE produtos SET ativo=false WHERE id=?",c.produto);
  erro("PRODUTO_INDISPONIVEL",()->c.service.duplicar(id));assertEquals("1",c.valor("SELECT count(*) FROM cotacoes"));erro("COTACAO_INEXISTENTE",()->c.service.duplicar(UUID.randomUUID()));erro("DADOS_INVALIDOS",()->c.service.duplicar(null));
 }
 @Test void autenticacaoExigidaNaListagemEDuplicacao() {
  c.como("inexistente");erro("ACESSO_NEGADO",()->c.service.listar(filtro(0)));erro("ACESSO_NEGADO",()->c.service.duplicar(UUID.randomUUID()));
 }
}
