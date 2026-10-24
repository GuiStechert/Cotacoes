package br.com.cotacao.web;
import static br.com.cotacao.application.Cotacoes.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
class JsonFormatoTest {
 @Test void datasEInstantesSaoIsoParaOFrontend() throws Exception {
  var mapper=JsonConfig.mapper();
  var comercial=new Comercial(UUID.randomUUID(),UUID.randomUUID(),LocalDate.of(2026,1,1),LocalDate.of(2026,8,1),BigDecimal.ZERO,null,"RASCUNHO",null,BigDecimal.TEN,null,List.of());
  var json=mapper.readTree(mapper.writeValueAsString(comercial));
  assertEquals("2026-01-01",json.get("faturamento").textValue());assertEquals("2026-08-01",json.get("vencimento").textValue());
  var resumo=new Resumo(UUID.randomUUID(),UUID.randomUUID(),"Cliente","Comercial",Instant.parse("2026-10-24T15:00:00Z"),"RASCUNHO",null,BigDecimal.TEN,true,false);
  assertEquals("2026-10-24T15:00:00Z",mapper.readTree(mapper.writeValueAsString(resumo)).get("criadoEm").textValue());
 }
}
