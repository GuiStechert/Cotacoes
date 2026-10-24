package br.com.cotacao.security;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.savedrequest.NullRequestCache;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.beans.factory.ObjectProvider;
@Configuration
public class SegurancaConfiguration {
 @Bean public HttpSessionSecurityContextRepository contextosWindows(){return new HttpSessionSecurityContextRepository();}
 @Bean public HttpSessionCsrfTokenRepository tokensCsrf(){return new HttpSessionCsrfTokenRepository();}
 @Bean public SecurityFilterChain filtros(HttpSecurity http,ObjectProvider<FiltroWindows> windows,
   HttpSessionSecurityContextRepository contextos,HttpSessionCsrfTokenRepository tokens) throws Exception {
  var filtroWindows=windows.getIfAvailable();
  http
   .securityContext(c->c.securityContextRepository(contextos))
   .csrf(c->c.csrfTokenRepository(tokens))
   .httpBasic(AbstractHttpConfigurer::disable)
   .formLogin(AbstractHttpConfigurer::disable)
   .logout(AbstractHttpConfigurer::disable)
   .requestCache(c->c.requestCache(new NullRequestCache()))
   .sessionManagement(c->c.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
   .authorizeHttpRequests(a->a.requestMatchers("/api/csrf").permitAll().anyRequest().authenticated())
   .exceptionHandling(e->e
    .authenticationEntryPoint((request,response,ex)->{
     if(filtroWindows!=null)FiltroWindows.naoAutenticado(response,false);
     else falha(response,401,"NAO_AUTENTICADO");
    })
    .accessDeniedHandler((request,response,ex)->falha(response,403,"ACESSO_NEGADO")));
  if(filtroWindows!=null)http.addFilterBefore(filtroWindows,CsrfFilter.class);
  return http.build();
 }
 private static void falha(HttpServletResponse resposta,int status,String codigo) throws IOException {
  resposta.setStatus(status);resposta.setContentType("application/json");resposta.setCharacterEncoding("UTF-8");
  resposta.getWriter().write("{\"codigo\":\""+codigo+"\"}");
 }
}
