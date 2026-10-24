package br.com.cotacao.infrastructure;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.UUID;
import java.sql.*;
import java.time.Instant;
import static br.com.cotacao.application.Catalogos.*;

public class CatalogoRepository {
 private final JdbcTemplate jdbc;
 private static final String COLUNAS="p.id,p.nome,p.principio_ativo,p.embalagem,p.dose_recomendada,p.aplicacoes_recomendadas,p.unidade_medida,p.margem_recomendada,p.data_insercao,p.data_desativacao,p.ativo";
 public CatalogoRepository(JdbcTemplate jdbc) {this.jdbc=jdbc;}
 public UsuarioAtual usuario(String conta){return jdbc.query("SELECT id,nome,perfil FROM usuarios WHERE lower(conta_windows)=lower(?) AND ativo=true",(r,i)->new UsuarioAtual(r.getObject("id",UUID.class),r.getString("nome"),r.getString("perfil")),conta).stream().findFirst().orElse(null);}
 public List<Cliente> clientes(){return List.copyOf(jdbc.query("SELECT id,nome FROM clientes ORDER BY lower(nome),id",(r,i)->new Cliente(r.getObject("id",UUID.class),r.getString("nome"))));}
 public List<ProdutoComercial> produtosComerciais(){return List.copyOf(jdbc.query("SELECT "+COLUNAS+" FROM produtos p ORDER BY lower(p.nome),p.id",(r,i)->metadados(r)));}
 public List<ProdutoAprovador> produtosAprovadores(){
  return List.copyOf(jdbc.query("SELECT "+COLUNAS+",cr.custo_real FROM produtos p LEFT JOIN LATERAL (SELECT custo_real FROM produto_custos_reais WHERE produto_id=p.id ORDER BY data_criacao DESC,id DESC LIMIT 1) cr ON true ORDER BY lower(p.nome),p.id",(r,i)->{
   var p=metadados(r);return new ProdutoAprovador(p.id(),p.nome(),p.principioAtivo(),p.embalagem(),p.doseRecomendada(),p.aplicacoesRecomendadas(),p.unidadeMedida(),p.margemRecomendada(),p.dataInsercao(),p.dataDesativacao(),p.ativo(),r.getBigDecimal("custo_real"));
  }));
 }
 private ProdutoComercial metadados(ResultSet r) throws SQLException {
  return new ProdutoComercial(r.getObject("id",UUID.class),r.getString("nome"),r.getString("principio_ativo"),r.getBigDecimal("embalagem"),r.getBigDecimal("dose_recomendada"),r.getBigDecimal("aplicacoes_recomendadas"),r.getString("unidade_medida"),r.getBigDecimal("margem_recomendada"),instante(r,"data_insercao"),instante(r,"data_desativacao"),r.getBoolean("ativo"));
 }
 private Instant instante(ResultSet r,String coluna) throws SQLException {var data=r.getTimestamp(coluna);return data==null?null:data.toInstant();}
}
