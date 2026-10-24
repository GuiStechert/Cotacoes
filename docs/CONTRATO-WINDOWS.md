# Integração Waffle/NTLM

Waffle `waffle-spring-security6:3.7.0`, artefato oficial para Spring Security 6/Jakarta. Versão conferida no Maven Central e fontes oficiais inspecionados. Referências: [Waffle](https://github.com/Waffle/waffle), [filtro Spring](https://github.com/Waffle/waffle/blob/master/Docs/spring/SpringSecuritySingleSignOnFilter.md).

Ativação explícita: `cotacao.auth.windows.enabled=true` (ambiente: `COTACAO_AUTH_WINDOWS_ENABLED=true`). O padrão continua desativado, preservando execução local sem login e testes atuais. Quando ativado, o provider nativo exige Windows e o servidor anuncia somente Negotiate e NTLM. Basic, usuário convidado e impersonação do processo permanecem desabilitados. Não são usados cabeçalhos X-Usuario/X-Perfil ou grupos Windows para conceder perfis de negócio.

O filtro usa o Waffle real para validar os tokens e obter a conta FQN. Apenas após validação grava uma identidade simples no SecurityContext e na sessão Spring, sem credencial, handle nativo ou autoridades de grupos. O perfil e o cadastro ativo continuam sendo resolvidos no PostgreSQL por conta_windows. Usuário Windows válido sem cadastro ativo recebe 403 nos endpoints da aplicação.

O filtro executa uma única vez, dentro da cadeia Spring, antes do CSRF. Login inicial ou troca de conta muda o ID da sessão e invalida o token CSRF anterior; nova autenticação da mesma conta mantém o token. O cliente deve buscar GET /api/csrf depois da autenticação. A persistência explícita do contexto permite requisições posteriores na mesma sessão sem novo cabeçalho de negociação. CSRF continua obrigatório para escritas.

Sem cabeçalho/token válido: 401 com desafios Negotiate/NTLM. Tokens inválidos, conta vazia ou convidado: 401, sem contexto autenticado novo. O handshake incompleto continua sob responsabilidade do provider Waffle e não concede identidade. Recursos nativos são liberados pelo filtro Waffle mesmo em falha. O filtro não é registrado também no container Servlet, evitando execução duplicada.

Testes de filtro usam Waffle real e simulam somente SecurityFilterProviderCollection (fronteira externa de validação), com IWindowsIdentity representando seu resultado. Não se mockam simultaneamente as chamadas nativas internas desse provider. Testes de configuração de protocolos usam IWindowsAuthProvider mockado e coleção real. Testes integrados usam catálogo/banco reais e provider Waffle simulado, exclusivamente no código de teste.

Limite da validação: este computador foi inspecionado e não está ingressado em domínio (PartOfDomain=false). A integração com AD, políticas de navegador/intranet, DNS e TLS precisa ser homologada no ambiente corporativo. Não alterar registro do Windows nem relaxar políticas de loopback/browser para contornar falhas locais. Nenhuma validação simulada equivale a homologação corporativa.

Smoke nativo realizado: JAR com SSO ativado iniciou no Windows com provider real; desafio inicial, token inválido e Basic/cabeçalho forjado resultaram em 401. Não foram usadas credenciais de usuários nem o banco da aplicação. Login positivo com AD permanece não homologado.
