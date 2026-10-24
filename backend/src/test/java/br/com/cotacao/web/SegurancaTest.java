package br.com.cotacao.web;
import br.com.cotacao.CotacaoApplication;
import br.com.cotacao.application.CotacoesService;
import static br.com.cotacao.application.Cotacoes.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.core.env.Environment;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=CotacaoApplication.class,properties={
 "DATABASE_URL=postgresql://cotacao_test:@127.0.0.1:55432/postgres",
 "spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/postgres",
 "spring.datasource.username=cotacao_test","spring.datasource.password=",
 "spring.main.banner-mode=off","logging.level.root=WARN"})
@AutoConfigureMockMvc
class SegurancaTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper mapper;@Autowired Environment ambiente;
 @MockitoBean CotacoesService service;
 private MockHttpSession sessao(){var s=new MockHttpSession();s.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated("DOM\\comercial","",List.of())));return s;}
 private String entrada(){return "{\"clienteId\":\""+UUID.randomUUID()+"\",\"faturamento\":\"2026-01-01\",\"vencimento\":\"2026-08-01\",\"jurosAnual\":0.12,\"itens\":[]}";}
 @Test void anonimoRecebe401SemRedirecionamento() throws Exception {mvc.perform(get("/api/cotacoes")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO")).andExpect(header().doesNotExist("Location"));verifyNoInteractions(service);}
 @Test void cabecalhoForjadoEBasicNaoAutenticam() throws Exception {mvc.perform(get("/api/cotacoes").header("X-Usuario","DOM\\gerente").header("X-Perfil","GERENTE")).andExpect(status().isUnauthorized());mvc.perform(get("/api/cotacoes").header("Authorization","Basic Z2VyZW50ZTpnZXJlbnRl")).andExpect(status().isUnauthorized());verifyNoInteractions(service);}
 @Test void contextoAutenticadoPodeConsultar() throws Exception {when(service.listar(any())).thenReturn(new Pagina(0,20,0,0,List.of()));mvc.perform(get("/api/cotacoes").session(sessao())).andExpect(status().isOk()).andExpect(jsonPath("$.tamanho").value(20));verify(service).listar(any());}
 @Test void escritaSemCsrfNegadaMesmoAutenticada() throws Exception {mvc.perform(post("/api/cotacoes").session(sessao()).contentType("application/json").content(entrada())).andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));verifyNoInteractions(service);}
 @Test void tokenCsrfPermiteEscritaAutenticada() throws Exception {var sessao=sessao();var resposta=mvc.perform(get("/api/csrf").session(sessao)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();var token=mapper.readTree(resposta);assertFalse(token.get("token").asText().isBlank());var id=UUID.randomUUID();when(service.criar(any())).thenReturn(id);mvc.perform(post("/api/cotacoes").session(sessao).header(token.get("headerName").asText(),token.get("token").asText()).contentType("application/json").content(entrada())).andExpect(status().isCreated());verify(service).criar(any());}
 @Test void tokenCsrfNaoAutenticaVisitante() throws Exception {var sessao=new MockHttpSession();var resposta=mvc.perform(get("/api/csrf").session(sessao)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();var token=mapper.readTree(resposta);mvc.perform(post("/api/cotacoes").session(sessao).header(token.get("headerName").asText(),token.get("token").asText()).contentType("application/json").content(entrada())).andExpect(status().isUnauthorized());verifyNoInteractions(service);}
 @Test void mapperRealDoSpringRejeitaCampoInjetado() throws Exception {var sessao=sessao();var resposta=mvc.perform(get("/api/csrf").session(sessao)).andReturn().getResponse().getContentAsString();var token=mapper.readTree(resposta);mvc.perform(post("/api/cotacoes").session(sessao).header(token.get("headerName").asText(),token.get("token").asText()).contentType("application/json").content(entrada().replaceFirst("\\{","{\"custoReal\":1,"))).andExpect(status().isBadRequest());verifyNoInteractions(service);}
 @Test void enderecoLocalPorPadrao(){assertEquals("127.0.0.1",ambiente.getProperty("server.address"));}
 @Test void tokenDeOutraSessaoContinuaNegado() throws Exception {var resposta=mvc.perform(get("/api/csrf").session(sessao())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();var token=mapper.readTree(resposta);mvc.perform(post("/api/cotacoes").session(sessao()).header(token.get("headerName").asText(),token.get("token").asText()).contentType("application/json").content(entrada())).andExpect(status().isForbidden());verifyNoInteractions(service);}
}
