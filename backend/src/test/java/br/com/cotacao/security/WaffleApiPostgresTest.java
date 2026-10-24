package br.com.cotacao.security;

import br.com.cotacao.CotacaoApplication;
import br.com.cotacao.support.CenarioBanco;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import jakarta.servlet.http.*;
import waffle.servlet.spi.SecurityFilterProviderCollection;
import waffle.windows.auth.*;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=CotacaoApplication.class,properties={"cotacao.auth.windows.enabled=true","spring.main.banner-mode=off","logging.level.root=WARN"})
@AutoConfigureMockMvc
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class WaffleApiPostgresTest {
 private static CenarioBanco banco;
 @Autowired MockMvc mvc;@Autowired ObjectMapper mapper;
 @MockitoBean SecurityFilterProviderCollection provedoresWindows;
 @DynamicPropertySource static void configurar(DynamicPropertyRegistry props) throws Exception {banco=new CenarioBanco();props.add("DATABASE_URL",()->"postgresql://cotacao_test:@127.0.0.1:55432/postgres?currentSchema="+banco.schema());}
 @AfterAll static void limpar(){if(banco!=null)banco.close();}
 @BeforeEach void protocolos(){when(provedoresWindows.isSecurityPackageSupported("Negotiate")).thenReturn(true);when(provedoresWindows.isSecurityPackageSupported("NTLM")).thenReturn(true);}
 private IWindowsIdentity validar(String conta) throws Exception {
  var identity=mock(IWindowsIdentity.class);when(identity.getFqn()).thenReturn(conta);when(identity.getSid()).thenReturn(new byte[]{1});when(identity.getSidString()).thenReturn("S-1-5-21-1");when(identity.getGroups()).thenReturn(new IWindowsAccount[0]);
  doReturn(identity).when(provedoresWindows).doFilter(argThat(r->r!=null&&"Negotiate dGVzdGU=".equals(r.getHeader("Authorization"))),any(HttpServletResponse.class));return identity;
 }
 @Test void anonimoRecebeSomenteDesafiosNegotiateENTLM() throws Exception {
  mvc.perform(get("/api/usuario")).andExpect(status().isUnauthorized()).andExpect(header().stringValues("WWW-Authenticate","Negotiate","NTLM")).andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));verify(provedoresWindows,never()).doFilter(any(),any());
 }
 @Test void contaValidadaPersisteSessaoEPerfilVemDoBanco() throws Exception {
  var identity=validar("DOM\\comercial");var resposta=mvc.perform(get("/api/usuario").header("Authorization","Negotiate dGVzdGU=").header("X-Perfil","GERENTE")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(banco.comercial.toString())).andExpect(jsonPath("$.perfil").value("COMERCIAL")).andReturn();
  var sessao=(MockHttpSession)resposta.getRequest().getSession(false);assertNotNull(sessao);
  mvc.perform(get("/api/usuario").session(sessao)).andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("comercial"));verify(provedoresWindows,times(1)).doFilter(any(),any());verify(identity).dispose();
 }
 @Test void comercialConsultaCatalogosSemCustoReal() throws Exception {
  validar("DOM\\comercial");mvc.perform(get("/api/produtos").header("Authorization","Negotiate dGVzdGU=")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(banco.produto.toString())).andExpect(jsonPath("$[0].custoReal").doesNotExist());
  mvc.perform(get("/api/clientes").header("Authorization","Negotiate dGVzdGU=")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(banco.cliente.toString()));
 }
 @Test void aprovadorRecebeUltimoCustoRealPeloServicoReal() throws Exception {
  banco.db.update("INSERT INTO produto_custos_reais(id,produto_id,custo_real) VALUES(?,?,8)",UUID.randomUUID(),banco.produto);
  validar("DOM\\gerente");mvc.perform(get("/api/produtos").header("Authorization","Negotiate dGVzdGU=")).andExpect(status().isOk()).andExpect(jsonPath("$[0].custoReal").value(8));
 }
 @Test void windowsValidoSemCadastroOuInativoRecebe403() throws Exception {
  validar("DOM\\desconhecido");mvc.perform(get("/api/usuario").header("Authorization","Negotiate dGVzdGU=")).andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
  banco.db.update("INSERT INTO usuarios(id,email,conta_windows,nome,perfil,ativo) VALUES(?,'inativo@empresa','DOM\\inativo','Inativo','GERENTE',false)",UUID.randomUUID());
  validar("DOM\\inativo");mvc.perform(get("/api/produtos").header("Authorization","Negotiate dGVzdGU=")).andExpect(status().isForbidden());
 }
 @Test void repeticaoDeNegotiateNaoQuebraCsrfEEscritaExigeToken() throws Exception {
  validar("DOM\\comercial");var sessao=new MockHttpSession();
  var token=mapper.readTree(mvc.perform(get("/api/csrf").session(sessao).header("Authorization","Negotiate dGVzdGU=")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  String corpo="{\"clienteId\":\""+banco.cliente+"\",\"faturamento\":\"2026-01-01\",\"vencimento\":\"2026-08-01\",\"jurosAnual\":0.12,\"itens\":[{\"produtoId\":\""+banco.produto+"\",\"quantidade\":1,\"preco\":100}]}";
  mvc.perform(post("/api/cotacoes").session(sessao).header("Authorization","Negotiate dGVzdGU=").contentType("application/json").content(corpo)).andExpect(status().isForbidden());
  mvc.perform(post("/api/cotacoes").session(sessao).header("Authorization","Negotiate dGVzdGU=").header(token.get("headerName").asText(),token.get("token").asText()).contentType("application/json").content(corpo)).andExpect(status().isCreated());
 }
 @Test void cabecalhosForjadosEBasicContinuamSemAcesso() throws Exception {
  mvc.perform(get("/api/usuario").header("X-Usuario","DOM\\gerente").header("X-Perfil","GERENTE")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/usuario").header("Authorization","Basic ZmFsc286ZmFsc28=")).andExpect(status().isUnauthorized());verify(provedoresWindows,never()).doFilter(any(),any());
 }
 @Test void falhaNativaOuConvidadoNaoConcedemIdentidade() throws Exception {
  when(provedoresWindows.doFilter(any(),any())).thenThrow(new IOException("falha de teste"));mvc.perform(get("/api/usuario").header("Authorization","Negotiate dGVzdGU=")).andExpect(status().isUnauthorized());
  var convidado=validar("DOM\\Guest");when(convidado.isGuest()).thenReturn(true);mvc.perform(get("/api/usuario").header("Authorization","Negotiate dGVzdGU=")).andExpect(status().isUnauthorized());verify(convidado).dispose();
 }
}
