# Decisões e andamento

- Documentação original copiada para a raiz; `.env` é local e ignorado pelo Git.
- Confirmação do usuário: arredondar para baixo somente o percentual exibido das margens; a alçada utiliza a razão precisa.
- Primeira entrega Java: núcleo financeiro isolado, BigDecimal com DECIMAL128, testes JUnit 5. Sem banco, relógio ou serviços externos no cálculo; não são necessários mocks nesse núcleo.
- Próxima etapa escolhida: tabelas PostgreSQL antes do Angular. Históricos independentes, snapshots no envio, autorização e transações precisam sustentar a API usada pelas três telas.
- O arquivo `.env` contém DATABASE_URL; conexão validada com SELECT 1. A migração V1 foi testada em PostgreSQL 18 isolado e aplicada em transação ao schema próprio `cotacao`, com checksum SHA256 registrado em `schema_migrations`. Nenhuma tabela preexistente foi alterada.
- Implementados: repositório JDBC e serviço Java transacional para criar, editar, consultar e enviar cotações; autorização por identidade injetável resolvida na tabela de usuários. Ainda pendentes: configuração da aplicação Spring executável, API REST, Waffle/NTLM, paginação, duplicação, decisões/auditoria e frontend. A estrutura inicial não representa o MVP completo.
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
