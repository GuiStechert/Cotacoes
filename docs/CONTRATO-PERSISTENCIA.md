# Contrato da primeira migração

O esquema contém as oito tabelas do README, UUIDs fornecidos pela aplicação, datas com fuso e valores financeiros NUMERIC sem redução silenciosa de escala. Preços persistidos precisam ter no máximo duas casas. Margens permanecem precisas; o arredondamento percentual é apenas de apresentação.

Históricos de custos aceitam apenas inserção. A consulta vigente ordena cada histórico independentemente por data e UUID descendentes. Snapshots pertencem aos itens e não são recalculados por alteração do cadastro. Exclusões de entidades referenciadas são bloqueadas. Versões de cotação permitem futuro bloqueio otimista.

Os testes usam PostgreSQL real em localhost, porta 55432, num cluster descartável em `.tools/pg-test`. Cada teste cria seu próprio schema e o remove ao terminar. Nenhum teste usa o DATABASE_URL corporativo. Não há mocks: as restrições precisam ser exercitadas no banco real.

Autorização por usuário, envio atômico, decisões e auditoria serão implementados e testados na camada Java; o esquema sozinho não substitui esses contratos. Não foi definida precisão agronômica, portanto a primeira migração não inventa arredondamento de quantidade.
