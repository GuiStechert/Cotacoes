package br.com.cotacao.web;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.dao.ConcurrencyFailureException;
import br.com.cotacao.application.Cotacoes.Erro;
@RestControllerAdvice
public class ErrosHttp {
 public record Falha(String codigo) {}
 @ExceptionHandler(Exception.class)
 public ResponseEntity<Falha> tratar(Exception erro) {
  if(erro instanceof Erro dominio) {
   var status=switch(dominio.codigo()) {
    case "NAO_AUTENTICADO" -> HttpStatus.UNAUTHORIZED;
    case "ACESSO_NEGADO" -> HttpStatus.FORBIDDEN;
    case "COTACAO_INEXISTENTE" -> HttpStatus.NOT_FOUND;
    case "ESTADO_INVALIDO" -> HttpStatus.CONFLICT;
    default -> HttpStatus.BAD_REQUEST;
   };
   return ResponseEntity.status(status).body(new Falha(dominio.codigo()));
  }
  if(erro instanceof ConcurrencyFailureException)return ResponseEntity.status(409).body(new Falha("CONFLITO_CONCORRENCIA"));
  if(erro instanceof HttpMessageNotReadableException||erro instanceof MethodArgumentTypeMismatchException||erro instanceof MissingServletRequestParameterException)
   return ResponseEntity.badRequest().body(new Falha("DADOS_INVALIDOS"));
  return ResponseEntity.internalServerError().body(new Falha("ERRO_INTERNO"));
 }
}
