package br.com.cotacao.web;
import br.com.cotacao.CotacaoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
@SpringBootTest(classes=CotacaoApplication.class,properties={"DATABASE_URL=postgresql://cotacao_test:@127.0.0.1:55432/postgres","spring.main.banner-mode=off","logging.level.root=WARN"})
@AutoConfigureMockMvc
class VueRecursosTest {
 @Autowired MockMvc mvc;
 @Test void templatesVueSaoPublicosSemLiberarApi() throws Exception {
  for(var nome:new String[]{"app","lista","cotacao","aprovacao"})mvc.perform(get("/"+nome+".html")).andExpect(status().isOk()).andExpect(content().string(containsString("v-if")));
  mvc.perform(get("/index.html")).andExpect(status().isOk()).andExpect(content().string(containsString("vue@3.5.22")));
  mvc.perform(get("/api/usuario")).andExpect(status().isUnauthorized());
 }
}
