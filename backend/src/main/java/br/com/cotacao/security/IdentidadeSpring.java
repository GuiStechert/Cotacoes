package br.com.cotacao.security;
import br.com.cotacao.application.Identidade;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
public final class IdentidadeSpring implements Identidade {
 @Override public String contaAutenticada() {
  var auth=SecurityContextHolder.getContext().getAuthentication();
  if(auth==null||!auth.isAuthenticated()||auth instanceof AnonymousAuthenticationToken)return null;
  return auth.getName();
 }
}
