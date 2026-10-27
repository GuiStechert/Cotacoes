# Executar a aplicação

Requisitos locais: JDK 25, PostgreSQL 18, Maven 3.9.11 em `.tools/apache-maven-3.9.11` e Node.js 24.14.1 com npm para executar os testes do frontend Vue. A interface usa Vue 3.5.22 via CDN, sem compilação, Angular, Vite ou esbuild. Os navegadores precisam acessar cdn.jsdelivr.net; a versão é fixa e validada por SRI.

```powershell
./scripts/empacotar-aplicacao.ps1 -InstalarDependencias
./scripts/iniciar-backend.ps1
```

Depois do primeiro download, `-InstalarDependencias` pode ser omitido. O empacotador testa o Vue e verifica os arquivos estáticos, executa os testes Java, gera o JAR e confere os arquivos estáticos e suas licenças. O script de testes inicia um PostgreSQL isolado na porta 55432, cria schemas exclusivos por teste e encerra o cluster ao terminar. Não utiliza o banco da aplicação. A suíte deve executar diretamente no terminal, sem redirecionar o script externo.

Abra `http://127.0.0.1:8080`. O JAR serve o frontend Vue e a API na mesma origem. As três telas são lista/fila (`/cotacoes`), edição/consulta comercial (`/cotacao/nova` ou `/cotacao/{id}`) e aprovação (`/aprovacao/{id}`). A sessão obtém o perfil do backend; não existe seletor de perfil. Sem SSO autenticado, a interface apresenta a falha de acesso.

Para desenvolver a interface, edite os arquivos de frontend/public e reempacote o backend, que serve interface e API na mesma origem. Use `./scripts/testar-frontend.ps1 -Instalar -Build` para instalar dependências de teste, executar testes e verificar os arquivos estáticos, ou `./scripts/testar-backend.ps1` para a suíte Java. `npm run build` apenas verifica sintaxe e recursos; não compila nem executa esbuild. Maven inclui diretamente os arquivos de frontend/public. Use clean package ao empacotar manualmente para remover recursos legados. O JAR final não exige Node, npm ou ferramentas de build no servidor.

O inicializador lê DATABASE_URL do ambiente ou do `.env` da raiz sem imprimir o valor e sem passá-lo como argumento da linha de comando. O schema padrão é `cotacao`, já criado no banco configurado. A aplicação não cria tabelas no startup nem altera a migração V1 aplicada.

O JAR gerado é `backend/target/cotacao-backend-0.1.0-SNAPSHOT.jar`. Para iniciar manualmente com DATABASE_URL já definido:

```powershell
java -jar backend/target/cotacao-backend-0.1.0-SNAPSHOT.jar
```

Por padrão, HTTP escuta em `127.0.0.1:8080`. GET `/api/csrf` fornece um token da sessão. As rotas de cotações exigem autenticação Spring Security; escritas também exigem esse token no cabeçalho indicado pela resposta, usando a mesma sessão.

As páginas e os arquivos estáticos são públicos para permitir carregar a interface e apresentar erros de acesso. Sem ativar o SSO Windows, a API continua recebendo 401. X-Usuario, X-Perfil e Basic não concedem acesso. Não há usuário/senha padrão ou perfil de execução com impersonação. A integração Waffle/NTLM está disponível com ativação explícita, mas a homologação com Active Directory ainda é necessária.

## SSO Windows

O roteiro de preparação, testes e aceite pela TI está em [HOMOLOGACAO-AD.md](HOMOLOGACAO-AD.md), incluindo os limites da validação local sem domínio/rede corporativa.

```powershell
./scripts/iniciar-backend.ps1 -WindowsSso
```

Também é possível definir `COTACAO_AUTH_WINDOWS_ENABLED=true` no ambiente do processo, ou passar `--cotacao.auth.windows.enabled=true` ao JAR. O padrão é false. A opção exige Windows, anuncia Negotiate/NTLM e não habilita Basic, convidados ou impersonação.

A conta validada pelo Windows (formato DOMINIO\usuario) precisa corresponder a `usuarios.conta_windows`, com cadastro ativo, email e perfil cadastrados. Grupos do Windows não concedem alçadas; o PostgreSQL é a fonte dos perfis. Contas válidas sem cadastro ativo recebem 403. A sessão guarda apenas o nome validado, sem senha ou handle nativo.

O cliente deve consultar `/api/usuario` para completar a negociação e obter seu perfil, depois `/api/csrf` na mesma sessão antes de escrever. Login inicial ou troca de conta renova o ID da sessão e invalida o CSRF anterior; renegociação da mesma conta mantém o token.

Este computador não está ingressado em domínio. A validação de AD/SSO real depende de máquina/usuários corporativos, políticas de navegador e intranet, DNS e TLS definidos pela TI. Os testes simulam a fronteira de autenticação Windows; não substituem essa homologação. A aplicação continua escutando em loopback por padrão, sem alteração de firewall, registro do Windows ou políticas do navegador.

## Rotas disponíveis

| Método | Rota | Resultado |
|---|---|---|
| POST | `/api/cotacoes` | Criar rascunho, 201 com Location e id |
| GET | `/api/cotacoes?page=0&size=20` | Lista autorizada com total e paginação |
| GET | `/api/cotacoes/{id}` | DTO de consulta segundo perfil |
| PUT | `/api/cotacoes/{id}` | Editar rascunho, 204 |
| POST | `/api/cotacoes/{id}/enviar` | Fixar snapshots e enviar, 204 |
| POST | `/api/cotacoes/{id}/duplicar` | Novo rascunho independente, 201 |
| POST | `/api/cotacoes/{id}/decisoes` | Confirmar todos os itens pendentes, 204 |
| GET | `/api/cotacoes/{id}/historico` | Auditoria autorizada sem custos/margens |
| GET | `/api/aprovacoes/pendentes?page=0&size=20` | Fila paginada do aprovador atribuído |
| GET | `/api/clientes` | Cadastro de clientes para consulta |
| GET | `/api/produtos` | Metadados dos produtos; custo real somente para aprovadores |
| GET | `/api/usuario` | ID, nome e perfil do próprio usuário ativo |

Filtros opcionais da lista: `status`, `clienteId`, `inicio` e `fim` (datas ISO). Corpo de criação/edição:

```json
{
  "clienteId": "uuid-cliente",
  "faturamento": "2026-01-01",
  "vencimento": "2026-08-01",
  "jurosAnual": 0.12,
  "areaCliente": null,
  "itens": [
    { "produtoId": "uuid-produto", "quantidade": 2, "preco": 100.001 }
  ]
}
```

UUIDs do exemplo devem ser substituídos por referências cadastradas. Campos extras, custos e perfis enviados pelo consumidor são rejeitados. Datas saem em ISO; erros retornam apenas `{ "codigo": "..." }`, sem SQL e credenciais.

Para confirmar uma aprovação, envie os IDs de TODOS os itens pendentes, sem repetições:

```json
{
  "itens": [
    { "itemId": "uuid-item-1", "aprovado": true },
    { "itemId": "uuid-item-2", "aprovado": false, "comentario": "Revisar preço" }
  ]
}
```

O campo `aprovado` é obrigatório; reprovar não exige comentário. Somente o perfil atribuído decide, sempre com CSRF da mesma sessão autenticada. A confirmação grava itens, autor, horário, comentário e estado final em uma transação; envio repetido ou decisão concorrente conflitante retorna 409. Detalhar, listar ou consultar histórico não aprova itens. O contrato completo está em `docs/CONTRATO-APROVACAO.md`.

Os catálogos são somente leitura. Produtos inativos permanecem na consulta com `ativo=false`, para identificar produtos de cotações históricas; novas inclusões continuam bloqueadas pelo backend. O catálogo Comercial não retorna custo real nem consulta sua tabela. O catálogo do aprovador retorna somente o custo real vigente, sem expor todo o histórico.

Ainda pendentes: homologação corporativa Waffle/NTLM e integração/atualização dos cadastros. A recomendação usa dose × área na mesma unidade do produto, sem conversão por embalagem, com duas casas e arredondamento para cima. O usuário aplica a sugestão explicitamente; quantidades manuais com mais de duas casas efetivas são rejeitadas.
