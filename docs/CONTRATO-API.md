# API, paginação e duplicação

Rotas desta entrega: POST/GET `/api/cotacoes`, GET/PUT `/api/cotacoes/{id}`, POST `/api/cotacoes/{id}/enviar`, POST `/api/cotacoes/{id}/duplicar`. Criação/duplicação retornam 201, corpo `{id}` e Location; edição/envio retornam 204. Consulta conserva DTOs separados por perfil.

Listagem: `page` inicia em zero, `size` é obrigatoriamente 20 (ambos têm padrão). Filtros opcionais `status`, `clienteId`, `inicio`, `fim`. O período refere-se à criação, com datas inclusivas no fuso America/Cuiaba em execução normal; o serviço usa o fuso do Clock injetado. Ordenação `criado_em DESC, id DESC`; paginação e total calculados no banco, na mesma transação. Páginas além do fim são vazias. Página negativa, tamanho diferente de 20, enum desconhecido e período invertido resultam em DADOS_INVALIDOS.

Comercial lista somente suas cotações; aprovador lista somente as atribuídas ao seu perfil. Resumos incluem cliente/solicitante, estado, alçada e preço total, sem margens ou custos. Ações são sinalizadas: editar somente rascunho próprio; duplicar somente não rascunho e Comercial autorizado.

Duplicação: somente Comercial e cotação própria não rascunho. Copia os dados de entrada, gera UUIDs novos, remove snapshots/decisões/comentários e não altera o original. Valida cadastro vigente e produtos ativos como qualquer nova inclusão; consulta/envio do novo rascunho usa custos atuais. Falha é atômica.

Erros HTTP têm corpo `{codigo}`: 400 para formato/tipo/validação; 401 sem autenticação; 403 para acesso negado; 404 cotação inexistente; 409 estado inválido ou conflito de concorrência; 500 genérico sem SQL/stack trace. Payload rejeita campos desconhecidos e coerção de texto/booleano para números. Erros não refletem dados sensíveis.

Autenticação: Spring Security exige contexto autenticado; identidade vem do SecurityContext, nunca de X-Usuario ou perfil no payload. Form login e Basic permanecem desabilitados, sem senha gerada. Não há simulador de usuário fora de testes. CSRF permanece ativo e tokens podem ser obtidos em GET `/api/csrf`; sessão autenticada é necessária para as outras rotas. A integração Waffle/NTLM ainda precisa fornecer o contexto autenticado e será tratada em etapa própria. Por padrão o servidor escuta apenas 127.0.0.1.

Testes de controlador mockam somente o serviço completo. Testes de serviço/paginação exercitam PostgreSQL real isolado, mockando somente identidade e fixando o Clock. Testes de segurança usam contexto/sessão sintéticos exclusivamente no ambiente de teste.

Decisões, histórico e fila de aprovação foram acrescentados conforme `docs/CONTRATO-APROVACAO.md`. JSON também rejeita números e booleanos em campos de texto, sem convertê-los silenciosamente em comentários.
