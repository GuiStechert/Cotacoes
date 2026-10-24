package br.com.cotacao.web;
import br.com.cotacao.security.IdentidadeSpring;
import org.junit.jupiter.api.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class IdentidadeSpringTest {
 @AfterEach void limpar(){SecurityContextHolder.clearContext();}
 @Test void contextoVazioNaoAutentica(){assertNull(new IdentidadeSpring().contaAutenticada());}
 @Test void tokenNaoValidadoNaoAutentica(){SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.unauthenticated("DOM\\falso",""));assertNull(new IdentidadeSpring().contaAutenticada());}
 @Test void anonimoNaoAutentica(){SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("chave","anonymousUser",List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));assertNull(new IdentidadeSpring().contaAutenticada());}
 @Test void contaValidadaEhFornecida(){SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("DOM\\comercial","",List.of()));assertEquals("DOM\\comercial",new IdentidadeSpring().contaAutenticada());}
}
