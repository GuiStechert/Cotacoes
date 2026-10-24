# Rascunho, consulta e envio — serviço Java

Entradas tipadas: cliente UUID, datas, juros anuais não negativos, área opcional positiva e lista não vazia de produtos/quantidades/preços positivos. Preço arredondado CEILING(2). Os custos não fazem parte do comando. Erros de domínio possuem código estável: NAO_AUTENTICADO, ACESSO_NEGADO, DADOS_INVALIDOS, CLIENTE_INEXISTENTE, PRODUTO_INDISPONIVEL, COTACAO_INEXISTENTE, ESTADO_INVALIDO, CUSTO_AUSENTE.

A identidade injetável fornece somente a conta corporativa já autenticada; o perfil e o UUID são sempre lidos de usuarios, exigindo cadastro ativo. Nenhum método aceita perfil ou solicitante informado pelo consumidor. A integração NTLM e os endpoints permanecem para a próxima etapa.

Somente Comercial cria/edita/envia seus rascunhos. Consulta Comercial restringe-se às próprias cotações e usa DTO sem custo/margem real. Aprovadores consultam somente cotações atribuídas ao seu perfil e recebem DTO próprio, sem indicador comercial.

Criar/editar valida o cadastro e persiste rascunho, sem exigir histórico de custos existente. Na consulta de rascunho, custos comerciais vigentes são recalculados; margem ponderada comercial é nula se faltar custo comercial. No envio, ambos os históricos são obrigatórios, independentes e selecionados por data DESC/id DESC. Envio grava os snapshots e mantém TODOS os itens PENDENTE, direcionando a cotação ao maior nível requerido individualmente. Consulta após envio usa somente snapshots.

Operações são transacionais em REPEATABLE_READ. Editar e enviar bloqueiam a linha da cotação com FOR UPDATE; submissão repetida não sobrescreve snapshots. Em disputa simultânea o PostgreSQL também pode abortar uma transação por serialização, exigindo tratamento HTTP futuro. Exceções fazem rollback integral. Data/hora usa Clock injetável.

Testes: serviço real, repositório JDBC real e PostgreSQL isolado; identidade mockada e relógio fixo. DTOs são serializados com Jackson para verificar ausência de campos restritos. Não há API HTTP nesta entrega, portanto tipos JSON inválidos serão testados no contrato HTTP futuro.
