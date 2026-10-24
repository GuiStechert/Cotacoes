package br.com.cotacao.domain;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
/** Núcleo decimal puro; o percentual de apresentação não altera a alçada. */
public final class Financeiro {
 private Financeiro() {}
 private static final MathContext MC = MathContext.DECIMAL128;
 private static BigDecimal naoNegativo(BigDecimal valor, String campo) {
  if (valor == null || valor.signum() < 0) throw new IllegalArgumentException(campo + " deve ser não negativo");
  return valor;
 }
 public static long meses(LocalDate inicio, LocalDate fim) {
  if (inicio == null || fim == null || fim.isBefore(inicio)) throw new IllegalArgumentException("Datas obrigatórias e vencimento não anterior ao faturamento");
  return ChronoUnit.MONTHS.between(inicio.withDayOfMonth(1),fim.withDayOfMonth(1)) + 1;
 }
 public static BigDecimal preco(BigDecimal valor) {
  if (valor == null || valor.signum() <= 0) throw new IllegalArgumentException("Preço deve ser positivo");
  return valor.setScale(2,RoundingMode.CEILING);
 }
 public static BigDecimal margem(BigDecimal custo, BigDecimal valor, BigDecimal anual, LocalDate inicio, LocalDate fim) {
  naoNegativo(custo,"Custo"); naoNegativo(anual,"Juros anuais");
  BigDecimal fator = BigDecimal.ONE.add(anual.divide(BigDecimal.valueOf(12),MC).multiply(BigDecimal.valueOf(meses(inicio,fim)),MC),MC);
  return custo.multiply(fator,MC).divide(preco(valor),MC);
 }
 public static BigDecimal percentual(BigDecimal margem) {
  return naoNegativo(margem,"Margem").multiply(BigDecimal.valueOf(100)).setScale(2,RoundingMode.FLOOR);
 }
 public static String alcada(BigDecimal margem) {
  naoNegativo(margem,"Margem");
  if (margem.compareTo(new BigDecimal("0.10")) >= 0) return "GESTOR";
  if (margem.compareTo(new BigDecimal("0.08")) >= 0) return "SUPERVISOR";
  return "GERENTE";
 }
}
