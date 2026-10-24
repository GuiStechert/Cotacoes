package br.com.cotacao.domain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
class FinanceiroTest {
 private static BigDecimal n(String s) { return new BigDecimal(s); }
 @Test void mesesInclusivos() { assertEquals(8, Financeiro.meses(LocalDate.of(2026,1,31),LocalDate.of(2026,8,1))); assertEquals(2,Financeiro.meses(LocalDate.of(2026,12,31),LocalDate.of(2027,1,1))); assertEquals(1,Financeiro.meses(LocalDate.of(2026,1,1),LocalDate.of(2026,1,1))); }
 @Test void datasInvalidas() { assertThrows(IllegalArgumentException.class,()->Financeiro.meses(LocalDate.of(2026,2,1),LocalDate.of(2026,1,31))); assertThrows(IllegalArgumentException.class,()->Financeiro.meses(null,LocalDate.now())); }
 @ParameterizedTest @CsvSource({"100.001,100.01","100.990,100.99","0.001,0.01"}) void precoParaCima(String entrada,String esperado) { assertEquals(n(esperado),Financeiro.preco(n(entrada))); }
 @Test void jurosSimples() { assertEquals(0,n("0.108").compareTo(Financeiro.margem(n("10"),n("100"),n("0.12"),LocalDate.of(2026,1,1),LocalDate.of(2026,8,1)))); }
 @Test void precoArredondadoAntesDaMargem() { assertEquals(0,n("0.1").compareTo(Financeiro.margem(n("10"),n("99.999"),BigDecimal.ZERO,LocalDate.of(2026,1,1),LocalDate.of(2026,1,1)))); }
 @ParameterizedTest @CsvSource({"0.07999,GERENTE","0.08,SUPERVISOR","0.08001,SUPERVISOR","0.09999,SUPERVISOR","0.10,GESTOR","0.10001,GESTOR"}) void limitesDeAlcada(String margem,String perfil) { assertEquals(perfil,Financeiro.alcada(n(margem))); }
 @Test void percentualApenasExibicao() { assertEquals(n("8.99"),Financeiro.percentual(n("0.089999"))); assertEquals("SUPERVISOR",Financeiro.alcada(n("0.089999"))); }
 @Test void rejeitaValoresInvalidos() { for(String valor:new String[]{"0","-1"}) assertThrows(IllegalArgumentException.class,()->Financeiro.preco(n(valor))); assertThrows(IllegalArgumentException.class,()->Financeiro.preco(null)); assertThrows(IllegalArgumentException.class,()->Financeiro.alcada(n("-0.1"))); assertThrows(IllegalArgumentException.class,()->Financeiro.alcada(null)); assertThrows(IllegalArgumentException.class,()->Financeiro.margem(n("-1"),n("1"),n("0"),LocalDate.now(),LocalDate.now())); assertThrows(IllegalArgumentException.class,()->Financeiro.margem(n("1"),n("1"),n("-1"),LocalDate.now(),LocalDate.now())); }
}
