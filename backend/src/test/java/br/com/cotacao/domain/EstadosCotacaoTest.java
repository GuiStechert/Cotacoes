package br.com.cotacao.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EstadosCotacaoTest {
 @ParameterizedTest
 @CsvSource({"RASCUNHO,RASCUNHO,RASCUNHO", "PENDENTE,PENDENTE,PENDENTE_APROVACAO",
             "APROVADO,PENDENTE,PENDENTE_APROVACAO", "REPROVADO,PENDENTE,PENDENTE_APROVACAO",
             "APROVADO,APROVADO,APROVADA", "REPROVADO,REPROVADO,REPROVADA",
             "APROVADO,REPROVADO,APROVADA_PARCIAL"})
 void consolidaSomenteEstadosPrevistos(String a,String b,String esperado) {
  assertEquals(esperado,EstadosCotacao.consolidar(List.of(a,b)));
 }
 @Test void rejeitaListaVaziaNulosDesconhecidosOuMisturaDeRascunho() {
  var casos=new ArrayList<Collection<String>>();
  casos.add(null);casos.add(List.of());casos.add(Arrays.asList("APROVADO",null));
  casos.add(List.of("DESCONHECIDO"));casos.add(List.of("RASCUNHO","PENDENTE"));casos.add(List.of("RASCUNHO","APROVADO"));
  for(var estados:casos)assertEquals("Estados de itens inválidos",assertThrows(IllegalArgumentException.class,()->EstadosCotacao.consolidar(estados)).getMessage());
 }
}
