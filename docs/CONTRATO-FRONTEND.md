# Angular — três telas

Angular 21 com Vitest, compatível com Node 24.14 instalado. Rotas: `/cotacoes`, `/cotacao/:id` (inclui id `nova`) e `/aprovacao/:id`. Detalhe e histórico permanecem nessas telas; autenticação/erro são estados da moldura, sem quarta tela. Paleta ADM em tokens CSS; layout responsivo, rótulos de estado textuais e campos acessíveis.

Autorização: GET usuario antes de CSRF. Guards restringem cotação ao Comercial e aprovação aos três aprovadores; o backend continua sendo a autoridade. Nenhuma impersonação, perfil escolhido pelo navegador, custo real comercial ou banco direto no cliente. Chamadas relativas `/api` na mesma origem, sem repetição automática de escritas e sem refletir mensagens sensíveis de erros.

Lista: filtros por estado/cliente/período; paginação do backend em 20 linhas, total e anterior/próxima. Editar somente rascunho permitido; duplicar somente estados não rascunho permitidos. Fila do aprovador é um modo da mesma listagem.

Cotação: criar/editar rascunho, cliente/datas/juros percentuais/área/itens. Custos nunca integram o comando. Preço CEILING(2), total estimado decimal no cliente e margens atualizadas pelo backend ao salvar. Exibir margem comercial somente. Confirmação de envio precede a chamada de envio; cotações enviadas ficam somente leitura.

Definição confirmada pelo usuário: dose e quantidade na unidade do produto, sem conversão por embalagem; quantidade manual aceita duas casas, e recomendação `área*dose*aplicações` arredonda SEMPRE PARA CIMA em duas casas. Botão só aparece com área/dose/aplicações positivas e rascunho editável; nada é preenchido automaticamente. Backend também deve rejeitar quantidade com precisão efetiva superior a duas casas.

Aprovação: pendentes começam marcados, sem gravar nada. Desmarcar mostra comentário opcional. Confirmar inclui TODOS os pendentes uma vez, com Boolean explícito; itens decididos são somente leitura. Confirmar abre revisão local; somente a confirmação final persiste. Conflitos não disparam retry de escrita. Histórico acessível dentro da tela.

Testes puros para validação/decimais e erros; serviço HTTP com HttpTestingController; componentes mockam apenas o serviço de API e sessão; testes de guards/perfis. RED de comportamento antes da implementação. QA visual pode usar servidor de respostas fictícias em `.tools` apenas para testes, nunca incluído no bundle ou no backend.
