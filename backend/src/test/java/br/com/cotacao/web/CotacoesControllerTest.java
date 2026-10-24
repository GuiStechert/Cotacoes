package br.com.cotacao.web;
import br.com.cotacao.application.CotacoesService;
import static br.com.cotacao.application.Cotacoes.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.dao.CannotSerializeTransactionException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CotacoesControllerTest {
 private CotacoesService service;private MockMvc mvc;
 private final UUID id=UUID.randomUUID(),cliente=UUID.randomUUID(),produto=UUID.randomUUID();
 @BeforeEach void preparar(){service=mock(CotacoesService.class);mvc=MockMvcBuilders.standaloneSetup(new CotacoesController(service)).setControllerAdvice(new ErrosHttp()).setMessageConverters(new MappingJackson2HttpMessageConverter(JsonConfig.mapper())).build();}
 private String entrada(){return """
  {"clienteId":"%s","faturamento":"2026-01-01","vencimento":"2026-08-01","jurosAnual":0.12,"areaCliente":null,"itens":[{"produtoId":"%s","quantidade":2,"preco":100.001}]}
  """.formatted(cliente,produto);}
 @Test void criarRetorna201LocationEId() throws Exception {when(service.criar(any())).thenReturn(id);mvc.perform(post("/api/cotacoes").contentType("application/json").content(entrada())).andExpect(status().isCreated()).andExpect(header().string("Location","/api/cotacoes/"+id)).andExpect(jsonPath("$.id").value(id.toString()));verify(service).criar(new Entrada(cliente,LocalDate.of(2026,1,1),LocalDate.of(2026,8,1),new BigDecimal("0.12"),null,List.of(new ItemEntrada(produto,new BigDecimal("2"),new BigDecimal("100.001")))));}
 @Test void editarEEnviarRetornam204() throws Exception {mvc.perform(put("/api/cotacoes/"+id).contentType("application/json").content(entrada())).andExpect(status().isNoContent());mvc.perform(post("/api/cotacoes/"+id+"/enviar")).andExpect(status().isNoContent());verify(service).editar(eq(id),any());verify(service).enviar(id);}
 @Test void duplicarRetorna201() throws Exception {var novo=UUID.randomUUID();when(service.duplicar(id)).thenReturn(novo);mvc.perform(post("/api/cotacoes/"+id+"/duplicar")).andExpect(status().isCreated()).andExpect(header().string("Location","/api/cotacoes/"+novo)).andExpect(jsonPath("$.id").value(novo.toString()));verify(service).duplicar(id);}
 @Test void listagemPadraoEFiltrada() throws Exception {when(service.listar(any())).thenReturn(new Pagina(0,20,0,0,List.of()));mvc.perform(get("/api/cotacoes")).andExpect(status().isOk()).andExpect(jsonPath("$.tamanho").value(20)).andExpect(jsonPath("$.total").value(0));verify(service).listar(new Filtro(0,20,null,null,null,null));mvc.perform(get("/api/cotacoes").param("page","2").param("size","20").param("status","APROVADA").param("clienteId",cliente.toString()).param("inicio","2026-01-01").param("fim","2026-10-24")).andExpect(status().isOk());verify(service).listar(new Filtro(2,20,"APROVADA",cliente,LocalDate.of(2026,1,1),LocalDate.of(2026,10,24)));}
 @Test void consultaComercialSemCampoRestrito() throws Exception {when(service.consultar(id)).thenReturn(new Comercial(id,cliente,LocalDate.of(2026,1,1),LocalDate.of(2026,8,1),BigDecimal.ZERO,null,"RASCUNHO",null,BigDecimal.TEN,BigDecimal.ONE,List.of()));mvc.perform(get("/api/cotacoes/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString())).andExpect(jsonPath("$.custoReal").doesNotExist()).andExpect(jsonPath("$.margemRealPonderada").doesNotExist());}
 @ParameterizedTest @CsvSource({"NAO_AUTENTICADO,401","ACESSO_NEGADO,403","COTACAO_INEXISTENTE,404","ESTADO_INVALIDO,409","DADOS_INVALIDOS,400","CUSTO_AUSENTE,400","CLIENTE_INEXISTENTE,400","PRODUTO_INDISPONIVEL,400"}) void errosPublicos(String codigo,int esperado) throws Exception {when(service.consultar(id)).thenThrow(new Erro(codigo));mvc.perform(get("/api/cotacoes/"+id)).andExpect(status().is(esperado)).andExpect(jsonPath("$.codigo").value(codigo));}
 @Test void conflitoDeConcorrencia409() throws Exception {doThrow(new CannotSerializeTransactionException("SQL sensível")).when(service).enviar(id);mvc.perform(post("/api/cotacoes/"+id+"/enviar")).andExpect(status().isConflict()).andExpect(content().json("{\"codigo\":\"CONFLITO_CONCORRENCIA\"}"));}
 @Test void erroInternoSemDetalhes() throws Exception {when(service.consultar(id)).thenThrow(new IllegalStateException("senha e SQL não devem sair"));mvc.perform(get("/api/cotacoes/"+id)).andExpect(status().isInternalServerError()).andExpect(content().json("{\"codigo\":\"ERRO_INTERNO\"}"));}
 @Test void rejeitaUuidDatasPaginaEJsonInvalidos() throws Exception {mvc.perform(get("/api/cotacoes/abc")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));mvc.perform(get("/api/cotacoes").param("inicio","nao-data")).andExpect(status().isBadRequest());mvc.perform(get("/api/cotacoes").param("page","true")).andExpect(status().isBadRequest());mvc.perform(post("/api/cotacoes").contentType("application/json").content("{")).andExpect(status().isBadRequest());verifyNoInteractions(service);}
 @Test void rejeitaCustosPerfilECamposDesconhecidos() throws Exception {for(var campo:List.of("custoReal","margemReal","solicitanteId","perfil")){mvc.perform(post("/api/cotacoes").contentType("application/json").content(entrada().replaceFirst("\\{","{\""+campo+"\":\"injetado\","))).andExpect(status().isBadRequest());}verifyNoInteractions(service);}
 @Test void rejeitaTextoBooleanoEObjetosEmNumeros() throws Exception {for(var valor:List.of("\"2\"","true","{}","[]")){mvc.perform(post("/api/cotacoes").contentType("application/json").content(entrada().replace("\"quantidade\":2","\"quantidade\":"+valor))).andExpect(status().isBadRequest());}verifyNoInteractions(service);}
 @Test void corpoAusenteRejeitado() throws Exception {mvc.perform(post("/api/cotacoes").contentType("application/json")).andExpect(status().isBadRequest());verifyNoInteractions(service);}
}
