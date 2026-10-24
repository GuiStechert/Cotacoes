# Decisões e andamento

- Documentação original copiada para a raiz; `.env` é local e ignorado pelo Git.
- Confirmação do usuário: arredondar para baixo somente o percentual exibido das margens; a alçada utiliza a razão precisa.
- Primeira entrega Java: núcleo financeiro isolado, BigDecimal com DECIMAL128, testes JUnit 5. Sem banco, relógio ou serviços externos no cálculo; não são necessários mocks nesse núcleo.
- Próxima etapa escolhida: tabelas PostgreSQL antes do Angular. Históricos independentes, snapshots no envio, autorização e transações precisam sustentar a API usada pelas três telas.
- O arquivo `.env` contém DATABASE_URL; conexão validada com SELECT 1. A migração V1 foi testada em PostgreSQL 18 isolado e aplicada em transação ao schema próprio `cotacao`, com checksum SHA256 registrado em `schema_migrations`. Nenhuma tabela preexistente foi alterada.
- Implementados: aplicação Spring executável, API de cotações/decisões/auditoria/fila e consultas de clientes/produtos/usuário; JDBC e transações, autorização pelo cadastro ativo no PostgreSQL, Spring Security, CSRF e integração Waffle/NTLM com ativação explícita. Ainda pendentes: homologação corporativa de SSO, integração/atualização dos cadastros e frontend. O sistema ainda não representa o MVP completo.
- Antes da sugestão de quantidade: esclarecer unidade da dose e precisão por produto, conforme definições futuras do README.

## Execução

JDK 25 e Maven 3.9.11. Maven local em `.tools/apache-maven-3.9.11` (ignorado).

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.4.1'
.tools/apache-maven-3.9.11/bin/mvn.cmd -f backend/pom.xml test
```

O pom prepara as dependências Spring Web, Data JPA, Security e PostgreSQL. A aplicação HTTP ainda não foi implementada: não há autenticação provisória ou endpoints expostos.

## Evidência de testes — 24/10/2026

Comando executado com repositório Maven local (relativo a backend):

```powershell
.tools/apache-maven-3.9.11/bin/mvn.cmd -f backend/pom.xml -Dmaven.repo.local=.tools/repository test
```

1. RED: 15 testes executados; 2 falhas e 13 erros causados pelos contratos ainda sem implementação (`UnsupportedOperationException`). Nenhum erro de compilação/configuração nessa execução. O primeiro download exigiu acesso de rede autorizado.
2. GREEN: mesmo conjunto de testes, sem alteração; 15 executados, zero falhas, zero erros, zero ignorados; BUILD SUCCESS. Execução offline após resolução das dependências.
3. Logs locais: `docs/red.log` e `docs/green.log` (ignorados no Git); relatórios em `backend/target/surefire-reports`.

Cobertura entregue: meses inclusivos e virada de ano, datas inválidas, juros simples, preço CEILING antes do cálculo, seis fronteiras de alçada, percentual FLOOR apenas para apresentação, valores nulos/negativos aplicáveis. Médias ponderadas, consolidação de estados, permissões e persistência pertencem às próximas funcionalidades e ainda não estão implementadas.

## PostgreSQL — segunda entrega, 24/10/2026

- Contrato em `docs/CONTRATO-PERSISTENCIA.md`; migração em `backend/src/main/resources/db/migration/V1__estrutura_inicial.sql`.
- RED confirmado: 5 testes de persistência falharam pela estrutura ausente (2 falhas e 3 erros); os 15 testes financeiros permaneceram verdes. Falhas anteriores de permissão/conexão foram resolvidas antes de contabilizar o RED.
- GREEN: 20 testes, zero falhas, zero erros, zero ignorados. Os 5 testes de persistência exercitam PostgreSQL real, com schema exclusivo por teste e limpeza posterior.
- Cobertos: oito tabelas, históricos comerciais/reais independentes, desempate de custo por UUID, bloqueio de UPDATE/DELETE em históricos, unicidade de email sem distinção de caixa, perfis válidos, FK, quantidade/preço/área/datas, exigência de snapshots em itens pendentes, preservação do snapshot após novo custo e rollback de operação parcial.
- Script reproduzível: `scripts/testar-backend.ps1` inicializa/inicia o cluster local na porta 55432, executa a suíte e encerra o servidor em finally. Requer JDK 25, Maven local e PostgreSQL 18. O cluster usa trust apenas em localhost e é encerrado após os testes.
- `scripts/verificar-conexao.ps1` verifica SELECT 1 sem exibir credenciais. Com `-AplicarEstrutura`, cria o schema e aplica V1 em uma transação. Recusa schema já existente; não é um atualizador genérico. Futuras versões exigem novas migrações, preservando V1.
- Logs locais ignorados: `docs/persistencia-red.log` e `docs/persistencia-green.log`.
- Ainda não há integração automática de migrações no startup Spring, nem serviço de envio/aprovação. Regras entre tabelas, autorização e proteção dos snapshots após envio serão concluídas na camada Java com transações e controle de concorrência.
- Etapa seguinte realizada abaixo: repositórios e serviços Java para criação/consulta/envio, com DTOs separados por perfil.

Validação final do script: `./scripts/testar-backend.ps1` executado com sucesso, 20 testes, zero falhas/erros/ignorados; PostgreSQL iniciado e encerrado pelo próprio script. O modo padrão é offline para evitar dependência de rede; use `-BaixarDependencias` apenas quando precisar resolver pacotes. A tentativa anterior com resolução online foi interrompida após ficar sem progresso; a suíte final offline passou integralmente.

Conferência no banco configurado: SELECT 1 bem-sucedido; schema `cotacao` contém 9 tabelas (8 de domínio + `schema_migrations`).

## Serviço Java — terceira entrega, 24/10/2026

Contrato: `docs/CONTRATO-COTACOES.md`. `CotacoesService` aplica as regras e depende de `CotacaoRepository`, transações Spring, identidade e Clock injetáveis. O repositório usa JDBC parametrizado; a dependência JPA permanece disponível para futuras entidades, mas não foi necessário introduzir ORM neste fluxo.

- Criar/editar rascunho próprio com validações, arredondamento do preço e referência a cadastros válidos. Custos não integram o comando aceito.
- Consultar rascunho recalcula a margem comercial com o histórico vigente. Ausência de custo comercial retorna indicador nulo, sem impedir a existência do rascunho.
- Enviar exige os dois custos, calcula snapshots, margem ponderada/preço total, define a maior alçada individual e mantém todos os itens pendentes.
- Consulta após envio usa snapshots. O DTO Comercial não possui custo/margem real; o DTO do aprovador não possui margem comercial. Consulta e alteração são autorizadas pelo usuário cadastrado/ativo e pelo vínculo da cotação.
- `FOR UPDATE` e transações REPEATABLE_READ protegem edição/envio; envio duplicado é rejeitado e falhas revertem a operação. O adaptador HTTP deverá mapear falhas de serialização para conflito e não repetir decisões automaticamente.

TDD: 18 novos testes definidos antes da implementação; RED observado em execução completa: 38 testes, 7 falhas e 11 erros de comportamento ainda ausente (`UnsupportedOperationException` nos contratos), com os 20 anteriores verdes. GREEN: 38 testes, zero falhas/erros/ignorados. Os testes novos usam PostgreSQL isolado, identidade Mockito e relógio fixo. Testes anteriores preservados.

Comando: `./scripts/testar-backend.ps1`. Evidência detalhada: `backend/target/surefire-reports`. O redirecionamento externo do script ficou sem progresso e foi interrompido; as execuções diretas concluíram normalmente. Não foi necessário acessar ou alterar o banco do `.env` nesta etapa.

Limites desta entrega: serviço e repositório testados, ainda sem servidor HTTP/configuração de injeção Spring executável. A identidade simulada existe somente nos testes. Próxima etapa: API REST e autenticação/identidade confiável, listagem paginada e duplicação; depois decisões e Angular.

## API e aplicação executável — quarta entrega, 24/10/2026

Contrato em `docs/CONTRATO-API.md`; execução e exemplos em `docs/EXECUCAO.md`.

- API REST para criação, edição, detalhe, envio e duplicação. Retornos 201/204 e Location nas criações; erros de domínio/conversão/concorrência mapeados para HTTP sem detalhes de infraestrutura.
- Listagem paginada no PostgreSQL com 20 linhas, total, ordenação estável, filtros por estado/cliente/período e autorização incorporada ao SQL. Resumos não contêm custos nem margens; ações disponíveis dependem do estado/perfil.
- Duplicação cria novos IDs e rascunho independente, sem custos/margens/decisões/comentários herdados. Original e histórico preservados; cadastro atual validado.
- Configuração Spring com JDBC/transações REPEATABLE_READ, Clock America/Cuiaba, JSON estrito e identidade oriunda do SecurityContext. JPA não é inicializado neste fluxo JDBC. Sem criação automática de tabelas no startup.
- Segurança: escuta padrão apenas em 127.0.0.1, sem Basic/form login ou senha gerada. Cabeçalhos de identidade/perfil são ignorados; requisições sem contexto autenticado recebem 401. Escritas exigem CSRF da mesma sessão. Waffle/NTLM continua pendente e não há login de desenvolvimento.
- DATABASE_URL convertido para URL JDBC sem senha na URL. Credenciais separadas; suporte a escapes e à senha legada contendo @. Erros de configuração possuem mensagem genérica. Inicializador lê o `.env` sem imprimir nem passar credenciais na linha de comando.

### Evidência TDD e aceite

1. Listagem/duplicação/controladores: RED com 65 testes, 23 falhas e 4 erros por comportamentos ausentes; os 38 anteriores passaram. GREEN subsequente: 65 testes sem falhas.
2. Conexão/identidade/segurança: contratos e testes antes das implementações. A descoberta automática de repositórios JPA foi desativada para que a configuração JDBC inicializasse; essa falha de configuração não foi contabilizada como RED de comportamento. Depois disso, o teste isolado de segurança confirmou 6 falhas e 1 erro nos comportamentos ainda ausentes.
3. Regressão de JSON: mapper inicialmente serializava LocalDate em array, incompatível com o contrato de datas ISO. `JsonFormatoTest` foi escrito e executado primeiro (RED: 1 falha), seguido da correção no mapper. Teste permanente preservado.
4. Correção de especificação autorizada explicitamente pelo usuário: `SegurancaTest.tokenCsrfPermiteEscritaAutenticada` criava uma sessão para obter o token e outra para enviá-lo. Alterado somente `.session(sessao())` para `.session(sessao)` no GET. Expectativas mantidas. Adicionado teste separado que exige 403 para token de outra sessão; a segurança não foi afrouxada.
5. Aceite integrado `ApiPostgresTest`: Spring MVC, filtros, identidade, serviço, repositório e PostgreSQL reais; sessão autenticada sintética somente no teste. Exercita criação, consulta, acesso de outro usuário, rollback por custo ausente, edição, envio, listagem, detalhe do gerente e duplicação.
6. Validação final: `./scripts/testar-backend.ps1 -Empacotar -BaixarDependencias` — **84 testes, zero falhas, zero erros, zero ignorados; BUILD SUCCESS**. Cluster de testes encerrado pelo script. JAR Spring Boot gerado em `backend/target/cotacao-backend-0.1.0-SNAPSHOT.jar`.
7. Smoke HTTP do JAR real com configuração do `.env`: servidor iniciado temporariamente em loopback/porta dinâmica; GET de cotações anônimo = 401; X-Usuario/X-Perfil forjados = 401; GET CSRF = 200. Processo encerrado após a verificação. Nenhuma tabela ou dado da aplicação foi alterado nesta etapa.

Próxima etapa: fluxo de decisões e auditoria, endpoints de cadastros e integração/validação Waffle/NTLM. Frontend Angular permanece posterior aos contratos da API.

## Decisões e auditoria — quinta entrega, 24/10/2026

Contrato em `docs/CONTRATO-APROVACAO.md`. Novas rotas: POST `/api/cotacoes/{id}/decisoes`, GET `/api/cotacoes/{id}/historico` e GET `/api/aprovacoes/pendentes`.

- Confirmação exige exatamente todos os itens pendentes, com IDs únicos pertencentes à cotação e Boolean explícito. Comentário opcional, inclusive em reprovação. Ausência de confirmação nunca decide itens.
- Apenas o aprovador da alçada atribuída decide; o Comercial e outras alçadas recebem acesso negado. O aprovador atribuído decide inclusive itens de faixas inferiores.
- Atualizações dos itens, auditoria com autor/horário e consolidação do cabeçalho na mesma transação REPEATABLE_READ, sob bloqueio da cotação. Snapshots e parâmetros financeiros permanecem intactos. Decisões anteriores e confirmações repetidas não podem ser reescritas.
- Estados consolidados pelo núcleo puro `EstadosCotacao`: rascunho, pendente, aprovada, reprovada e parcial, sem estados adicionais. Serviço rejeita dados persistidos incompatíveis.
- Histórico disponível ao Comercial dono e ao aprovador atribuído, sem custos ou margens; ordenação por data e UUID. Proteções de imutabilidade do histórico já existiam na V1 e foram exercitadas, sem alteração da migração ou do schema real.
- Fila paginada com 20 registros, restrita a cotações pendentes da alçada autenticada. Reutiliza o SQL de paginação/autorização; não há permissões de edição/duplicação para aprovadores.

TDD: contratos e 37 testes novos definidos antes da implementação. RED: 121 testes, 15 falhas e 20 erros por comportamento ausente; os 84 testes anteriores continuaram verdes. Após implementação: 122 testes passando, incluindo a regressão abaixo, zero erros/falhas/ignorados.

Regressão de validação JSON: o mapper aceitava `comentario: 123` e o transformava em texto, apesar de impedir outras coerções escalares. `JsonComentarioTest` foi escrito e executado primeiro: RED com 1 falha pela conversão indevida. Corrigida a política de campos textuais para rejeitar inteiro, decimal e booleano. Novo teste permanente; nenhum teste anterior foi alterado.

Cobertura de banco: aprovar/reprovar/misturar, decidir somente os pendentes quando já há decisões anteriores, preservar snapshots, rejeitar IDs externos/repetidos/incompletos, impedir redecisão, usuários/perfis indevidos, metadados da auditoria, imutabilidade UPDATE/DELETE/TRUNCATE, fila e paginação, falha induzida na segunda auditoria com rollback integral e duas confirmações opostas simultâneas.

Validação final: `./scripts/testar-backend.ps1 -Empacotar` em modo offline — **123 testes, zero falhas, zero erros, zero ignorados; BUILD SUCCESS**. JAR atualizado em `backend/target/cotacao-backend-0.1.0-SNAPSHOT.jar`; cluster de testes encerrado. Nenhum teste anterior foi modificado, nenhuma migração existente foi alterada e o banco do `.env` não foi acessado nesta etapa.

Aceite HTTP `AprovacaoApiPostgresTest`: filtros Spring, identidade, serviço, transações e PostgreSQL reais, com sessão sintética e Clock fixo somente nos testes. Verificados CSRF, Comercial impedido de decidir, consulta sem efeitos, conjunto incompleto e Boolean ausente sem gravações, confirmação parcial, histórico com autor/horário e comentário, confidencialidade por perfil, saída da fila e reconfirmação 409.

Próximas etapas: consultas de cadastros de clientes/produtos para alimentar as telas e integração/validação de Waffle/NTLM. Autenticação corporativa e frontend ainda não estão prontos; não há login ou impersonação de desenvolvimento habilitados.

## Catálogos e integração Windows — sexta entrega, 24/10/2026

Contratos em `docs/CONTRATO-CATALOGOS.md` e `docs/CONTRATO-WINDOWS.md`.

- GET `/api/clientes`, `/api/produtos` e `/api/usuario`, somente para usuários cadastrados ativos. Ordenação estável, metadados agronômicos preservados e produtos inativos identificáveis para consultas históricas. Comercial recebe DTO sem campo real e consulta SQL sem acesso ao histórico real. Aprovador recebe último custo real por data/UUID, incluindo nulo se ausente.
- Waffle 3.7.0 oficial, módulo Spring Security 6/Jakarta; fontes inspecionados e versão publicada confirmada no Maven Central. Ativação via `-WindowsSso` no inicializador ou propriedade `cotacao.auth.windows.enabled=true`. Desativado por padrão.
- Provider apenas Negotiate/NTLM, sem Basic/convidado/impersonação. Perfil de negócio vem do PostgreSQL por conta FQN; grupos Windows não são usados como alçadas. Sessão persiste identidade simples sem senha ou handle nativo, com rotação de sessão/CSRF apenas no login inicial ou troca de conta. Registro Servlet do filtro desabilitado para evitar execução duplicada fora da cadeia Spring.
- Inspeção local somente leitura: PartOfDomain=false. SSO corporativo não pode ser homologado neste computador fora do domínio; permanece dependente de ambiente AD, políticas de navegador, DNS e TLS. Nenhuma política do Windows foi alterada.

### TDD e regressões

- Catálogos: 14 testes escritos antes da implementação. Após corrigir o classpath, RED em 138 testes: 6 falhas e 8 erros por comportamento de catálogo ausente; os 123 anteriores e a nova regressão de dependências passaram.
- Regressão do classpath: a dependência Waffle excluía spring-jcl no caminho escolhido pelo Maven. Após excluir commons-logging duplicado, faltava LogFactory. Novo `WaffleDependenciasTest` escrito e executado primeiro: RED com ClassNotFoundException; corrigido o POM com spring-jcl explícito. A primeira execução com falhas de configuração não foi contabilizada como RED funcional dos catálogos.
- Windows: contratos e 22 testes definidos antes da implementação. Durante preparação, corrigidos os mocks para as assinaturas reais da biblioteca e para sobrescrever resposta excepcional sem executá-la; expectativas não alteradas. RED válido: 22 testes, 17 falhas e 1 erro pelos contratos ainda ausentes. Implementado somente após esse RED.
- GREEN focado: `./scripts/testar-backend.ps1 -Testes 'CatalogoServiceTest,CatalogosControllerTest,WaffleDependenciasTest,FiltroWindowsTest,WaffleConfigurationTest,WaffleApiPostgresTest'` — 37 testes, zero falhas/erros/ignorados.
- Aceite integrado: Waffle real na cadeia Spring e PostgreSQL real, simulando somente a fronteira externa de validação Windows. Verificados conta persistida, perfil do banco, consultas por perfil, conta não cadastrada/inativa, convidados, Basic e cabeçalhos forjados, CSRF e renegociação sem invalidar o token da mesma conta.

Validação completa: `./scripts/testar-backend.ps1 -Empacotar` — **160 testes, zero falhas, zero erros, zero ignorados; BUILD SUCCESS**. JAR atualizado e cluster de testes encerrado. Testes anteriores preservados; nenhuma migração ou dado do banco da aplicação foi alterado.

Smoke nativo do JAR: execução temporária em Windows/loopback/porta dinâmica com `--cotacao.auth.windows.enabled=true`, provider nativo real e DATABASE_URL fictícia, sem credenciais de usuários. Confirmados 401 com desafios Negotiate/NTLM (sem Basic), rejeição 401 de token inválido e de Basic/cabeçalho forjado. Processo encerrado depois da verificação. Isso valida carga do componente nativo e comportamento negativo do protocolo, não um login corporativo positivo.

Próxima etapa: frontend Angular com exatamente três telas e paleta ADM. A homologação AD continua dependente de ambiente corporativo; unidade/conversão e precisão da quantidade recomendada precisam ser esclarecidas antes de implementar sua sugestão. O restante das telas pode progredir de forma independente dessas definições.
