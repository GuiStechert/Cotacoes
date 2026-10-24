# Decisões, auditoria e fila de aprovação

`POST /api/cotacoes/{id}/decisoes` recebe `{ "itens": [{ "itemId": "uuid", "aprovado": true, "comentario": null }] }` e retorna 204. A lista deve conter exatamente todos os itens atualmente pendentes, sem IDs repetidos ou externos. `aprovado` é Boolean obrigatório: sua ausência nunca implica aprovação. Comentário é texto opcional; inclusive reprovar sem comentário é válido. O serviço aceita comentário fornecido para qualquer decisão; a interface futura só o oferecerá ao desmarcar a aprovação, conforme README.

Somente usuário ativo cadastrado com perfil igual à alçada atribuída pode decidir. Comercial não decide; um Gerente não assume uma fila atribuída ao Gestor. O aprovador atribuído pode decidir TODOS os produtos da cotação, independentemente da faixa individual. A cotação deve estar PENDENTE_APROVACAO e itens já decididos não podem ser alterados.

O envio deve coincidir exatamente com o conjunto pendente. Campos ausentes/nulos/tipos inválidos: DADOS_INVALIDOS (400). Conjunto incompleto, IDs repetidos/externos: DECISOES_INVALIDAS (400). Item já decidido, estado de cotação incompatível ou dados persistidos inconsistentes: ESTADO_INVALIDO (409). Permissão e autenticação usam os códigos existentes. Concorrência PostgreSQL mantém CONFLITO_CONCORRENCIA (409).

Bloqueio FOR UPDATE da cotação e transação REPEATABLE_READ abrangem validação, atualização dos itens, inserção dos registros de auditoria e consolidação do cabeçalho. Qualquer falha reverte tudo. Nenhum custo, margem ou parâmetro do snapshot é recalculado ou modificado na decisão. Confirmação repetida é rejeitada; decisões simultâneas não podem persistir resultados conflitantes.

Consolidação: todos RASCUNHO → RASCUNHO; qualquer PENDENTE sem rascunhos → PENDENTE_APROVACAO; todos APROVADO → APROVADA; todos REPROVADO → REPROVADA; mistura final → APROVADA_PARCIAL. Lista vazia, estado desconhecido e mistura de rascunho com estados enviados são inválidos. Na confirmação normal, todos os pendentes são decididos de uma só vez, podendo coexistir itens previamente decididos.

`GET /api/cotacoes/{id}/historico` retorna id do evento, itemId, usuarioId, autorNome, criadoEm ISO, decisao e comentario, ordenados por criado_em ASC/id ASC. Acesso: Comercial dono ou aprovador da alçada atribuída, incluindo cotações finalizadas. Não retorna custos nem margens. A auditoria registra relógio e autor do backend e é imutável no PostgreSQL. Consultar detalhes, histórico ou fila nunca decide itens.

`GET /api/aprovacoes/pendentes?page=0&size=20` retorna a estrutura de paginação já existente, restrita a PENDENTE_APROVACAO e à alçada do aprovador, com 20 registros por página e ordenação estável. Comercial recebe ACESSO_NEGADO.

Testes: núcleo puro da consolidação sem mocks; serviço/repositório em PostgreSQL isolado com identidade mockada e Clock fixo; controladores mockam somente CotacoesService; aceite HTTP combina filtros, identidade Spring, serviço e banco reais com sessões sintéticas apenas em testes. Falha induzida no segundo registro de auditoria e duas confirmações concorrentes verificam atomicidade.
