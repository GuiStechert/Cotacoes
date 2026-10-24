package br.com.cotacao.web;

import br.com.cotacao.CotacaoApplication;
import br.com.cotacao.support.CenarioBanco;
import static br.com.cotacao.application.Cotacoes.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.test.context.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=CotacaoApplication.class,properties={"spring.main.banner-mode=off","logging.level.root=WARN"})
@AutoConfigureMockMvc
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
@Import(AprovacaoApiPostgresTest.Relogio.class)
class AprovacaoApiPostgresTest {
 private static CenarioBanco banco;
 @Autowired MockMvc mvc;@Autowired ObjectMapper mapper;
 @TestConfiguration static class Relogio {
  @Bean @Primary Clock clockDeTeste(){return Clock.fixed(Instant.parse("2026-10-24T15:00:00Z"),ZoneOffset.UTC);}
 }
 @DynamicPropertySource static void configurar(DynamicPropertyRegistry props) throws Exception {
  banco=new CenarioBanco();props.add("DATABASE_URL",()->"postgresql://cotacao_test:@127.0.0.1:55432/postgres?currentSchema="+banco.schema());
 }
 @AfterAll static void limpar(){if(banco!=null)banco.close();}
 private record Cliente(MockHttpSession sessao,String cabecalho,String token) {}
 private Cliente autenticarNoTeste(String conta) throws Exception {
  var sessao=new MockHttpSession();sessao.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated("DOM\\"+conta,"",List.of())));
  var resposta=mvc.perform(get("/api/csrf").session(sessao)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  var json=mapper.readTree(resposta);return new Cliente(sessao,json.get("headerName").asText(),json.get("token").asText());
 }
 @Test void confirmaMisturaAuditaEProtegeReconfirmacaoComFiltrosEServicosReais() throws Exception {
  banco.db.update("INSERT INTO produto_custos(id,produto_id,custo) VALUES(?,?,10)",UUID.randomUUID(),banco.produto);
  banco.db.update("INSERT INTO produto_custos_reais(id,produto_id,custo_real) VALUES(?,?,1)",UUID.randomUUID(),banco.produto);
  var entrada=new Entrada(banco.cliente,LocalDate.of(2026,1,1),LocalDate.of(2026,8,1),BigDecimal.ZERO,null,List.of(new ItemEntrada(banco.produto,BigDecimal.ONE,new BigDecimal("100")),new ItemEntrada(banco.produto,new BigDecimal("2"),new BigDecimal("200"))));
  var id=banco.service.criar(entrada);banco.service.enviar(id);
  var itens=banco.db.query("SELECT id FROM cotacao_itens WHERE cotacao_id=? ORDER BY id",(r,i)->r.getObject(1,UUID.class),id);
  var gerente=autenticarNoTeste("gerente");var comercial=autenticarNoTeste("comercial");var outro=autenticarNoTeste("outro");
  mvc.perform(get("/api/aprovacoes/pendentes").session(gerente.sessao())).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
  mvc.perform(get("/api/aprovacoes/pendentes").session(comercial.sessao())).andExpect(status().isForbidden());
  mvc.perform(get("/api/cotacoes/"+id+"/historico").session(gerente.sessao())).andExpect(status().isOk()).andExpect(content().json("[]"));
  assertEquals("2",banco.valor("SELECT count(*) FROM cotacao_itens WHERE status='PENDENTE'"));
  var corpo=mapper.writeValueAsString(new Decisoes(List.of(new DecisaoItem(itens.getFirst(),true,null),new DecisaoItem(itens.getLast(),false,"Revisar preço"))));
  mvc.perform(post("/api/cotacoes/"+id+"/decisoes").session(gerente.sessao()).contentType("application/json").content(corpo)).andExpect(status().isForbidden());
  mvc.perform(post("/api/cotacoes/"+id+"/decisoes").session(comercial.sessao()).header(comercial.cabecalho(),comercial.token()).contentType("application/json").content(corpo)).andExpect(status().isForbidden());
  var incompleto=mapper.writeValueAsString(new Decisoes(List.of(new DecisaoItem(itens.getFirst(),true,null))));
  mvc.perform(post("/api/cotacoes/"+id+"/decisoes").session(gerente.sessao()).header(gerente.cabecalho(),gerente.token()).contentType("application/json").content(incompleto)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DECISOES_INVALIDAS"));
  var semBooleano=mapper.writeValueAsString(new Decisoes(List.of(new DecisaoItem(itens.getFirst(),null,null),new DecisaoItem(itens.getLast(),false,null))));
  mvc.perform(post("/api/cotacoes/"+id+"/decisoes").session(gerente.sessao()).header(gerente.cabecalho(),gerente.token()).contentType("application/json").content(semBooleano)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));
  assertEquals("0",banco.valor("SELECT count(*) FROM historico_aprovacoes"));
  mvc.perform(post("/api/cotacoes/"+id+"/decisoes").session(gerente.sessao()).header(gerente.cabecalho(),gerente.token()).contentType("application/json").content(corpo)).andExpect(status().isNoContent());
  mvc.perform(get("/api/cotacoes/"+id).session(comercial.sessao())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADA_PARCIAL")).andExpect(jsonPath("$.margemRealPonderada").doesNotExist());
  mvc.perform(get("/api/aprovacoes/pendentes").session(gerente.sessao())).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
  var historico=mvc.perform(get("/api/cotacoes/"+id+"/historico").session(comercial.sessao())).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].usuarioId").value(banco.gerente.toString())).andExpect(jsonPath("$[0].criadoEm").value("2026-10-24T15:00:00Z")).andExpect(jsonPath("$[0].custoReal").doesNotExist()).andReturn().getResponse().getContentAsString();
  assertTrue(historico.contains("Revisar preço"));mvc.perform(get("/api/cotacoes/"+id+"/historico").session(outro.sessao())).andExpect(status().isForbidden());
  mvc.perform(post("/api/cotacoes/"+id+"/decisoes").session(gerente.sessao()).header(gerente.cabecalho(),gerente.token()).contentType("application/json").content(corpo)).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("ESTADO_INVALIDO"));
  assertEquals("2",banco.valor("SELECT count(*) FROM historico_aprovacoes"));
 }
}
