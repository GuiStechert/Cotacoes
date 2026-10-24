# AGENTS.md — Instruções obrigatórias para agentes de desenvolvimento

## 1. Escopo e precedência

Este arquivo estabelece o processo obrigatório de desenvolvimento do Sistema de Cotações e Aprovação de Margens. Consulte `README.md` para requisitos, fórmulas, perfis, estados, regras de alçada, arquitetura e identidade visual. Não invente regras de negócio quando houver ambiguidade: registre a dúvida e solicite esclarecimento antes de implementar.

**Regra principal: testes primeiro, implementação depois.** Não escreva código de produção de uma funcionalidade antes de definir seus contratos, preparar os mocks e escrever seus testes.

## 2. Processo obrigatório — Test-Driven Development (TDD)

Para cada nova função, método, endpoint, componente ou fluxo:

1. **Defina o contrato**: entradas, saídas, tipos, validações, dependências, efeitos colaterais, estados possíveis e erros esperados. Use as regras do `README.md`.
2. **Planeje os mocks**: identifique os pontos de entrada e as dependências externas. Faça mocks de todas as funcionalidades ou fluxos que ainda não tenham implementação, no nível mais alto apropriado (ver seção 3).
3. **Escreva todos os testes antes da implementação**: inclua casos válidos, limites, casos extremos, tipos inválidos, valores nulos e mensagens/códigos de erro. Verifique tanto resultados corretos quanto falhas corretas.
4. **Execute os testes e confirme a falha inicial esperada (RED)**: testes novos devem falhar por ausência do comportamento a implementar, não por falhas de configuração, importação ou sintaxe do próprio teste.
5. **Implemente somente o necessário para passar nos testes (GREEN)**.
6. **Execute novamente todos os testes afetados e a suíte completa aplicável**. Corrija o código de produção até que passem.
7. **Refatore somente o código de produção**, mantendo o comportamento e os testes existentes intactos; execute novamente os testes.
8. **Registre a evidência**: comandos executados, quantidade de testes e resultado. Nunca declare que testes passaram sem executá-los.

É proibido começar pela implementação e escrever testes depois para apenas confirmar o comportamento existente.

## 3. Política de mocks

- Prepare um mock para cada **ponto de entrada funcional** sob teste e para cada **fronteira externa necessária**, como banco de dados, relógio, autenticação futura, chamadas HTTP e serviços de terceiros.
- **Se uma função de nível superior utiliza outras funções internas, faça o mock apenas da função de nível superior ao simular aquele fluxo**. Não duplique mocks de funções internas chamadas por ela.
- Ao testar a função de nível superior de verdade, **não a substitua por um mock**: execute a implementação real e simule apenas suas dependências externas. Isso evita testes que validam somente o comportamento de um mock.
- Funções internas com regras próprias também devem receber testes unitários diretos quando fizer sentido, sem exigir mocks redundantes em testes de integração do fluxo superior.
- Configure respostas, erros e interações esperadas dos mocks explicitamente. Não use mocks que retornam sucesso genérico para qualquer entrada.
- Prefira dependências injetáveis e contratos claros, evitando dependências globais ou acesso direto a infraestrutura dentro de regras de negócio.

**Exemplo:** se `enviarCotacao()` chama `calcularMargens()`, `determinarAlcada()` e `atualizarStatus()`, o teste do fluxo deve exercitar `enviarCotacao()` e observar seus efeitos, sem criar mocks separados das três funções internas. Simule o repositório ou serviço externo que o fluxo utiliza. Em um teste de um consumidor externo de `enviarCotacao()`, pode-se simular somente `enviarCotacao()`.

## 4. Cobertura obrigatória dos testes

Para cada contrato, considere e teste, quando aplicável:

- **Caminho de sucesso:** entradas válidas, resultados exatos, mudanças de estado e efeitos persistidos.
- **Limites:** valores imediatamente abaixo, iguais e imediatamente acima de cada limite; mínimo, máximo, zero, datas de virada de mês/ano e arredondamento.
- **Nulos e ausentes:** `null`, campos ausentes, listas vazias, strings vazias e valores opcionais omitidos.
- **Tipos inválidos:** texto onde se espera número, números onde se espera data, booleanos, objetos/arrays inesperados, valores não finitos quando relevantes e enumerações desconhecidas.
- **Dados inválidos:** números negativos proibidos, quantidade zero, preço zero, datas inconsistentes, referências inexistentes, duplicações e dados malformados.
- **Erros esperados:** tipo/classe de exceção, código HTTP ou código de domínio, campo relacionado e mensagem quando fizer parte do contrato público. Não basta verificar que “ocorreu um erro”.
- **Permissões:** acesso autorizado, negado e tentativa de acessar campos sensíveis ou executar ações de outro perfil.
- **Persistência e concorrência:** rollback em falhas, operações atômicas, duplicidade de submissão e decisões simultâneas.
- **Interface:** visibilidade por perfil, checkbox inicialmente marcada, comentário exibido apenas quando desmarcada, comportamento de confirmação e tratamento de erros da API.

Cada teste deve ser determinístico, isolado e reproduzível; não depender da ordem de execução, da rede corporativa ou de dados mutáveis de produção.

## 5. Imutabilidade dos testes

**Depois de escritos e validados como especificação, os testes existentes não devem ser alterados, enfraquecidos, removidos, ignorados ou marcados como `skip` para fazer uma implementação passar.**

- Não altere expectativas, entradas, mensagens esperadas ou asserts para acomodar um defeito no código.
- Não substitua testes precisos por testes mais permissivos.
- Corrija a implementação quando um teste falhar.
- Novos requisitos devem gerar **novos testes**, preservando os anteriores.
- Se um requisito novo contradisser um teste existente ou um teste estiver objetivamente incorreto, **interrompa a alteração e peça autorização explícita** para revisar a especificação e o teste. Não faça mudanças silenciosas.

## 6. Política obrigatória de regressão de bugs

Todo bug encontrado durante o projeto deverá resultar em um teste de regressão permanente:

1. Documente o cenário, a entrada que reproduz o defeito, o resultado incorreto e o comportamento correto esperado.
2. Escreva **primeiro** um novo teste que reproduza o bug.
3. Execute o teste e confirme que falha pelo motivo esperado.
4. Corrija o código de produção sem modificar os testes anteriores.
5. Execute o teste novo, os testes relacionados e a suíte aplicável até todos passarem.
6. Preserve o teste de regressão para impedir a reincidência do defeito.

Não considere um bug resolvido sem seu teste de regressão, salvo impossibilidade técnica documentada e explicitamente aprovada.

## 7. Casos críticos específicos deste sistema

Os testes devem contemplar, no mínimo, os contratos aplicáveis definidos no `README.md`:

- **Juros:** contagem inclusiva de meses; janeiro a agosto = 8; virada de ano; taxa anual dividida por 12; juros simples; datas inválidas.
- **Margens:** fórmula `custo * fator_juros / preco`; cálculo com custo comercial e custo real; divisão por zero; precisão decimal; quantidade inválida.
- **Arredondamento:** preço exibido com duas casas decimais e arredondamento para cima, incluindo valores já exatos e casos na terceira casa decimal.
- **Médias ponderadas:** soma de `margem * quantidade` dividida pela soma das quantidades, separadamente para margem comercial e real; quantidade total zero.
- **Preço total:** soma de `preco * quantidade` de todos os itens.
- **Alçadas por margem real individual:** Gestor para margem real >= 10%; Supervisor para 8% <= margem real < 10%; Gerente para margem real < 8%; testar exatamente 8% e 10% e valores adjacentes.
- **Roteamento:** usar a maior alçada entre os produtos; encaminhar diretamente ao único aprovador competente, sem etapas intermediárias; aprovador de maior alçada pode decidir todos os produtos da cotação.
- **Estados:** `RASCUNHO`, `PENDENTE_APROVACAO`, `APROVADA`, `APROVADA_PARCIAL`, `REPROVADA` para cotação; `RASCUNHO`, `PENDENTE`, `APROVADO`, `REPROVADO` para itens. Testar transições válidas e inválidas e consolidação de decisões mistas.
- **Aprovação:** nenhum item é aprovado automaticamente apenas pela margem; checkbox marcada por padrão não persiste aprovação antes da confirmação; desmarcar significa reprovar; comentário opcional aparece somente para item desmarcado.
- **Segurança:** Comercial não recebe `custo_real` nem `margem_real`; aprovadores podem visualizar margem real; permissões aplicadas no backend. Autenticação corporativa ainda não definida: não assumir integração pronta.
- **Custos:** obtidos da tabela de produtos, não confiados ao payload do Comercial.

Quando houver conflito entre esta lista e uma mudança aprovada nos requisitos, registrar a mudança e seguir a regra de imutabilidade dos testes.

## 8. Ferramentas e organização

- **Backend Java/Spring Boot:** JUnit 5, Mockito e testes de integração do Spring conforme necessário; `BigDecimal` para valores financeiros.
- **Frontend Angular/TypeScript:** usar o runner de testes configurado no projeto, com mocks de API e testes de componentes e serviços.
- **Banco PostgreSQL:** testar consultas, restrições e transações em ambiente isolado quando a regra depender de comportamento real do banco; mocks não substituem todos os testes de integração.
- Nomeie testes por cenário e comportamento esperado, por exemplo: `deveEncaminharAoGerenteQuandoUmProdutoTemMargemRealInferiorA8()`.
- Organize testes próximos à camada testada, mantendo fixtures pequenas e legíveis.

## 9. Critério de conclusão (Definition of Done)

Uma funcionalidade só está concluída quando:

- [ ] Seu contrato e suas regras de negócio estão claros.
- [ ] Os mocks necessários foram preparados sem duplicação de funções internas.
- [ ] Os testes foram escritos **antes** do código de produção.
- [ ] Foram cobertos sucesso, limites, nulos, tipos inválidos e erros esperados aplicáveis.
- [ ] A falha inicial dos novos testes foi verificada.
- [ ] O código de produção foi implementado.
- [ ] Todos os testes relacionados e a suíte aplicável passaram.
- [ ] Nenhum teste anterior foi modificado para obter sucesso.
- [ ] Bugs descobertos possuem testes de regressão.
- [ ] Os resultados dos testes foram registrados e limitações conhecidas foram informadas.

**Nunca contorne testes para entregar uma funcionalidade.**
