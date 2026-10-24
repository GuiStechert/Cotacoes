package br.com.cotacao.security;
import waffle.spring.NegotiateSecurityFilter;
import waffle.servlet.spi.SecurityFilterProviderCollection;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.session.*;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import waffle.windows.auth.PrincipalFormat;
import java.io.*;
import java.util.*;

public class FiltroWindows extends NegotiateSecurityFilter {
 private final SecurityContextRepository contextos;
 private final SessionAuthenticationStrategy sessoes;
 public FiltroWindows(SecurityFilterProviderCollection provedores,SecurityContextRepository contextos,CsrfTokenRepository tokens) {
  setProvider(provedores);
  this.contextos=Objects.requireNonNull(contextos);
  this.sessoes=new CompositeSessionAuthenticationStrategy(List.of(new ChangeSessionIdAuthenticationStrategy(),new CsrfAuthenticationStrategy(Objects.requireNonNull(tokens))));
  setAllowGuestLogin(false);setImpersonate(false);setPrincipalFormatEnum(PrincipalFormat.FQN);
  setRoleFormatEnum(PrincipalFormat.NONE);setDefaultGrantedAuthority(null);
 }
 @Override protected boolean setAuthentication(HttpServletRequest request,HttpServletResponse response,Authentication authentication) {
  var conta=authentication.getName();
  if(conta==null||conta.isBlank()){sendUnauthorized(response,true);return false;}
  var anterior=SecurityContextHolder.getContext().getAuthentication();
  var validada=UsernamePasswordAuthenticationToken.authenticated(conta,null,List.of());
  boolean mesmaConta=anterior!=null&&anterior.isAuthenticated()&&!(anterior instanceof AnonymousAuthenticationToken)&&conta.equalsIgnoreCase(anterior.getName());
  if(!mesmaConta)sessoes.onAuthentication(validada,request,response);
  var contexto=SecurityContextHolder.createEmptyContext();contexto.setAuthentication(validada);
  SecurityContextHolder.setContext(contexto);contextos.saveContext(contexto,request,response);
  return true;
 }
 @Override protected void sendUnauthorized(HttpServletResponse resposta,boolean fechar) {
  try{naoAutenticado(resposta,fechar);}catch(IOException erro){throw new UncheckedIOException(erro);}
 }
 public static void naoAutenticado(HttpServletResponse resposta,boolean fechar) throws IOException {
  resposta.addHeader("WWW-Authenticate","Negotiate");resposta.addHeader("WWW-Authenticate","NTLM");
  resposta.setHeader("Connection",fechar?"close":"keep-alive");resposta.setStatus(401);
  resposta.setContentType("application/json");resposta.setCharacterEncoding("UTF-8");
  resposta.getWriter().write("{\"codigo\":\"NAO_AUTENTICADO\"}");
 }
}
