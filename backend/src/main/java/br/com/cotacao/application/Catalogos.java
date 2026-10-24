package br.com.cotacao.application;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class Catalogos {
 private Catalogos() {}
 public record Cliente(UUID id,String nome) {}
 public record UsuarioAtual(UUID id,String nome,String perfil) {}
 public sealed interface Produto permits ProdutoComercial,ProdutoAprovador {}
 public record ProdutoComercial(UUID id,String nome,String principioAtivo,BigDecimal embalagem,
   BigDecimal doseRecomendada,BigDecimal aplicacoesRecomendadas,String unidadeMedida,BigDecimal margemRecomendada,
   Instant dataInsercao,Instant dataDesativacao,boolean ativo) implements Produto {}
 public record ProdutoAprovador(UUID id,String nome,String principioAtivo,BigDecimal embalagem,
   BigDecimal doseRecomendada,BigDecimal aplicacoesRecomendadas,String unidadeMedida,BigDecimal margemRecomendada,
   Instant dataInsercao,Instant dataDesativacao,boolean ativo,BigDecimal custoReal) implements Produto {}
}
