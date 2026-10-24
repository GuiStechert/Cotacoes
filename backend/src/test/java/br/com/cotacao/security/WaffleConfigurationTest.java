package br.com.cotacao.security;
import org.junit.jupiter.api.Test;
import waffle.windows.auth.IWindowsAuthProvider;
import waffle.servlet.spi.SecurityFilterProviderCollection;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class WaffleConfigurationTest {
 @Test void exigeWindowsSemAssumirDominio(){assertDoesNotThrow(()->WaffleConfiguration.exigirWindows("Windows 11"));for(var sistema:Arrays.asList(null,"Linux","Mac OS X","Darwin")){assertEquals("Autenticação Windows requer Windows",assertThrows(IllegalStateException.class,()->WaffleConfiguration.exigirWindows(sistema)).getMessage());}}
 @Test void providerSomenteNegotiateENTLM(){var nativo=mock(IWindowsAuthProvider.class);var providers=new WaffleConfiguration().provedoresWindows(nativo);assertTrue(providers.isSecurityPackageSupported("Negotiate"));assertTrue(providers.isSecurityPackageSupported("NTLM"));assertFalse(providers.isSecurityPackageSupported("Basic"));verifyNoInteractions(nativo);}
 @Test void filtroNaoEhRegistradoDuasVezes(){var filtro=new FiltroWindows(mock(SecurityFilterProviderCollection.class),new HttpSessionSecurityContextRepository(),new HttpSessionCsrfTokenRepository());assertFalse(new WaffleConfiguration().registroWindows(filtro).isEnabled());}
}
