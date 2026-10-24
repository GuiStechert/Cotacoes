package br.com.cotacao.security;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.savedrequest.NullRequestCache;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
@Configuration
public class SegurancaConfiguration {
 @Bean public SecurityFilterChain filtros(HttpSecurity http) throws Exception {
  return http
   .httpBasic(AbstractHttpConfigurer::disable)
   .formLogin(AbstractHttpConfigurer::disable)
   .logout(AbstractHttpConfigurer::disable)
   .requestCache(c->c.requestCache(new NullRequestCache()))
   .sessionManagement(c->c.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
   .authorizeHttpRequests(a->a.requestMatchers("/api/csrf").permitAll().anyRequest().authenticated())
   .exceptionHandling(e->e
    .authenticationEntryPoint((request,response,ex)->falha(response,401,"NAO_AUTENTICADO"))
    .accessDeniedHandler((request,response,ex)->falha(response,403,"ACESSO_NEGADO")))
   .build();
 }
 private static void falha(HttpServletResponse resposta,int status,String codigo) throws IOException {
  resposta.setStatus(status);resposta.setContentType("application/json");resposta.setCharacterEncoding("UTF-8");
  resposta.getWriter().write("{\"codigo\":\""+codigo+"\"}");
 }
}
