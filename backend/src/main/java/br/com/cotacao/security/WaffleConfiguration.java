package br.com.cotacao.security;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import waffle.windows.auth.IWindowsAuthProvider;
import waffle.windows.auth.impl.WindowsAuthProviderImpl;
import waffle.servlet.spi.SecurityFilterProviderCollection;
import waffle.servlet.spi.SecurityFilterProvider;
import waffle.servlet.spi.NegotiateSecurityFilterProvider;
import java.util.*;

@Configuration
@ConditionalOnProperty(name="cotacao.auth.windows.enabled",havingValue="true")
public class WaffleConfiguration {
 public static void exigirWindows(String sistema) {
  if(sistema==null||!sistema.toLowerCase(Locale.ROOT).startsWith("windows"))throw new IllegalStateException("Autenticação Windows requer Windows");
 }
 @Bean @Lazy public IWindowsAuthProvider autenticadorWindows() {
  exigirWindows(System.getProperty("os.name"));return new WindowsAuthProviderImpl();
 }
 @Bean public SecurityFilterProviderCollection provedoresWindows(IWindowsAuthProvider autenticadorWindows) {
  var negociar=new NegotiateSecurityFilterProvider(autenticadorWindows);
  negociar.setProtocols(List.of("Negotiate","NTLM"));
  return new SecurityFilterProviderCollection(new SecurityFilterProvider[]{negociar});
 }
 @Bean public FiltroWindows filtroWindows(SecurityFilterProviderCollection provedores,SecurityContextRepository contextos,CsrfTokenRepository tokens) { return new FiltroWindows(provedores,contextos,tokens); }
 @Bean public FilterRegistrationBean<FiltroWindows> registroWindows(FiltroWindows filtro) {
  var registro=new FilterRegistrationBean<>(filtro);registro.setEnabled(false);return registro;
 }
}
