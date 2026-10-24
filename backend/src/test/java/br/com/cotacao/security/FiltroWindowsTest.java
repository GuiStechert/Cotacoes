package br.com.cotacao.security;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.*;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import waffle.servlet.spi.SecurityFilterProviderCollection;
import waffle.windows.auth.*;
import jakarta.servlet.FilterChain;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FiltroWindowsTest {
 private SecurityFilterProviderCollection provider;
 private HttpSessionSecurityContextRepository contextos;
 private HttpSessionCsrfTokenRepository tokens;
 private FiltroWindows filtro;
 private MockHttpServletRequest request;private MockHttpServletResponse response;private FilterChain chain;
 @BeforeEach void preparar(){
  SecurityContextHolder.clearContext();provider=mock(SecurityFilterProviderCollection.class);
  contextos=new HttpSessionSecurityContextRepository();tokens=new HttpSessionCsrfTokenRepository();filtro=new FiltroWindows(provider,contextos,tokens);
  request=new MockHttpServletRequest("GET","/api/usuario");response=new MockHttpServletResponse();chain=mock(FilterChain.class);
  request.addHeader("Authorization","Negotiate dGVzdGU=");when(provider.isSecurityPackageSupported("Negotiate")).thenReturn(true);
 }
 @AfterEach void limpar(){SecurityContextHolder.clearContext();}
 private IWindowsIdentity identidade(String conta) throws Exception {
  var identity=mock(IWindowsIdentity.class);when(identity.getFqn()).thenReturn(conta);when(identity.getSid()).thenReturn(new byte[]{1});when(identity.getSidString()).thenReturn("S-1-5-21-1");when(identity.getGroups()).thenReturn(new IWindowsAccount[0]);
  when(provider.doFilter(request,response)).thenReturn(identity);return identity;
 }
 @Test void contaValidadaPersisteSemCredenciaisOuGruposNativos() throws Exception {
  var identity=identidade("DOM\\comercial");filtro.doFilter(request,response,chain);
  var auth=SecurityContextHolder.getContext().getAuthentication();assertNotNull(auth);assertTrue(auth.isAuthenticated());assertEquals("DOM\\comercial",auth.getName());assertInstanceOf(String.class,auth.getPrincipal());assertNull(auth.getCredentials());assertTrue(auth.getAuthorities().isEmpty());
  var salvo=(SecurityContext)request.getSession(false).getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);assertEquals(auth,salvo.getAuthentication());verify(identity).dispose();verify(chain).doFilter(request,response);
 }
 @Test void primeiroLoginMudaSessaoEInvalidaCsrfAnterior() throws Exception {
  identidade("DOM\\comercial");var sessao=request.getSession();var antes=sessao.getId();tokens.saveToken(tokens.generateToken(request),request,response);
  filtro.doFilter(request,response,chain);assertNotEquals(antes,request.getSession().getId());assertNull(tokens.loadToken(request));
 }
 @Test void mesmaContaNaoMudaSessaoNemInvalidaCsrfEmCadaRequisicao() throws Exception {
  var anterior=UsernamePasswordAuthenticationToken.authenticated("dom\\comercial",null,List.of());var contexto=new SecurityContextImpl(anterior);SecurityContextHolder.setContext(contexto);contextos.saveContext(contexto,request,response);
  var token=tokens.generateToken(request);tokens.saveToken(token,request,response);var sessao=request.getSession().getId();identidade("DOM\\COMERCIAL");
  filtro.doFilter(request,response,chain);assertEquals(sessao,request.getSession().getId());assertEquals(token.getToken(),tokens.loadToken(request).getToken());assertEquals("DOM\\COMERCIAL",SecurityContextHolder.getContext().getAuthentication().getName());
 }
 @Test void trocaDeContaRotacionaSessaoEToken() throws Exception {
  var contexto=new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated("DOM\\outro",null,List.of()));SecurityContextHolder.setContext(contexto);contextos.saveContext(contexto,request,response);
  var antes=request.getSession().getId();tokens.saveToken(tokens.generateToken(request),request,response);identidade("DOM\\comercial");filtro.doFilter(request,response,chain);
  assertNotEquals(antes,request.getSession().getId());assertNull(tokens.loadToken(request));assertEquals("DOM\\comercial",SecurityContextHolder.getContext().getAuthentication().getName());
 }
 @Test void convidadoNegadoELiberado() throws Exception {
  var identity=identidade("DOM\\Guest");when(identity.isGuest()).thenReturn(true);filtro.doFilter(request,response,chain);
  assertEquals(401,response.getStatus());assertEquals("{\"codigo\":\"NAO_AUTENTICADO\"}",response.getContentAsString());assertNull(SecurityContextHolder.getContext().getAuthentication());verifyNoInteractions(chain);verify(identity).dispose();
 }
 @Test void falhaDeValidacaoNaoRefleteMensagemDoProvider() throws Exception {
  when(provider.doFilter(request,response)).thenThrow(new IOException("detalhe sensível do provider"));filtro.doFilter(request,response,chain);
  assertEquals(401,response.getStatus());assertEquals(List.of("Negotiate","NTLM"),new ArrayList<>(response.getHeaders("WWW-Authenticate")));assertEquals("{\"codigo\":\"NAO_AUTENTICADO\"}",response.getContentAsString());assertNull(SecurityContextHolder.getContext().getAuthentication());verifyNoInteractions(chain);
 }
 @Test void handshakeIncompletoNaoCriaSessaoOuIdentidade() throws Exception {
  when(provider.doFilter(request,response)).thenAnswer(i->{response.setStatus(401);response.addHeader("WWW-Authenticate","Negotiate passo2");return null;});
  filtro.doFilter(request,response,chain);assertEquals(401,response.getStatus());assertNull(SecurityContextHolder.getContext().getAuthentication());assertNull(request.getSession(false));verifyNoInteractions(chain);
 }
 @Test void cabecalhoArbitrarioEBasicNaoSaoIdentidade() throws Exception {
  request.removeHeader("Authorization");request.addHeader("X-Usuario","DOM\\gerente");request.addHeader("Authorization","Basic ZmFsc286ZmFsc28=");filtro.doFilter(request,response,chain);
  assertNull(SecurityContextHolder.getContext().getAuthentication());verify(provider,never()).doFilter(any(),any());verify(chain).doFilter(request,response);
 }
 @Test void contaVaziaNaoEhPersistida() throws Exception {identidade(" ");filtro.doFilter(request,response,chain);assertEquals(401,response.getStatus());assertNull(SecurityContextHolder.getContext().getAuthentication());verifyNoInteractions(chain);}
 @Test void politicaDesabilitaConvidadoEImpersonacao(){assertFalse(filtro.isAllowGuestLogin());assertFalse(filtro.isImpersonate());}
 @Test void gruposWindowsNaoViramPerfisDeNegocio() throws Exception {
  var identity=identidade("DOM\\comercial");var grupo=mock(IWindowsAccount.class);
  when(grupo.getFqn()).thenReturn("DOM\\GERENTE");when(grupo.getSidString()).thenReturn("S-1-5-32-1");when(identity.getGroups()).thenReturn(new IWindowsAccount[]{grupo});
  filtro.doFilter(request,response,chain);assertNotNull(SecurityContextHolder.getContext().getAuthentication());assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().isEmpty());
 }
}
