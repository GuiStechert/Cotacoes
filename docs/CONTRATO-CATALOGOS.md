# Consultas para as telas

GET `/api/clientes` retorna lista de `{id,nome}`. GET `/api/produtos` retorna todos os produtos, incluindo indicador ativo e datas de ativação/desativação, para permitir resolver nomes de produtos em cotações históricas. Novas inclusões continuam exigindo produto ativo no serviço de cotações. Ordenação estável em ambos: lower(nome), id.

Produto Comercial: id, nome, principioAtivo, embalagem, doseRecomendada, aplicacoesRecomendadas, unidadeMedida, margemRecomendada, dataInsercao, dataDesativacao, ativo. Não retorna custos nem histórico real. Produto Aprovador: os mesmos metadados e custoReal vigente (nulo se ainda não cadastrado), obtido do histórico real por data_criacao DESC/id DESC. O catálogo não calcula margem real sem preço/datas/juros de uma cotação. DTOs distintos impedem campo real presente até como null na resposta Comercial; a consulta Comercial também não acessa a tabela de custos reais.

GET `/api/usuario` retorna apenas id, nome e perfil do próprio usuário cadastrado ativo, para a navegação das três telas. A conta vem do autenticador e o perfil sempre do banco. Estes três endpoints são somente leitura e exigem identidade autenticada e cadastro ativo em qualquer um dos quatro perfis. Ausência de identidade: NAO_AUTENTICADO/401; identidade sem cadastro ativo: ACESSO_NEGADO/403. Não há parâmetro para escolher usuário/perfil.

Sem CRUD de cadastro, alteração de custos ou cálculo/sugestão de quantidade nesta etapa. Unidade e precisão agronômica ainda constam como definições futuras no README; serão esclarecidas antes de implementar a sugestão.

Testes de serviço usam PostgreSQL isolado e identidade mockada, sem mocks de consultas internas. Testes de controlador mockam somente CatalogoService. Aceite integrado usa filtros/identidade Spring/serviço/banco reais, com sessão sintética apenas no teste. Nenhuma consulta altera o cadastro.
