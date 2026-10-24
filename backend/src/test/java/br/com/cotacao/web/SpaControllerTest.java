package br.com.cotacao.web;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class SpaControllerTest {
 @Test void tresTelasEEntradaDiretaUsamOMesmoIndex() throws Exception {
  var mvc=MockMvcBuilders.standaloneSetup(new SpaController()).setControllerAdvice(new ErrosHttp()).build();
  for(var rota:new String[]{"/","/cotacoes","/cotacao/nova","/cotacao/00000000-0000-0000-0000-000000000001","/aprovacao/00000000-0000-0000-0000-000000000001"})mvc.perform(get(rota)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
 }
}
