# Homologação da autenticação Windows

## Situação atual

A aplicação usa Waffle/SSPI com Negotiate/NTLM. A identidade validada pelo Windows é associada a `usuarios.conta_windows`; o cadastro ativo no PostgreSQL define o perfil de negócio. Não existe autenticação LDAP direta, formulário de senha ou escolha de perfil na interface.

Este computador está fora do domínio e sem acesso à rede corporativa. Ele permite testar a aplicação e o carregamento do provider nativo, mas ainda não comprova um login de domínio. Estar fora do domínio, isoladamente, não determina o resultado de todos os cenários Windows; conectividade, protocolo e políticas também participam da autenticação.

| Verificação | Ambiente disponível | Evidência exigida |
|---|---|---|
| Interface com SSO ativado e desafio inicial | Windows local | Página 200; API anônima 401 com Negotiate/NTLM |
| Rejeição de token inválido, Basic e identidade forjada | Windows local | 401; nenhum acesso por X-Usuario/X-Perfil |
| Sessão, CSRF, autorização e perfis | Testes isolados com fronteira Windows simulada | Suíte de segurança aprovada |
| Login Windows positivo com conta local | Ainda não executado | Identidade validada pelo provider real; não comprova AD |
| Login de domínio e comportamento do navegador | Ambiente corporativo ou laboratório AD | Identidade real, protocolo negociado e resultados funcionais |
| Aceite das políticas da empresa | Ambiente corporativo | Aprovação da TI e responsáveis pelos perfis |

## Preparação com a TI

1. Disponibilizar uma máquina Windows de homologação com acesso ao domínio, DNS e horário corretos. Para acesso remoto, usar o meio autorizado pela empresa. Uma VPN isoladamente não garante SSO.
2. Definir o nome de acesso da aplicação, HTTPS e navegador suportado. A TI deve verificar as políticas de autenticação integrada e, quando aplicável, a identidade do serviço e os SPNs necessários ao Kerberos. Registrar o protocolo efetivamente negociado; anunciar Negotiate não comprova que Kerberos foi utilizado.
3. Preparar PostgreSQL e cadastros exclusivos de homologação. Não executar o roteiro de criação/envio/decisão contra dados operacionais. A migração V1 já aplicada não deve ser reaplicada sobre o mesmo schema.
4. Preparar contas de teste para COMERCIAL, GESTOR, SUPERVISOR e GERENTE, além de uma conta Windows válida sem cadastro e uma com cadastro inativo. Confirmar o FQN retornado pelo Windows antes de associá-lo ao cadastro; não usar grupos AD como alçadas.
5. Registrar versão/commit do pacote e configuração do ambiente, preservando senhas, cookies, tokens de autenticação e CSRF fora das evidências compartilhadas.

O servidor escuta em `127.0.0.1` por padrão. O acesso por outra máquina exige configuração de publicação pela TI, incluindo endereço de escuta, HTTPS e encaminhamento compatível com autenticação Windows. Apenas acrescentar `-WindowsSso` não publica o serviço na rede.

## Execução

Construir e conferir o pacote conforme `docs/EXECUCAO.md`. Na máquina escolhida, configurar `DATABASE_URL` para a base de homologação pelo ambiente ou `.env` local e iniciar:

```powershell
./scripts/iniciar-backend.ps1 -WindowsSso
```

Usar o nome e o endereço de homologação definidos pela TI. Cada conta deve ser testada em sua sessão Windows/navegador correspondente; evitar reutilizar uma sessão autenticada de outra pessoa. O sistema não oferece troca de identidade por parâmetros ou cabeçalhos.

| Caso | Procedimento | Resultado esperado |
|---|---|---|
| Sem identidade válida | Consultar `/api/usuario` sem completar autenticação | 401; desafios Negotiate/NTLM, sem Basic |
| Conta cadastrada e ativa | Abrir a aplicação com cada conta de teste | `/api/usuario` 200 com nome e perfil do cadastro correto |
| Conta não cadastrada ou inativa | Acessar com cada uma das duas contas | 403; nenhum dado de negócio disponível |
| Identidade forjada | Enviar X-Usuario/X-Perfil sem autenticação válida | 401; cabeçalhos não concedem acesso |
| Comercial | Consultar produtos, lista e uma cotação própria | Sem `custoReal`/`margemReal` nas respostas; sem ações de aprovação |
| Aprovadores | Abrir a fila e uma cotação atribuída ao perfil | Margem real disponível; acesso negado à decisão de outra alçada |
| Roteamento | Enviar cotações de teste com itens nas três faixas | Gestor >= 10%; Supervisor >= 8% e < 10%; Gerente < 8%, usando a razão precisa e a maior alçada dos itens |
| Confirmação | Desmarcar um item e abrir a revisão, sem confirmar | Nenhuma decisão persistida; comentário opcional apenas no item desmarcado |
| Decisão mista | Confirmar aprovação e reprovação na mesma cotação | APROVADA_PARCIAL, histórico com autor e decisões persistidas |
| CSRF | Repetir uma escrita autorizada sem token e depois com token da mesma sessão | Sem token: 403; com token válido e dados válidos: sucesso |
| Concorrência | Confirmar a mesma cotação em duas sessões | Uma decisão efetiva; a outra retorna conflito, sem duplicar histórico |
| Reabertura | Recarregar uma rota de detalhe diretamente | Interface carrega e consulta somente os dados autorizados |

Não incluir tokens, cookies ou senhas em capturas ou chamados. Para comprovar Kerberos/NTLM, usar diagnóstico de navegador/Windows aprovado pela TI, com tratamento dos dados de autenticação.

## Diagnóstico e aceite

- **401:** verificar negociação Windows, políticas do navegador e conectividade; não cadastrar um perfil para tentar corrigir um handshake que não ocorreu.
- **403 após identidade validada:** conferir cadastro ativo e correspondência de `conta_windows`; em escritas, conferir também CSRF e autorização da ação.
- **Falha de infraestrutura:** conferir logs e disponibilidade do banco/DNS/serviço. Uma resposta 500 não comprova login funcional.

Não desabilitar proteção de loopback, validação de certificados, CSRF ou políticas corporativas para obter um resultado positivo. Se o ambiente bloquear NTLM, validar o caminho Kerberos permitido pela TI; não relaxar a política para aprovar o teste.

Registrar o aceite com: commit, data, ambiente, navegador, protocolo negociado, casos executados e resultados, responsável da TI e pendências. As evidências locais abaixo não substituem esse aceite.

Referências: [autenticação integrada Windows](https://learn.microsoft.com/en-us/aspnet/web-api/overview/security/integrated-windows-authentication), [Kerberos](https://learn.microsoft.com/en-us/windows-server/security/kerberos/kerberos-authentication-overview), [NTLM](https://learn.microsoft.com/en-us/windows-server/security/kerberos/ntlm-overview).
