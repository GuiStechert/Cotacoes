package br.com.cotacao.web;

import br.com.cotacao.application.CotacoesService;
import static br.com.cotacao.application.Cotacoes.*;
import org.junit.jupiter.api.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.Instant;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AprovacoesControllerTest {
 private CotacoesService service;private MockMvc mvc;
 private final UUID id=UUID.randomUUID(),item=UUID.randomUUID();
 @BeforeEach void preparar(){service=mock(CotacoesService.class);mvc=MockMvcBuilders.standaloneSetup(new CotacoesController(service),new AprovacoesController(service)).setControllerAdvice(new ErrosHttp()).setMessageConverters(new MappingJackson2HttpMessageConverter(JsonConfig.mapper())).build();}
 private String corpo(){return "{\"itens\":[{\"itemId\":\""+item+"\",\"aprovado\":false,\"comentario\":\"Revisar\"}]}";}
 @Test void confirmarRetorna204EDelegaUmaVez() throws Exception {mvc.perform(post("/api/cotacoes/"+id+"/decisoes").contentType("application/json").content(corpo())).andExpect(status().isNoContent());verify(service).decidir(id,new Decisoes(List.of(new DecisaoItem(item,false,"Revisar"))));}
 @Test void comentarioOmitidoEhOpcional() throws Exception {mvc.perform(post("/api/cotacoes/"+id+"/decisoes").contentType("application/json").content("{\"itens\":[{\"itemId\":\""+item+"\",\"aprovado\":true}]}" )).andExpect(status().isNoContent());verify(service).decidir(id,new Decisoes(List.of(new DecisaoItem(item,true,null))));}
 @Test void historicoRetornaEventosSemCustos() throws Exception {var usuario=UUID.randomUUID();when(service.historico(id)).thenReturn(List.of(new EventoAprovacao(UUID.randomUUID(),item,usuario,"Gerente",Instant.parse("2026-10-24T15:00:00Z"),"REPROVADO","Revisar")));mvc.perform(get("/api/cotacoes/"+id+"/historico")).andExpect(status().isOk()).andExpect(jsonPath("$[0].usuarioId").value(usuario.toString())).andExpect(jsonPath("$[0].criadoEm").value("2026-10-24T15:00:00Z")).andExpect(jsonPath("$[0].comentario").value("Revisar")).andExpect(jsonPath("$[0].custoReal").doesNotExist());verify(service).historico(id);}
 @Test void filaDelegaPaginaETamanho() throws Exception {when(service.pendentes(anyInt(),anyInt())).thenReturn(new Pagina(0,20,0,0,List.of()));mvc.perform(get("/api/aprovacoes/pendentes")).andExpect(status().isOk()).andExpect(jsonPath("$.tamanho").value(20));verify(service).pendentes(0,20);mvc.perform(get("/api/aprovacoes/pendentes").param("page","2").param("size","20")).andExpect(status().isOk());verify(service).pendentes(2,20);}
 @Test void errosDeConjuntoEstadoEPermissaoPreservados() throws Exception {doThrow(new Erro("DECISOES_INVALIDAS")).when(service).decidir(eq(id),any());mvc.perform(post("/api/cotacoes/"+id+"/decisoes").contentType("application/json").content(corpo())).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DECISOES_INVALIDAS"));when(service.historico(id)).thenThrow(new Erro("ACESSO_NEGADO"));mvc.perform(get("/api/cotacoes/"+id+"/historico")).andExpect(status().isForbidden());when(service.pendentes(0,20)).thenThrow(new Erro("ACESSO_NEGADO"));mvc.perform(get("/api/aprovacoes/pendentes")).andExpect(status().isForbidden());}
 @Test void rejeitaBooleanoTextoNumeroOuObjeto() throws Exception {for(var valor:List.of("\"false\"","0","{}","[]")){mvc.perform(post("/api/cotacoes/"+id+"/decisoes").contentType("application/json").content(corpo().replace("false",valor))).andExpect(status().isBadRequest());}verifyNoInteractions(service);}
 @Test void rejeitaComentarioNumericoEBooleano() throws Exception {for(var valor:List.of("123","true","{}","[]")){mvc.perform(post("/api/cotacoes/"+id+"/decisoes").contentType("application/json").content(corpo().replace("\"Revisar\"",valor))).andExpect(status().isBadRequest());}verifyNoInteractions(service);}
 @Test void rejeitaCamposInjetadosJsonAusenteUuidEPaginaInvalidos() throws Exception {mvc.perform(post("/api/cotacoes/"+id+"/decisoes").contentType("application/json").content(corpo().replace("\"aprovado\"","\"usuarioId\":\"forjado\",\"aprovado\""))).andExpect(status().isBadRequest());mvc.perform(post("/api/cotacoes/"+id+"/decisoes").contentType("application/json")).andExpect(status().isBadRequest());mvc.perform(get("/api/cotacoes/invalido/historico")).andExpect(status().isBadRequest());mvc.perform(get("/api/aprovacoes/pendentes").param("page","x")).andExpect(status().isBadRequest());verifyNoInteractions(service);}
}
