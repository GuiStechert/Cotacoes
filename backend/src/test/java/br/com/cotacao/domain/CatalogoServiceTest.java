package br.com.cotacao.domain;
import br.com.cotacao.application.*;
import static br.com.cotacao.application.Catalogos.*;
import br.com.cotacao.infrastructure.CatalogoRepository;
import br.com.cotacao.support.CenarioBanco;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogoServiceTest {
 private CenarioBanco c;private CatalogoService service;
 @BeforeEach void preparar() throws Exception {
  c=new CenarioBanco();var tx=new TransactionTemplate(new DataSourceTransactionManager(c.db.getDataSource()));tx.setIsolationLevel(4);
  service=new CatalogoService(new CatalogoRepository(c.db),tx,c.identidade);
 }
 @AfterEach void limpar(){if(c!=null)c.close();}
 private void negado(String codigo){for(Runnable acao:List.<Runnable>of(()->service.clientes(),()->service.produtos(),()->service.usuarioAtual()))assertEquals(codigo,assertThrows(Cotacoes.Erro.class,acao::run).codigo());}
 @Test void usuarioAtualEhResolvidoPelaContaNoBanco(){assertEquals(new UsuarioAtual(c.comercial,"comercial","COMERCIAL"),service.usuarioAtual());c.como("gerente");assertEquals(new UsuarioAtual(c.gerente,"gerente","GERENTE"),service.usuarioAtual());}
 @Test void clientesOrdenadosSemAlteracao(){var z=UUID.fromString("00000000-0000-0000-0000-000000000002");var a=UUID.fromString("00000000-0000-0000-0000-000000000001");c.db.update("INSERT INTO clientes(id,nome) VALUES(?,'Alfa'),(?,'alfa')",z,a);assertEquals(List.of(a,z,c.cliente),service.clientes().stream().map(Cliente::id).toList());assertEquals("3",c.valor("SELECT count(*) FROM clientes"));}
 @Test void comercialRecebeMetadadosSemRealInclusiveProdutoInativo() throws Exception {
  c.db.update("UPDATE produtos SET principio_ativo='Ativo',embalagem=5,dose_recomendada=2,aplicacoes_recomendadas=3,margem_recomendada=0.1,ativo=false,data_desativacao=data_insercao+interval '1 day' WHERE id=?",c.produto);
  var p=assertInstanceOf(ProdutoComercial.class,service.produtos().getFirst());assertEquals("Ativo",p.principioAtivo());assertEquals(new BigDecimal("2"),p.doseRecomendada());assertFalse(p.ativo());assertNotNull(p.dataDesativacao());
  var json=br.com.cotacao.web.JsonConfig.mapper().writeValueAsString(p);assertFalse(json.contains("custoReal"));assertFalse(json.contains("custo_real"));
 }
 @Test void consultaComercialNaoDependeDaTabelaReal(){c.db.execute("DROP TABLE produto_custos_reais");assertEquals(1,service.produtos().size());assertInstanceOf(ProdutoComercial.class,service.produtos().getFirst());}
 @Test void aprovadorObtemSomenteUltimoCustoRealComDesempate(){
  c.db.update("INSERT INTO produto_custos_reais(id,produto_id,custo_real,data_criacao) VALUES('00000000-0000-0000-0000-000000000001',?,8,'2026-01-01'),('00000000-0000-0000-0000-000000000002',?,9,'2026-01-01'),('00000000-0000-0000-0000-000000000003',?,999,'2025-12-31')",c.produto,c.produto,c.produto);
  c.como("gerente");var p=assertInstanceOf(ProdutoAprovador.class,service.produtos().getFirst());assertEquals(new BigDecimal("9"),p.custoReal());assertEquals("3",c.valor("SELECT count(*) FROM produto_custos_reais"));
 }
 @Test void custoAusentePermiteConsultaComNuloSomenteParaAprovador(){c.como("gerente");assertNull(assertInstanceOf(ProdutoAprovador.class,service.produtos().getFirst()).custoReal());}
 @Test void todosPerfisCadastradosConsultamClientesEProdutos(){for(var perfil:List.of("GESTOR","SUPERVISOR")){var conta=perfil.toLowerCase();c.db.update("INSERT INTO usuarios(id,email,conta_windows,nome,perfil) VALUES(?,?,?,?,?)",UUID.randomUUID(),conta+"@empresa","DOM\\"+conta,conta,perfil);c.como(conta);assertEquals(1,service.clientes().size());assertInstanceOf(ProdutoAprovador.class,service.produtos().getFirst());}}
 @Test void listasVaziasValidas(){c.db.update("DELETE FROM produtos");c.db.update("DELETE FROM clientes");assertTrue(service.clientes().isEmpty());assertTrue(service.produtos().isEmpty());}
 @Test void identidadeAusenteVaziaOuDesconhecidaNegada(){when(c.identidade.contaAutenticada()).thenReturn(null);negado("NAO_AUTENTICADO");when(c.identidade.contaAutenticada()).thenReturn(" ");negado("NAO_AUTENTICADO");c.como("desconhecido");negado("ACESSO_NEGADO");}
 @Test void usuarioInativoNaoConsulta(){c.db.update("UPDATE usuarios SET ativo=false WHERE id=?",c.comercial);negado("ACESSO_NEGADO");}
}
