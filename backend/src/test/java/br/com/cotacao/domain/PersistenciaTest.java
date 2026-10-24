package br.com.cotacao.domain;

import org.junit.jupiter.api.*;
import java.sql.*;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PersistenciaTest {
 private Connection db;
 private String schema;
 private void sql(String value) throws SQLException { try(var s=db.createStatement()) { s.execute(value); } }
 private String scalar(String value) throws SQLException { try(var s=db.createStatement(); var r=s.executeQuery(value)) { r.next(); return r.getString(1); } }
 private void rejeita(String state,String query) { assertEquals(state,assertThrows(SQLException.class,()->sql(query)).getSQLState()); }
 @BeforeEach void preparar() throws Exception {
  db=DriverManager.getConnection("jdbc:postgresql://127.0.0.1:55432/postgres","cotacao_test","");
  schema="teste_"+UUID.randomUUID().toString().replace("-","");
  sql("CREATE SCHEMA "+schema); sql("SET search_path TO "+schema);
  sql(Files.readString(Path.of("src/main/resources/db/migration/V1__estrutura_inicial.sql")));
 }
 @AfterEach void limpar() throws Exception { if(db!=null) { try { sql("DROP SCHEMA "+schema+" CASCADE"); } finally { db.close(); } } }
 private void produto() throws Exception { sql("INSERT INTO produtos(id,nome,unidade_medida) VALUES ('00000000-0000-0000-0000-000000000001','Produto','L')"); }
 @Test void criaOitoTabelas() throws Exception { assertEquals("8",scalar("SELECT count(*) FROM information_schema.tables WHERE table_schema=current_schema() AND table_type='BASE TABLE'")); }
 @Test void custosIndependentesEImutaveis() throws Exception {
  produto();
  sql("INSERT INTO produto_custos(id,produto_id,custo,data_criacao) VALUES ('00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000001',10,'2026-01-01'),('00000000-0000-0000-0000-000000000003','00000000-0000-0000-0000-000000000001',12,'2026-01-01')");
  sql("INSERT INTO produto_custos_reais(id,produto_id,custo_real) VALUES (gen_random_uuid(),'00000000-0000-0000-0000-000000000001',8)");
  assertEquals("12",scalar("SELECT custo FROM produto_custos ORDER BY data_criacao DESC,id DESC LIMIT 1"));
  assertEquals("8",scalar("SELECT custo_real FROM produto_custos_reais ORDER BY data_criacao DESC,id DESC LIMIT 1"));
  rejeita("55000","UPDATE produto_custos SET custo=99"); rejeita("55000","DELETE FROM produto_custos_reais");
 }
 @Test void validaCadastro() throws Exception {
  rejeita("23514","INSERT INTO usuarios(id,email,conta_windows,nome,perfil) VALUES(gen_random_uuid(),'a@b','DOM\\a','A','ADMIN')");
  sql("INSERT INTO usuarios(id,email,conta_windows,nome,perfil) VALUES(gen_random_uuid(),'a@b','DOM\\a','A','COMERCIAL')");
  rejeita("23505","INSERT INTO usuarios(id,email,conta_windows,nome,perfil) VALUES(gen_random_uuid(),'A@B','DOM\\b','B','GESTOR')");
  rejeita("23514","INSERT INTO produtos(id,nome,unidade_medida,dose_recomendada) VALUES(gen_random_uuid(),'A','L',0)");
  rejeita("23503","INSERT INTO produto_custos(id,produto_id,custo) VALUES(gen_random_uuid(),gen_random_uuid(),1)");
 }
 @Test void rollbackNaoDeixaCustosParciais() throws Exception {
  produto(); db.setAutoCommit(false);
  sql("INSERT INTO produto_custos(id,produto_id,custo) VALUES(gen_random_uuid(),'00000000-0000-0000-0000-000000000001',1)");
  rejeita("23514","INSERT INTO produto_custos_reais(id,produto_id,custo_real) VALUES(gen_random_uuid(),'00000000-0000-0000-0000-000000000001',-1)");
  db.rollback(); db.setAutoCommit(true);
  assertEquals("0",scalar("SELECT count(*) FROM produto_custos"));
 }
 @Test void validaCotacaoItemESnapshot() throws Exception {
  produto();
  sql("INSERT INTO usuarios(id,email,conta_windows,nome,perfil) VALUES('00000000-0000-0000-0000-000000000010','a@b','DOM\\a','A','COMERCIAL')");
  sql("INSERT INTO clientes(id,nome) VALUES('00000000-0000-0000-0000-000000000020','Cliente')");
  String cab="INSERT INTO cotacoes(id,cliente_id,solicitante_id,data_faturamento,data_vencimento,juros_anual,area_cliente) VALUES('00000000-0000-0000-0000-000000000030','00000000-0000-0000-0000-000000000020','00000000-0000-0000-0000-000000000010','2026-01-01','2026-08-01',0.12,";
  rejeita("23514",cab+"0)"); sql(cab+"NULL)");
  rejeita("23514","UPDATE cotacoes SET data_vencimento='2025-12-31'");
  rejeita("23514","UPDATE cotacoes SET status='DESCONHECIDO'");
  rejeita("23514","UPDATE cotacoes SET status='PENDENTE_APROVACAO'");
  assertEquals("RASCUNHO",scalar("SELECT status FROM cotacoes"));
  String item="INSERT INTO cotacao_itens(id,cotacao_id,produto_id,quantidade,preco) VALUES(gen_random_uuid(),'00000000-0000-0000-0000-000000000030','00000000-0000-0000-0000-000000000001',";
  rejeita("23514",item+"0,100)"); rejeita("23514",item+"1,100.001)"); sql(item+"1,100)");
  rejeita("23514","UPDATE cotacao_itens SET status='PENDENTE'");
  sql("UPDATE cotacao_itens SET custo=10,custo_real=8,margem=0.108,margem_real=0.0864,status='PENDENTE'");
  sql("INSERT INTO produto_custos_reais(id,produto_id,custo_real) VALUES(gen_random_uuid(),'00000000-0000-0000-0000-000000000001',999)");
  assertEquals("8",scalar("SELECT custo_real FROM cotacao_itens"));
  rejeita("23503","DELETE FROM produtos");
 }
}
