package br.com.cotacao.web;
import br.com.cotacao.application.CatalogoService;
import static br.com.cotacao.application.Catalogos.*;
import br.com.cotacao.application.Cotacoes.Erro;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class CatalogosControllerTest {
 private CatalogoService service;private MockMvc mvc;
 @BeforeEach void preparar(){service=mock(CatalogoService.class);mvc=MockMvcBuilders.standaloneSetup(new CatalogosController(service)).setControllerAdvice(new ErrosHttp()).setMessageConverters(new MappingJackson2HttpMessageConverter(JsonConfig.mapper())).build();}
 @Test void clientesDelegaAoServico() throws Exception {var id=UUID.randomUUID();when(service.clientes()).thenReturn(List.of(new Cliente(id,"Cliente")));mvc.perform(get("/api/clientes")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id.toString())).andExpect(jsonPath("$[0].nome").value("Cliente"));verify(service).clientes();}
 @Test void produtosDelegaListaVazia() throws Exception {when(service.produtos()).thenReturn(List.of());mvc.perform(get("/api/produtos")).andExpect(status().isOk()).andExpect(content().json("[]"));verify(service).produtos();}
 @Test void usuarioRetornaPerfilSomenteDoServico() throws Exception {var id=UUID.randomUUID();when(service.usuarioAtual()).thenReturn(new UsuarioAtual(id,"Comercial","COMERCIAL"));mvc.perform(get("/api/usuario").header("X-Usuario","forjado").param("perfil","GERENTE")).andExpect(status().isOk()).andExpect(jsonPath("$.perfil").value("COMERCIAL"));verify(service).usuarioAtual();}
 @Test void errosDeAcessoSaoPreservados() throws Exception {when(service.clientes()).thenThrow(new Erro("NAO_AUTENTICADO"));when(service.produtos()).thenThrow(new Erro("ACESSO_NEGADO"));mvc.perform(get("/api/clientes")).andExpect(status().isUnauthorized());mvc.perform(get("/api/produtos")).andExpect(status().isForbidden());}
}
