# Executar o backend

Requisitos locais: JDK 25, PostgreSQL 18, Maven 3.9.11 em `.tools/apache-maven-3.9.11`.

```powershell
./scripts/testar-backend.ps1 -Empacotar -BaixarDependencias
./scripts/iniciar-backend.ps1
```

Depois do primeiro download, `-BaixarDependencias` pode ser omitido. O script de testes inicia um PostgreSQL isolado na porta 55432, cria schemas exclusivos por teste e encerra o cluster ao terminar. Não utiliza o banco da aplicação. A suíte deve executar diretamente no terminal, sem redirecionar o script externo.

O inicializador lê DATABASE_URL do ambiente ou do `.env` da raiz sem imprimir o valor e sem passá-lo como argumento da linha de comando. O schema padrão é `cotacao`, já criado no banco configurado. A aplicação não cria tabelas no startup nem altera a migração V1 aplicada.

O JAR gerado é `backend/target/cotacao-backend-0.1.0-SNAPSHOT.jar`. Para iniciar manualmente com DATABASE_URL já definido:

```powershell
java -jar backend/target/cotacao-backend-0.1.0-SNAPSHOT.jar
```

Por padrão, HTTP escuta em `127.0.0.1:8080`. GET `/api/csrf` fornece um token da sessão. As rotas de cotações exigem autenticação Spring Security; escritas também exigem esse token no cabeçalho indicado pela resposta, usando a mesma sessão.

Esta entrega não oferece uma forma de login: Waffle/NTLM ainda não foi integrado e validado. Assim, requisições externas recebem 401; X-Usuario, X-Perfil e Basic não concedem acesso. Os contextos autenticados utilizados na suíte existem somente nos testes. Não há usuário/senha padrão ou perfil de execução com impersonação.

## Rotas disponíveis

| Método | Rota | Resultado |
|---|---|---|
| POST | `/api/cotacoes` | Criar rascunho, 201 com Location e id |
| GET | `/api/cotacoes?page=0&size=20` | Lista autorizada com total e paginação |
| GET | `/api/cotacoes/{id}` | DTO de consulta segundo perfil |
| PUT | `/api/cotacoes/{id}` | Editar rascunho, 204 |
| POST | `/api/cotacoes/{id}/enviar` | Fixar snapshots e enviar, 204 |
| POST | `/api/cotacoes/{id}/duplicar` | Novo rascunho independente, 201 |

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

Ainda pendentes: autenticação Waffle/NTLM, decisões/auditoria pela API, cadastros de produtos/clientes pela API e frontend Angular.
