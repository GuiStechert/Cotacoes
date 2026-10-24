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
 public record LinhaResumo(UUID id,UUID clienteId,String clienteNome,String solicitanteNome,Instant criadoEm,
                            String status,String perfilAprovador,BigDecimal precoTotal) {}
 public record Listagem(long total,List<LinhaResumo> linhas) {}
 public CotacaoRepository(JdbcTemplate jdbc) { this.jdbc=Objects.requireNonNull(jdbc); }
 public Usuario usuario(String conta) {
  return jdbc.query("SELECT id,perfil FROM usuarios WHERE lower(conta_windows)=lower(?) AND ativo=true",(r,i)->new Usuario(r.getObject("id",UUID.class),r.getString("perfil")),conta).stream().findFirst().orElse(null);
 }
 public boolean clienteExiste(UUID id) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM clientes WHERE id=?)",Boolean.class,id)); }
 public boolean produtoAtivo(UUID id) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM produtos WHERE id=? AND ativo=true)",Boolean.class,id)); }
 public Listagem listar(Usuario usuario,Filtro filtro,Instant inicio,Instant fimExclusivo) {
  var condicoes=new StringBuilder(" WHERE ");
  var parametros=new ArrayList<Object>();
  if(usuario.perfil().equals("COMERCIAL")) {
   condicoes.append("c.solicitante_id=?");parametros.add(usuario.id());
  } else {
   condicoes.append("c.perfil_aprovador=?");parametros.add(usuario.perfil());
  }
  if(filtro.status()!=null){condicoes.append(" AND c.status=?");parametros.add(filtro.status());}
  if(filtro.clienteId()!=null){condicoes.append(" AND c.cliente_id=?");parametros.add(filtro.clienteId());}
  if(inicio!=null){condicoes.append(" AND c.criado_em>=?");parametros.add(java.sql.Timestamp.from(inicio));}
  if(fimExclusivo!=null){condicoes.append(" AND c.criado_em<?");parametros.add(java.sql.Timestamp.from(fimExclusivo));}
  long total=jdbc.queryForObject("SELECT count(*) FROM cotacoes c"+condicoes,Long.class,parametros.toArray());
  parametros.add(20);parametros.add((long)filtro.pagina()*20);
  var linhas=jdbc.query("""
   SELECT c.id,c.cliente_id,cl.nome AS cliente_nome,u.nome AS solicitante_nome,
          c.criado_em,c.status,c.perfil_aprovador,
          COALESCE((SELECT sum(i.preco*i.quantidade) FROM cotacao_itens i WHERE i.cotacao_id=c.id),0) AS preco_total
   FROM cotacoes c JOIN clientes cl ON cl.id=c.cliente_id JOIN usuarios u ON u.id=c.solicitante_id
   """+condicoes+" ORDER BY c.criado_em DESC,c.id DESC LIMIT ? OFFSET ?",
   (r,i)->new LinhaResumo(r.getObject("id",UUID.class),r.getObject("cliente_id",UUID.class),r.getString("cliente_nome"),r.getString("solicitante_nome"),r.getTimestamp("criado_em").toInstant(),r.getString("status"),r.getString("perfil_aprovador"),r.getBigDecimal("preco_total")),parametros.toArray());
  return new Listagem(total,List.copyOf(linhas));
 }
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
 public void decidirItem(UUID cotacaoId,UUID itemId,String decisao,String comentario) {
  int alterados=jdbc.update("UPDATE cotacao_itens SET status=?,comentario=? WHERE id=? AND cotacao_id=? AND status='PENDENTE'",decisao,comentario,itemId,cotacaoId);
  if(alterados!=1)throw new Erro("ESTADO_INVALIDO");
 }
 public void registrarDecisao(UUID itemId,UUID usuarioId,String decisao,String comentario,Instant agora) {
  jdbc.update("INSERT INTO historico_aprovacoes(id,item_id,usuario_id,decisao,comentario,criado_em) VALUES(?,?,?,?,?,?)",UUID.randomUUID(),itemId,usuarioId,decisao,comentario,java.sql.Timestamp.from(agora));
 }
 public void concluir(UUID id,String status,Instant agora) {
  int alterados=jdbc.update("UPDATE cotacoes SET status=?,atualizado_em=?,versao=versao+1 WHERE id=? AND status='PENDENTE_APROVACAO'",status,java.sql.Timestamp.from(agora),id);
  if(alterados!=1)throw new Erro("ESTADO_INVALIDO");
 }
 public List<EventoAprovacao> historico(UUID cotacaoId) {
  return List.copyOf(jdbc.query("""
   SELECT h.id,h.item_id,h.usuario_id,u.nome AS autor_nome,h.criado_em,h.decisao,h.comentario
   FROM historico_aprovacoes h
   JOIN cotacao_itens i ON i.id=h.item_id
   JOIN usuarios u ON u.id=h.usuario_id
   WHERE i.cotacao_id=? ORDER BY h.criado_em ASC,h.id ASC
   """,(r,i)->new EventoAprovacao(r.getObject("id",UUID.class),r.getObject("item_id",UUID.class),r.getObject("usuario_id",UUID.class),r.getString("autor_nome"),r.getTimestamp("criado_em").toInstant(),r.getString("decisao"),r.getString("comentario")),cotacaoId));
 }
}
