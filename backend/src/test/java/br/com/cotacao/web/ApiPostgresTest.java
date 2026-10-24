package br.com.cotacao.web;

import br.com.cotacao.CotacaoApplication;
import br.com.cotacao.support.CenarioBanco;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Aceite da composição real: filtros, MVC, identidade, serviço, transações e PostgreSQL. */
@SpringBootTest(classes=CotacaoApplication.class,properties={"spring.main.banner-mode=off","logging.level.root=WARN"})
@AutoConfigureMockMvc
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class ApiPostgresTest {
 private static CenarioBanco banco;
 @Autowired MockMvc mvc;@Autowired ObjectMapper mapper;
 @DynamicPropertySource static void configurar(DynamicPropertyRegistry props) throws Exception {
  banco=new CenarioBanco();
  props.add("DATABASE_URL",()->"postgresql://cotacao_test:@127.0.0.1:55432/postgres?currentSchema="+banco.schema());
 }
 @AfterAll static void limpar(){if(banco!=null)banco.close();}
 private MockHttpSession sessao(String conta){var s=new MockHttpSession();s.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated("DOM\\"+conta,"",List.of())));return s;}
 @Test void fluxoHttpComPersistenciaEAutorizacaoReais() throws Exception {
  var sessao=sessao("comercial");
  var token=mapper.readTree(mvc.perform(get("/api/csrf").session(sessao)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  var cabecalho=token.get("headerName").asText();var valor=token.get("token").asText();
  String entrada="""
   {"clienteId":"%s","faturamento":"2026-01-01","vencimento":"2026-08-01","jurosAnual":0.12,"itens":[{"produtoId":"%s","quantidade":2,"preco":100.001}]}
   """.formatted(banco.cliente,banco.produto);
  var criada=mvc.perform(post("/api/cotacoes").session(sessao).header(cabecalho,valor).contentType("application/json").content(entrada)).andExpect(status().isCreated()).andReturn();
  var id=mapper.readTree(criada.getResponse().getContentAsString()).get("id").asText();
  mvc.perform(get("/api/cotacoes/"+id).session(sessao)).andExpect(status().isOk()).andExpect(jsonPath("$.faturamento").value("2026-01-01")).andExpect(jsonPath("$.itens[0].preco").value(100.01));
  mvc.perform(get("/api/cotacoes/"+id).session(sessao("outro"))).andExpect(status().isForbidden());
  mvc.perform(post("/api/cotacoes/"+id+"/enviar").session(sessao).header(cabecalho,valor)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("CUSTO_AUSENTE"));
  assertEquals("RASCUNHO",banco.valor("SELECT status FROM cotacoes WHERE id=?",UUID.fromString(id)));
  banco.db.update("INSERT INTO produto_custos(id,produto_id,custo) VALUES(?,?,10)",UUID.randomUUID(),banco.produto);
  banco.db.update("INSERT INTO produto_custos_reais(id,produto_id,custo_real) VALUES(?,?,1)",UUID.randomUUID(),banco.produto);
  mvc.perform(put("/api/cotacoes/"+id).session(sessao).header(cabecalho,valor).contentType("application/json").content(entrada)).andExpect(status().isNoContent());
  mvc.perform(post("/api/cotacoes/"+id+"/enviar").session(sessao).header(cabecalho,valor)).andExpect(status().isNoContent());
  mvc.perform(get("/api/cotacoes").session(sessao).param("status","PENDENTE_APROVACAO")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.conteudo[0].perfilAprovador").value("GERENTE")).andExpect(jsonPath("$.conteudo[0].podeDuplicar").value(true));
  mvc.perform(get("/api/cotacoes/"+id).session(sessao("gerente"))).andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].custoReal").value(1)).andExpect(jsonPath("$.margemPonderada").doesNotExist());
  var copia=mvc.perform(post("/api/cotacoes/"+id+"/duplicar").session(sessao).header(cabecalho,valor)).andExpect(status().isCreated()).andReturn();
  var novo=mapper.readTree(copia.getResponse().getContentAsString()).get("id").asText();assertNotEquals(id,novo);
  assertEquals("RASCUNHO",banco.valor("SELECT status FROM cotacoes WHERE id=?",UUID.fromString(novo)));
  assertEquals("PENDENTE_APROVACAO",banco.valor("SELECT status FROM cotacoes WHERE id=?",UUID.fromString(id)));
 }
}
