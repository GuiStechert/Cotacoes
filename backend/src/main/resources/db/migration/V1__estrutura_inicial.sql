CREATE TABLE usuarios (
 id UUID PRIMARY KEY,
 email TEXT NOT NULL CHECK (btrim(email) <> ''),
 conta_windows TEXT NOT NULL CHECK (btrim(conta_windows) <> ''),
 nome TEXT NOT NULL CHECK (btrim(nome) <> ''),
 perfil TEXT NOT NULL CHECK (perfil IN ('COMERCIAL','GESTOR','SUPERVISOR','GERENTE')),
 ativo BOOLEAN NOT NULL DEFAULT TRUE,
 criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX usuarios_email_unico ON usuarios(lower(email));
CREATE UNIQUE INDEX usuarios_conta_unica ON usuarios(lower(conta_windows));

CREATE TABLE clientes (
 id UUID PRIMARY KEY,
 nome TEXT NOT NULL CHECK (btrim(nome) <> ''),
 criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE produtos (
 id UUID PRIMARY KEY,
 nome TEXT NOT NULL CHECK (btrim(nome) <> ''),
 principio_ativo TEXT,
 embalagem NUMERIC CHECK (embalagem > 0),
 dose_recomendada NUMERIC CHECK (dose_recomendada > 0),
 aplicacoes_recomendadas NUMERIC CHECK (aplicacoes_recomendadas > 0),
 unidade_medida TEXT NOT NULL CHECK (btrim(unidade_medida) <> ''),
 margem_recomendada NUMERIC CHECK (margem_recomendada >= 0),
 data_insercao TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 data_desativacao TIMESTAMPTZ,
 ativo BOOLEAN NOT NULL DEFAULT TRUE,
 CHECK (data_desativacao IS NULL OR data_desativacao >= data_insercao)
);

CREATE TABLE produto_custos (
 id UUID PRIMARY KEY,
 produto_id UUID NOT NULL REFERENCES produtos(id),
 custo NUMERIC NOT NULL CHECK (custo >= 0 AND custo < 'Infinity'::numeric),
 data_criacao TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX produto_custos_vigente ON produto_custos(produto_id,data_criacao DESC,id DESC);
CREATE TABLE produto_custos_reais (
 id UUID PRIMARY KEY,
 produto_id UUID NOT NULL REFERENCES produtos(id),
 custo_real NUMERIC NOT NULL CHECK (custo_real >= 0 AND custo_real < 'Infinity'::numeric),
 data_criacao TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX produto_custos_reais_vigente ON produto_custos_reais(produto_id,data_criacao DESC,id DESC);

CREATE FUNCTION impedir_alteracao_historico() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 RAISE EXCEPTION 'Histórico é imutável: insira um novo registro' USING ERRCODE='55000';
END;
$$;
CREATE TRIGGER custos_imutaveis BEFORE UPDATE OR DELETE OR TRUNCATE ON produto_custos
 FOR EACH STATEMENT EXECUTE FUNCTION impedir_alteracao_historico();
CREATE TRIGGER custos_reais_imutaveis BEFORE UPDATE OR DELETE OR TRUNCATE ON produto_custos_reais
 FOR EACH STATEMENT EXECUTE FUNCTION impedir_alteracao_historico();

CREATE TABLE cotacoes (
 id UUID PRIMARY KEY,
 cliente_id UUID NOT NULL REFERENCES clientes(id),
 solicitante_id UUID NOT NULL REFERENCES usuarios(id),
 data_faturamento DATE NOT NULL,
 data_vencimento DATE NOT NULL CHECK (data_vencimento >= data_faturamento),
 juros_anual NUMERIC NOT NULL CHECK (juros_anual >= 0 AND juros_anual < 'Infinity'::numeric),
 area_cliente NUMERIC CHECK (area_cliente > 0 AND area_cliente < 'Infinity'::numeric),
 status TEXT NOT NULL DEFAULT 'RASCUNHO' CHECK (status IN ('RASCUNHO','PENDENTE_APROVACAO','APROVADA','APROVADA_PARCIAL','REPROVADA')),
 perfil_aprovador TEXT CHECK (perfil_aprovador IN ('GESTOR','SUPERVISOR','GERENTE')),
 criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 versao BIGINT NOT NULL DEFAULT 0 CHECK (versao >= 0),
 CHECK ((status='RASCUNHO' AND perfil_aprovador IS NULL) OR (status<>'RASCUNHO' AND perfil_aprovador IS NOT NULL))
);
CREATE INDEX cotacoes_listagem ON cotacoes(criado_em DESC,id DESC);
CREATE INDEX cotacoes_solicitante ON cotacoes(solicitante_id,criado_em DESC,id DESC);
CREATE INDEX cotacoes_fila ON cotacoes(perfil_aprovador,status,criado_em DESC,id DESC);
CREATE INDEX cotacoes_cliente ON cotacoes(cliente_id);

CREATE TABLE cotacao_itens (
 id UUID PRIMARY KEY,
 cotacao_id UUID NOT NULL REFERENCES cotacoes(id),
 produto_id UUID NOT NULL REFERENCES produtos(id),
 quantidade NUMERIC NOT NULL CHECK (quantidade > 0 AND quantidade < 'Infinity'::numeric),
 preco NUMERIC NOT NULL CHECK (preco > 0 AND preco < 'Infinity'::numeric AND preco=round(preco,2)),
 custo NUMERIC CHECK (custo >= 0 AND custo < 'Infinity'::numeric),
 custo_real NUMERIC CHECK (custo_real >= 0 AND custo_real < 'Infinity'::numeric),
 margem NUMERIC CHECK (margem >= 0 AND margem < 'Infinity'::numeric),
 margem_real NUMERIC CHECK (margem_real >= 0 AND margem_real < 'Infinity'::numeric),
 status TEXT NOT NULL DEFAULT 'RASCUNHO' CHECK (status IN ('RASCUNHO','PENDENTE','APROVADO','REPROVADO')),
 comentario TEXT,
 CHECK (status='RASCUNHO' OR (custo IS NOT NULL AND custo_real IS NOT NULL AND margem IS NOT NULL AND margem_real IS NOT NULL))
);
CREATE INDEX cotacao_itens_cotacao ON cotacao_itens(cotacao_id);
CREATE INDEX cotacao_itens_produto ON cotacao_itens(produto_id);

CREATE TABLE historico_aprovacoes (
 id UUID PRIMARY KEY,
 item_id UUID NOT NULL REFERENCES cotacao_itens(id),
 usuario_id UUID NOT NULL REFERENCES usuarios(id),
 decisao TEXT NOT NULL CHECK (decisao IN ('APROVADO','REPROVADO')),
 comentario TEXT,
 criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(item_id)
);
CREATE INDEX historico_aprovacoes_usuario ON historico_aprovacoes(usuario_id);
CREATE TRIGGER aprovacoes_imutaveis BEFORE UPDATE OR DELETE OR TRUNCATE ON historico_aprovacoes
 FOR EACH STATEMENT EXECUTE FUNCTION impedir_alteracao_historico();
