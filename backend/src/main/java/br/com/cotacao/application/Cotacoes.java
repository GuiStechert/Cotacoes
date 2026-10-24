package br.com.cotacao.application;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
public final class Cotacoes {
 private Cotacoes() {}
 public record ItemEntrada(UUID produtoId, BigDecimal quantidade, BigDecimal preco) {}
 public record Entrada(UUID clienteId, LocalDate faturamento, LocalDate vencimento,
                       BigDecimal jurosAnual, BigDecimal areaCliente, List<ItemEntrada> itens) {}
 public sealed interface Visao permits Comercial, Aprovador {}
 public record ItemComercial(UUID id, UUID produtoId, BigDecimal quantidade, BigDecimal preco, BigDecimal margem, String status) {}
 public record Comercial(UUID id, UUID clienteId, LocalDate faturamento, LocalDate vencimento,
                         BigDecimal jurosAnual, BigDecimal areaCliente, String status, String perfilAprovador,
                         BigDecimal precoTotal, BigDecimal margemPonderada, List<ItemComercial> itens) implements Visao {}
 public record ItemAprovador(UUID id, UUID produtoId, BigDecimal quantidade, BigDecimal preco,
                             BigDecimal custoReal, BigDecimal margemReal, String status) {}
 public record Aprovador(UUID id, String status, String perfilAprovador, BigDecimal precoTotal,
                         BigDecimal margemRealPonderada, List<ItemAprovador> itens) implements Visao {}
 public static final class Erro extends RuntimeException {
  private final String codigo;
  public Erro(String codigo) { super(codigo); this.codigo=codigo; }
  public String codigo() { return codigo; }
 }
}
