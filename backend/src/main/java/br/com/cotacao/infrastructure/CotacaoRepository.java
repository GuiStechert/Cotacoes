package br.com.cotacao.infrastructure;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static br.com.cotacao.application.Cotacoes.*;
public class CotacaoRepository {
 private final JdbcTemplate jdbc;
 public record Usuario(UUID id,String perfil) {}
 public record Custos(BigDecimal comercial,BigDecimal real) {}
 public record Item(UUID id,UUID produtoId,BigDecimal quantidade,BigDecimal preco,BigDecimal custo,BigDecimal custoReal,BigDecimal margem,BigDecimal margemReal,String status) {}
 public record Registro(UUID id,UUID solicitanteId,Entrada entrada,String status,String perfilAprovador,List<Item> itens) {}
 public CotacaoRepository(JdbcTemplate jdbc) { this.jdbc=Objects.requireNonNull(jdbc); }
 public Usuario usuario(String conta) {
  return jdbc.query("SELECT id,perfil FROM usuarios WHERE lower(conta_windows)=lower(?) AND ativo=true",(r,i)->new Usuario(r.getObject("id",UUID.class),r.getString("perfil")),conta).stream().findFirst().orElse(null);
 }
 public boolean clienteExiste(UUID id) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM clientes WHERE id=?)",Boolean.class,id)); }
 public boolean produtoAtivo(UUID id) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM produtos WHERE id=? AND ativo=true)",Boolean.class,id)); }
 public void criar(UUID id,UUID usuario,Entrada e,Instant agora) {
  jdbc.update("INSERT INTO cotacoes(id,cliente_id,solicitante_id,data_faturamento,data_vencimento,juros_anual,area_cliente,criado_em,atualizado_em) VALUES(?,?,?,?,?,?,?,?,?)",id,e.clienteId(),usuario,e.faturamento(),e.vencimento(),e.jurosAnual(),e.areaCliente(),java.sql.Timestamp.from(agora),java.sql.Timestamp.from(agora));
  inserirItens(id,e.itens());
 }
 public void editar(UUID id,Entrada e,Instant agora) {
  jdbc.update("UPDATE cotacoes SET cliente_id=?,data_faturamento=?,data_vencimento=?,juros_anual=?,area_cliente=?,atualizado_em=?,versao=versao+1 WHERE id=?",e.clienteId(),e.faturamento(),e.vencimento(),e.jurosAnual(),e.areaCliente(),java.sql.Timestamp.from(agora),id);
  jdbc.update("DELETE FROM cotacao_itens WHERE cotacao_id=?",id);
  inserirItens(id,e.itens());
 }
 private void inserirItens(UUID id,List<ItemEntrada> itens) {
  for(var item:itens)jdbc.update("INSERT INTO cotacao_itens(id,cotacao_id,produto_id,quantidade,preco) VALUES(?,?,?,?,?)",UUID.randomUUID(),id,item.produtoId(),item.quantidade(),item.preco());
 }
 public Registro carregar(UUID id,boolean bloquear) {
  var cabecalhos=jdbc.query("SELECT * FROM cotacoes WHERE id=?"+(bloquear?" FOR UPDATE":""),(r,i)->new Registro(id,r.getObject("solicitante_id",UUID.class),new Entrada(r.getObject("cliente_id",UUID.class),r.getDate("data_faturamento").toLocalDate(),r.getDate("data_vencimento").toLocalDate(),r.getBigDecimal("juros_anual"),r.getBigDecimal("area_cliente"),List.of()),r.getString("status"),r.getString("perfil_aprovador"),List.of()),id);
  if(cabecalhos.isEmpty())return null;
  var cab=cabecalhos.getFirst();
  var itens=jdbc.query("SELECT * FROM cotacao_itens WHERE cotacao_id=? ORDER BY id",(r,i)->new Item(r.getObject("id",UUID.class),r.getObject("produto_id",UUID.class),r.getBigDecimal("quantidade"),r.getBigDecimal("preco"),r.getBigDecimal("custo"),r.getBigDecimal("custo_real"),r.getBigDecimal("margem"),r.getBigDecimal("margem_real"),r.getString("status")),id);
  var e=cab.entrada();
  return new Registro(id,cab.solicitanteId(),new Entrada(e.clienteId(),e.faturamento(),e.vencimento(),e.jurosAnual(),e.areaCliente(),itens.stream().map(v->new ItemEntrada(v.produtoId(),v.quantidade(),v.preco())).toList()),cab.status(),cab.perfilAprovador(),List.copyOf(itens));
 }
 public Custos custos(UUID produto) {
  return jdbc.queryForObject("SELECT (SELECT custo FROM produto_custos WHERE produto_id=? ORDER BY data_criacao DESC,id DESC LIMIT 1) AS comercial, (SELECT custo_real FROM produto_custos_reais WHERE produto_id=? ORDER BY data_criacao DESC,id DESC LIMIT 1) AS real",(r,i)->new Custos(r.getBigDecimal("comercial"),r.getBigDecimal("real")),produto,produto);
 }
 public void snapshot(Item item) {
  jdbc.update("UPDATE cotacao_itens SET custo=?,custo_real=?,margem=?,margem_real=?,status='PENDENTE' WHERE id=?",item.custo(),item.custoReal(),item.margem(),item.margemReal(),item.id());
 }
 public void enviar(UUID id,String perfil,Instant agora) {
  jdbc.update("UPDATE cotacoes SET status='PENDENTE_APROVACAO',perfil_aprovador=?,atualizado_em=?,versao=versao+1 WHERE id=?",perfil,java.sql.Timestamp.from(agora),id);
 }
}
