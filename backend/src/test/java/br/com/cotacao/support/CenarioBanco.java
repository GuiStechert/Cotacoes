package br.com.cotacao.support;

import br.com.cotacao.application.*;
import br.com.cotacao.infrastructure.CotacaoRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;

public final class CenarioBanco implements AutoCloseable {
 public final JdbcTemplate db;
 public final CotacoesService service;
 public final Identidade identidade=mock(Identidade.class);
 public final UUID comercial=UUID.randomUUID(),outro=UUID.randomUUID(),gerente=UUID.randomUUID();
 public final UUID cliente=UUID.randomUUID(),produto=UUID.randomUUID();
 private final JdbcTemplate admin;
 private final String schema="api_"+UUID.randomUUID().toString().replace("-","");
 public CenarioBanco() throws Exception {
  admin=new JdbcTemplate(new DriverManagerDataSource("jdbc:postgresql://127.0.0.1:55432/postgres","cotacao_test",""));
  admin.execute("CREATE SCHEMA "+schema);
  var ds=new DriverManagerDataSource("jdbc:postgresql://127.0.0.1:55432/postgres?currentSchema="+schema,"cotacao_test","");
  db=new JdbcTemplate(ds);
  db.execute(Files.readString(Path.of("src/main/resources/db/migration/V1__estrutura_inicial.sql")));
  var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
  tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
  when(identidade.contaAutenticada()).thenReturn("DOM\\comercial");
  service=new CotacoesService(new CotacaoRepository(db),tx,identidade,Clock.fixed(Instant.parse("2026-10-24T15:00:00Z"),ZoneId.of("America/Cuiaba")));
  usuario(comercial,"comercial","COMERCIAL");usuario(outro,"outro","COMERCIAL");usuario(gerente,"gerente","GERENTE");
  db.update("INSERT INTO clientes(id,nome) VALUES(?,'Cliente')",cliente);
  db.update("INSERT INTO produtos(id,nome,unidade_medida) VALUES(?,'Produto','L')",produto);
 }
 private void usuario(UUID id,String conta,String perfil) { db.update("INSERT INTO usuarios(id,email,conta_windows,nome,perfil) VALUES(?,?,?,?,?)",id,conta+"@empresa","DOM\\"+conta,conta,perfil); }
 public void como(String conta) { when(identidade.contaAutenticada()).thenReturn("DOM\\"+conta); }
 public String schema() { return schema; }
 public String valor(String sql,Object... args) { return db.queryForObject(sql,String.class,args); }
 public void cotacao(UUID id,UUID dono,String estado,String perfil,Instant criacao) {
  db.update("INSERT INTO cotacoes(id,cliente_id,solicitante_id,data_faturamento,data_vencimento,juros_anual,area_cliente,status,perfil_aprovador,criado_em) VALUES(?,?,?,'2026-01-01','2026-08-01',0.12,50,?,?,?)",id,cliente,dono,estado,perfil,java.sql.Timestamp.from(criacao));
  db.update("INSERT INTO cotacao_itens(id,cotacao_id,produto_id,quantidade,preco) VALUES(?,?,?,2,10.01)",UUID.randomUUID(),id,produto);
 }
 @Override public void close() { admin.execute("DROP SCHEMA "+schema+" CASCADE"); }
}
