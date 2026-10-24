package br.com.cotacao.web;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
@RestController
public class CsrfController {
 public record Token(String token,String headerName,String parameterName) {}
 @GetMapping("/api/csrf") public Token token(CsrfToken token) { return new Token(token.getToken(),token.getHeaderName(),token.getParameterName()); }
}
