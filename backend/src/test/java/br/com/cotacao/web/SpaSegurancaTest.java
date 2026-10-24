package br.com.cotacao.web;
import br.com.cotacao.CotacaoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(classes=CotacaoApplication.class,properties={"DATABASE_URL=postgresql://cotacao_test:@127.0.0.1:55432/postgres","spring.main.banner-mode=off","logging.level.root=WARN"})
@AutoConfigureMockMvc
class SpaSegurancaTest {
 @Autowired MockMvc mvc;
 @Test void shellPublicoNaoConcedeAcessoAosDados() throws Exception {
  mvc.perform(get("/cotacoes")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
  for(var rota:new String[]{"/api/cotacoes","/api/clientes","/api/produtos","/api/usuario","/api/aprovacoes/pendentes"})mvc.perform(get(rota).header("X-Usuario","DOM\\gerente")).andExpect(status().isUnauthorized());
 }
}
