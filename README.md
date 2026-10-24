# Sistema de Cotações e Aprovação de Margens

## 1. Objetivo

Aplicação web interna para cadastrar cotações comerciais com múltiplos produtos, calcular indicadores financeiros e encaminhar a cotação diretamente ao perfil com a maior alçada exigida pela **margem real individual** dos produtos. O aprovador decide individualmente cada item.

## 2. Stack e implantação

- **Frontend:** Angular + TypeScript, com identidade visual baseada em `tema_ADM.json`.
- **Backend:** Java + Spring Boot, API REST, Spring Data JPA e Spring Security (autorização por perfis e integração com identidade Windows autenticada).
- **Banco de dados:** PostgreSQL.
- **Acesso:** navegador na rede interna corporativa; inicialmente hospedado no computador corporativo do desenvolvedor, preferencialmente via HTTPS e DNS interno.
- **Segurança de rede:** o navegador acessa a API; o PostgreSQL é acessível somente ao backend.

### Autenticação Windows integrada (Active Directory / NTLM)

**Objetivo:** identificar automaticamente o usuário que acessa a aplicação no navegador, usando a sessão corporativa do Windows/Active Directory (exemplo de formato de conta: `SA\I292550`), sem exigir login manual quando o ambiente suportar autenticação integrada.

- **Mecanismo pretendido:** Windows Integrated Authentication por **Negotiate**, com suporte a **NTLM** conforme a infraestrutura corporativa; Kerberos pode ser negociado quando disponível. A escolha definitiva do componente que valida NTLM depende de testes e das políticas da TI.
- **Hospedagem inicial:** frontend e backend serão disponibilizados na rede interna a partir do computador corporativo do desenvolvedor. A máquina precisa permanecer ligada, acessível na rede e autorizada pelas políticas e firewall corporativos.
- **Responsabilidade do navegador:** Chrome/Edge pode negociar automaticamente as credenciais Windows com um servidor configurado para autenticação integrada, sujeito a políticas de intranet e navegador. **Angular/JavaScript puro não lê `whoami`, nem extrai diretamente o usuário Windows.**
- **Responsabilidade do servidor:** um componente confiável (por exemplo, IIS com Windows Authentication atuando como proxy reverso, se disponível) valida a autenticação e fornece a identidade ao Spring Boot. O Spring Security aplica as permissões após resolver a conta na tabela `usuarios`.
- **Confiança no proxy:** se a identidade for transmitida ao Spring Boot por cabeçalho, o backend deve estar inacessível diretamente aos clientes, aceitar o cabeçalho apenas do proxy autenticador e impedir cabeçalhos forjados. Nunca considerar um `X-Usuario` fornecido pelo navegador como autenticação válida.
- **`whoami` no backend:** identifica apenas a conta que executa o processo Java, não o visitante.
- **Perfis:** `COMERCIAL`, `GESTOR`, `SUPERVISOR`, `GERENTE`, associados à identidade corporativa autenticada no PostgreSQL. Um usuário sem perfil cadastrado deve ter acesso negado.
- **Fallback exclusivamente de desenvolvimento:** permitir simulação explícita de usuário/perfil apenas em perfil de execução local isolado, desativado na aplicação acessível pela rede e em produção.
- **Transporte:** preferir HTTPS também na intranet. A implementação deve considerar proteção contra CSRF quando usar autenticação automática do navegador e cookies.

**Faseamento:** desenvolver desde já os perfis e a autorização, deixando a identificação encapsulada em um serviço de identidade. Implementar e validar a autenticação Windows integrada quando a configuração de NTLM/Negotiate, do proxy e das políticas corporativas estiver disponível. Não publicar o MVP com impersonação de usuário habilitada.

## 3. Perfis e acesso a dados

| Funcionalidade | Comercial | Gestor | Supervisor | Gerente |
|---|---|---|---|---|
| Criar/editar rascunho próprio | Sim | Não | Não | Não |
| Enviar cotação | Sim | Não | Não | Não |
| Consultar preço total | Sim | Sim | Sim | Sim |
| Consultar margem comercial ponderada | Sim | Não (por padrão) | Não (por padrão) | Não (por padrão) |
| Consultar custo real e margem real | Não | Sim | Sim | Sim |
| Decidir produtos pendentes | Não | Cotações atribuídas | Cotações atribuídas | Cotações atribuídas |

A API deve usar DTOs separados para o Comercial e os aprovadores. **Nunca enviar `custo_real` ou `margem_real` ao Comercial**, nem apenas escondê-los no Angular.

## 4. Dados da cotação

### Cabeçalho

- `id` (UUID)
- `cliente_id` (referência ao cliente; nome exibido)
- `data_faturamento` (date)
- `data_vencimento` (date)
- `juros_anual` (decimal; armazenar como fração, por exemplo 12% = `0.12`)
- `status` (enum da cotação)
- `solicitante_id` (referência ao usuário)
- `perfil_aprovador` (enum, definido no envio)
- `area_cliente` (decimal positivo opcional, em hectares; utilizado apenas para sugestão de quantidade)
- `criado_em`, `atualizado_em` (timestamps)

### Itens da cotação

- `id` (UUID), `cotacao_id`, `produto_id`
- `quantidade` (decimal positivo)
- `preco` (decimal unitário positivo, com duas casas após arredondamento)
- `custo` (decimal, lido da tabela de produtos)
- `custo_real` (decimal, lido da tabela de produtos, restrito)
- `margem` (calculada)
- `margem_real` (calculada, restrita)
- `status` (enum do item)
- `comentario` (texto opcional)

**Fonte dos custos:** tabelas históricas `produto_custos` e `produto_custos_reais`, associadas à tabela `produtos`. O backend deve consultar o **registro mais recente de cada histórico por produto**, sem confiar em custos enviados pelo frontend. Os dois históricos são independentes. Ao enviar a cotação, persistir snapshot dos custos, margens e parâmetros efetivamente usados, para que alterações posteriores não modifiquem a decisão histórica. No rascunho, recalcular conforme o cadastro vigente; no envio, fixar o snapshot.

## 5. Regras financeiras

### 5.1 Meses de juros (inclusivos)

```text
meses_juros = (ano_vencimento - ano_faturamento) * 12
            + (mes_vencimento - mes_faturamento) + 1
```

Exemplo: janeiro a agosto do mesmo ano = **8 meses**. Exigir `data_vencimento >= data_faturamento`.

### 5.2 Juros simples

```text
juros_mensal = juros_anual / 12
fator_juros  = 1 + juros_mensal * meses_juros
```

### 5.3 Margem comercial e real

```text
margem      = custo      * fator_juros / preco
margem_real = custo_real * fator_juros / preco
```

As margens são razões decimais: `0.10` = **10%**. **Manter exatamente essas fórmulas**, mesmo que o indicador não corresponda à definição convencional de margem de lucro.

### 5.4 Indicadores consolidados

```text
quantidade_total = SOMA(quantidade_i)
margem_ponderada = SOMA(margem_i * quantidade_i) / quantidade_total
margem_real_ponderada = SOMA(margem_real_i * quantidade_i) / quantidade_total
preco_total = SOMA(preco_i * quantidade_i)
```

- Comercial: exibir `margem_ponderada` e `preco_total`.
- Aprovadores: exibir `margem_real_ponderada` e `preco_total`.
- Validar `quantidade_total > 0`.
- **A alçada NÃO é determinada pela margem ponderada**, e sim pela margem real individual mais crítica entre os itens.

### 5.5 Arredondamento

Arredondar o **preço unitário para cima, com duas casas decimais**, antes de exibir e usar nos cálculos finais. Em Java, para preços positivos:

```java
BigDecimal precoFinal = precoCalculado.setScale(2, RoundingMode.CEILING);
```
Usar `BigDecimal` para cálculos financeiros, política explícita de precisão intermediária e evitar `double` para dinheiro. Exemplo: `100.001` → `100.01`; `100.990` → `100.99`.

Arredondar o **preço margens para baixo, com duas casas decimais**, antes de exibir e usar nos cálculos finais.

## 6. Faixas e encaminhamento

A classificação usa **`margem_real` individual**:

| Margem real | Alçada |
|---|---|
| `>= 10%` | `GESTOR` |
| `>= 8%` e `< 10%` | `SUPERVISOR` |
| `< 8%` | `GERENTE` |

Ordem de alçada: `GESTOR < SUPERVISOR < GERENTE`.

Ao enviar uma cotação, calcular a faixa de cada produto e escolher a **maior alçada**. Encaminhar **diretamente** a esse perfil: Gerente não precisa de aprovação de Supervisor ou Gestor; Supervisor não precisa de Gestor. O aprovador responsável pode decidir **todos os produtos** da cotação, inclusive os de faixas inferiores.

**Não existe aprovação automática por margem:** todos os itens enviados precisam ser decididos por um aprovador.

Exemplo: itens nas faixas Gestor, Supervisor e Gerente → toda a cotação vai diretamente ao **Gerente**.

## 7. Estados

### 7.1 Cotação (`CotacaoStatus`)

- `RASCUNHO`: em elaboração.
- `PENDENTE_APROVACAO`: enviada e aguardando decisão dos itens.
- `APROVADA`: todos os itens aprovados.
- `APROVADA_PARCIAL`: há itens aprovados e reprovados, sem pendências.
- `REPROVADA`: todos os itens reprovados.

### 7.2 Produto (`ItemStatus`)

- `RASCUNHO`: item ainda não enviado.
- `PENDENTE`: item enviado, ainda sem decisão.
- `APROVADO`: aprovado pelo aprovador responsável.
- `REPROVADO`: reprovado pelo aprovador responsável.

### 7.3 Transições e consolidação

1. Criação: cotação `RASCUNHO`, itens `RASCUNHO`.
2. Envio: validar campos, recalcular valores e alçada, cotação `PENDENTE_APROVACAO`, itens `PENDENTE`.
3. Decisão: cada item passa de `PENDENTE` a `APROVADO` ou `REPROVADO`.
4. Se ainda existir algum `PENDENTE`, a cotação permanece `PENDENTE_APROVACAO`.
5. Sem pendências: todos aprovados → `APROVADA`; todos reprovados → `REPROVADA`; mistura → `APROVADA_PARCIAL`.

Não introduzir outros estados. Alterações ou reenvios após decisão exigem regra de negócio futura específica.

## 8. Tela de aprovação

- Listar todos os produtos da cotação, seus preços, quantidades, margens reais e o indicador real ponderado.
- Cada produto apresenta uma **checkbox de aprovação marcada por padrão**.
- Se a checkbox permanecer marcada, a decisão proposta é `APROVADO`.
- Ao **desmarcar**, exibir o campo `comentario` do item; a decisão proposta é `REPROVADO`.
- **Comentário é opcional**, mesmo para reprovação.
- A checkbox é apenas um valor de interface: abrir a tela **não altera** o estado `PENDENTE` no banco.
- O botão de confirmação envia as decisões de todos os itens pendentes; o backend valida perfil, alçada, vínculo com a cotação e estados, grava as decisões e recalcula o status consolidado.
- Recomenda-se registrar usuário, horário, decisão e comentário em histórico de auditoria; usar transação para evitar gravação parcial e controle de concorrência para evitar duas decisões conflitantes.

## 9. Telas do frontend (exatamente três)

### 9.1. Tela de cotação — Comercial

- Criar cotação e editar **somente** cotações com estado `RASCUNHO`, respeitando a autorização do usuário.
- Campos: cliente, data de faturamento, data de vencimento, juros anuais, **área do cliente em hectares (opcional)** e itens com produto, quantidade e preço unitário.
- Exibir a margem comercial ponderada e o preço total; nunca exibir custo real nem margem real.
- Para cada produto, oferecer o botão **“Usar quantidade recomendada”** somente quando a área do cliente tiver sido informada e os parâmetros do produto estiverem disponíveis. Ao clicar, preencher a quantidade do item com:

  ```text
  quantidade_recomendada = area_cliente * dose_recomendada * aplicacoes_recomendadas
  ```

- A sugestão não substitui automaticamente a quantidade digitada: o botão faz o preenchimento por ação explícita, permitindo ajuste manual enquanto o rascunho estiver editável.
- Validar área positiva quando preenchida, dose positiva e número de aplicações positivo; definir unidades compatíveis para área, dose e quantidade. A quantidade calculada deve respeitar a precisão definida para o produto.
- Exibir botão de envio somente para rascunhos válidos. Cotações nos demais estados podem ser consultadas, mas não editadas.

### 9.2. Tela de aprovação — somente aprovadores

- Disponível exclusivamente aos perfis `GESTOR`, `SUPERVISOR` e `GERENTE`, com acesso apenas às cotações atribuídas à sua alçada.
- Exibir os itens da cotação, preço total, margem real individual e margem real ponderada.
- Checkbox **marcada por padrão** para cada item pendente; desmarcar revela comentário opcional.
- A decisão só é persistida após confirmação. O aprovador da maior alçada pode decidir todos os itens da cotação.
- Itens já decididos não podem ser novamente alterados.

### 9.3. Tela de listagem de cotações

- Tabela paginada no **backend**, com **20 linhas por página**; exibir total de registros, página atual e controles anterior/próxima (e seleção de página, se desejado).
- Ordenação padrão: cotações mais recentes primeiro (`criado_em DESC`, com `id` como desempate). Filtros por estado, cliente e período, respeitando as permissões.
- Colunas sugeridas: ID, cliente, solicitante, criação, estado, perfil aprovador, preço total e ações. Indicadores de margem devem ser filtrados por perfil.
- Ação **Editar**: habilitada exclusivamente para `RASCUNHO` e usuário autorizado.
- Ação **Duplicar**: disponível exclusivamente para cotações cujo estado seja **diferente de `RASCUNHO`** e para usuários com permissão de criar cotações.
- A duplicação cria **uma nova cotação com novo ID**, estado `RASCUNHO`, todos os itens com novos IDs e estado `RASCUNHO`, sem decisões, comentários de aprovação ou histórico herdado. Copiar cliente, datas, juros, área (se houver), produtos, quantidades e preços; custos e margens deverão ser recalculados usando os registros mais recentes, antes de novo envio. A cotação original permanece inalterada.
- O backend deve bloquear edição e duplicação fora das regras mesmo se a API for chamada diretamente.
- A listagem também serve de acesso ao detalhe da cotação e, quando permitido, à tela de aprovação; não criar telas adicionais para essas funções.

## 10. Tema visual ADM

Usar o arquivo fornecido `tema_ADM.json` como referência de identidade visual, com estas cores principais:

| Uso | Hex |
|---|---|
| Azul principal | `#012169` |
| Verde ADM / ações de aprovação | `#00AD50` |
| Azul secundário | `#007DBA` |
| Laranja / alertas | `#F68D2E` |
| Azul escuro / destaque em tabelas | `#0F1934` |
| Texto principal | `#070F25` |
| Fundo | `#FFFFFF` |
| Verde claro | `#8BC400` |

Paleta completa (`dataColors`), na ordem do tema:

```json
["#012169", "#00AD50", "#007DBA", "#F68D2E", "#00109E", "#8BC400", "#008ED8", "#652F8F", "#3599B8", "#DFBFBF", "#4AC5BB", "#5F6B6D", "#FB8281", "#F4D25A", "#7F898A", "#A4DDEE", "#FDAB89", "#B687AC", "#28738A", "#A78F8F", "#168980", "#293537", "#BB4A4A", "#B59525", "#475052", "#6A9FB0", "#BD7150", "#7B4F71", "#1B4D5C", "#706060", "#0F5C55", "#1C2325"]
```

Implementar tokens CSS para manter consistência. Usar azul ADM em navegação, verde para ações positivas, laranja para pendências e fundo branco. As cores não substituem rótulos textuais de estado.

## 11. Modelo de dados PostgreSQL

| Tabela | Conteúdo |
|---|---|
| `usuarios` | Identidade futura, perfil e dados de exibição |
| `clientes` | Cadastro de clientes |
| `produtos` | Cadastro e parâmetros agronômicos do produto |
| `produto_custos` | Histórico de custo comercial por produto |
| `produto_custos_reais` | Histórico de custo real por produto, restrito a aprovadores |
| `cotacoes` | Cabeçalho, `area_cliente`, status, solicitante, perfil aprovador |
| `cotacao_itens` | Produtos, quantidades, preços, snapshots de custos e margens, status, comentário |
| `historico_aprovacoes` | Decisões por item, autor, data, comentário |

### 11.1. `produtos`

| Coluna | Tipo sugerido | Observação |
|---|---|---|
| `id` | UUID (PK) | ID do produto |
| `nome` | VARCHAR | Nome do produto |
| `principio_ativo` | VARCHAR | Princípio ativo |
| `embalagem` | NUMERIC | Embalagem |
| `dose_recomendada` | NUMERIC | Dose por hectare por aplicação, em unidade definida |
| `aplicacoes_recomendadas` | NUMERIC | Número recomendado de aplicações |
| `unidade_medida` | VARCHAR | Unidade de medida |
| `margem_recomendada` | NUMERIC | Razão decimal, ex.: `0.10` para 10% |
| `data_insercao` | TIMESTAMPTZ | Criação do cadastro |
| `data_desativacao` | TIMESTAMPTZ NULL | Desativação, quando ocorrer |
| `ativo` | BOOLEAN | Indica se está disponível para novas cotações |


### 11.2. `produto_custos` — histórico de custos comerciais

| Coluna | Tipo sugerido | Observação |
|---|---|---|
| `id` | UUID (PK) | Identificador do registro histórico |
| `produto_id` | UUID (FK → `produtos.id`) | Produto |
| `custo` | NUMERIC | Custo comercial registrado |
| `data_criacao` | TIMESTAMPTZ | Momento de registro |

### 11.3. `produto_custos_reais` — histórico de custos reais

| Coluna | Tipo sugerido | Observação |
|---|---|---|
| `id` | UUID (PK) | Identificador do registro histórico |
| `produto_id` | UUID (FK → `produtos.id`) | Produto |
| `custo_real` | NUMERIC | Custo real registrado, acesso restrito |
| `data_criacao` | TIMESTAMPTZ | Momento de registro |

**Política de histórico:** cada atualização de custo insere uma **nova linha** na tabela correspondente, sem sobrescrever nem apagar registros anteriores. Buscar sempre o registro de `data_criacao` mais recente **para cada produto e para cada tipo de custo separadamente**. Para empates de timestamp, ordenar também pelo `id` de forma determinística (ou utilizar chave sequencial de versão). Indexar `(produto_id, data_criacao DESC)`. Se não houver custo necessário para o produto, impedir o envio da cotação com erro de validação específico.

Exemplo de consulta ao custo comercial mais recente:

```sql
SELECT custo
FROM produto_custos
WHERE produto_id = :produto_id
ORDER BY data_criacao DESC, id DESC
LIMIT 1;
```

Para o custo real, consultar `produto_custos_reais` da mesma forma. Não retornar esse histórico nem seu custo ao perfil Comercial.

### 11.4. Persistência e integridade

- `cotacoes.area_cliente`: `NUMERIC NULL`; se informada, deve ser positiva.
- `cotacao_itens`: guardar o snapshot de `custo`, `custo_real`, `margem` e `margem_real` calculados no envio, preservando o histórico mesmo após novas alterações de custos.
- Chaves estrangeiras e índices nas relações principais; validar valores positivos e timestamps de criação.
- As faixas podem começar como constantes do backend e migrar para parâmetros configuráveis posteriormente.

## 12. API REST inicial (proposta)

| Método | Endpoint | Finalidade |
|---|---|---|
| `GET` | `/api/produtos` | Consultar produtos (resposta filtrada por perfil) |
| `GET` | `/api/clientes` | Consultar clientes |
| `POST` | `/api/cotacoes` | Criar rascunho |
| `GET` | `/api/cotacoes?page=0&size=20` | Listar cotações autorizadas com paginação de 20 registros |
| `GET` | `/api/cotacoes/{id}` | Detalhar cotação segundo perfil |
| `PUT` | `/api/cotacoes/{id}` | Editar exclusivamente rascunho autorizado |
| `POST` | `/api/cotacoes/{id}/duplicar` | Duplicar cotação não rascunho, gerando novo rascunho |
| `POST` | `/api/cotacoes/{id}/enviar` | Recalcular e enviar para aprovação |
| `GET` | `/api/aprovacoes/pendentes` | Listar cotações atribuídas ao aprovador |
| `POST` | `/api/cotacoes/{id}/decisoes` | Registrar decisões de todos os itens pendentes |
| `GET` | `/api/cotacoes/{id}/historico` | Consultar auditoria autorizada |

### Exemplo de envio de decisões

```json
{
  "itens": [
    { "itemId": "uuid-item-1", "aprovado": true },
    { "itemId": "uuid-item-2", "aprovado": false, "comentario": "Revisar preço" }
  ]
}
```

O backend deve verificar que os IDs pertencem à cotação e que o solicitante tem permissão para decidir a cotação. A implementação real deverá usar autenticação confiável quando esta for definida.

## 13. Requisitos de validação

- Cliente, datas, juros e ao menos um produto obrigatórios.
- Quantidade e preço unitário maiores que zero.
- Vencimento não anterior ao faturamento.
- Produtos existentes e ativos para novas inclusões; custos obtidos dos históricos `produto_custos` e `produto_custos_reais`, usando o registro mais recente de cada um.
- Margens e alçadas recalculadas no backend; nunca confiar em cálculos recebidos do Angular.
- Comercial não pode decidir aprovações nem consultar custo/margem real.
- Aprovadores só decidem cotações de sua fila atribuída.
- Impedir decisões duplicadas ou alterações concorrentes inconsistentes.
- O cálculo ponderado usa quantidades e exige denominador positivo.
- Arredondar preço unitário para cima com duas casas, antes dos cálculos finais.
- Área do cliente é opcional; quando preenchida, deve ser positiva. A sugestão de quantidade é `area_cliente * dose_recomendada * aplicacoes_recomendadas`, com unidades compatíveis.
- Paginação de cotações em 20 registros por página; ordenar de modo estável e respeitar filtros/autorização.
- Impedir edição de qualquer cotação fora de `RASCUNHO`; impedir duplicação de `RASCUNHO`.
- Duplicar não altera o original e cria novos IDs, estados `RASCUNHO` e nenhuma decisão herdada.
- Não expor custo real ou seu histórico a perfis não autorizados.

## 14. Critérios de aceite (MVP)

1. Comercial cria uma cotação com múltiplos produtos e visualiza preço total e margem comercial ponderada.
2. O sistema calcula meses inclusivos e juros simples corretamente.
3. O backend busca custos dos produtos e calcula margem e margem real.
4. Uma cotação com qualquer item abaixo de 8% de margem real vai diretamente ao Gerente.
5. Uma cotação sem itens abaixo de 8%, mas com item entre 8% e 10%, vai diretamente ao Supervisor.
6. Uma cotação cujos itens têm margem real >= 10% vai ao Gestor.
7. Todos os itens permanecem pendentes até a decisão do aprovador; nenhum é aprovado automaticamente.
8. A tela de aprovação inicia com todas as checkboxes marcadas e mostra comentário somente ao desmarcar.
9. O backend persiste decisões individuais e consolida corretamente `APROVADA`, `APROVADA_PARCIAL` ou `REPROVADA`.
10. Comercial não recebe custos reais nem margens reais na API.
11. O Angular segue a paleta ADM.
12. O frontend possui exatamente três telas: cotação comercial, aprovação restrita a aprovadores e listagem paginada de cotações.
13. A listagem retorna 20 registros por página, com total, filtros e navegação corretos.
14. Apenas rascunhos podem ser editados; o botão duplicar só aparece em estados diferentes de `RASCUNHO` e cria um novo rascunho independente.
15. O produto armazena nome, princípio ativo, embalagem, dose, aplicações, margem recomendada, datas de inserção/desativação e indicador ativo.
16. Custos comerciais e reais possuem históricos separados e imutáveis; o backend seleciona o registro mais recente de cada histórico.
17. O formulário aceita área opcional e oferece preenchimento da quantidade recomendada por produto com `área * dose * aplicações`, sem alterar quantidades automaticamente.
18. O módulo de autorização utiliza a identidade fornecida por um serviço de identidade substituível; a integração Windows/NTLM deverá ser validada antes da disponibilização para usuários reais.
19. Nenhuma requisição externa pode obter privilégios enviando um nome de usuário arbitrário em cabeçalhos HTTP.

## 15. Autenticação

- Utilizar a biblioteca Waffle/NTLM para autenticar os usuários.
- Crie uma tabela no DB com os email cadastrados e quais são seus perfis.

## 17. Versionamento

- Utilize GIT para versionamento.

## 16. Definições futuras

- Regras de designação quando houver vários aprovadores no mesmo perfil.
- Política de reabertura, edição ou reenvio de cotações decididas.
- Precisão/escala final de margens e regras de arredondamento intermediário.
- Integração e atualização dos cadastros de clientes/produtos.
- Unidade da dose e da quantidade, conversão por embalagem e precisão da quantidade recomendada.
- Política de acesso à duplicação por usuários de perfis aprovadores que também possam criar cotações.
- Hospedagem, DNS interno, certificados TLS e requisitos de infraestrutura corporativa.