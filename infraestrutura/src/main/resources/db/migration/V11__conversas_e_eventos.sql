CREATE TABLE chat_conversa (
    id UUID PRIMARY KEY,
    tipo VARCHAR(8) NOT NULL CHECK (tipo IN ('DM', 'GRUPO')),
    nome VARCHAR(80),
    dm_primeiro UUID REFERENCES usuario (id),
    dm_segundo UUID REFERENCES usuario (id),
    ultima_seq BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_chat_dm UNIQUE (dm_primeiro, dm_segundo),
    CHECK ((tipo = 'DM' AND nome IS NULL AND dm_primeiro IS NOT NULL AND dm_segundo IS NOT NULL
             AND dm_primeiro <> dm_segundo)
        OR (tipo = 'GRUPO' AND nome IS NOT NULL AND dm_primeiro IS NULL AND dm_segundo IS NULL))
);

CREATE TABLE chat_membro (
    conversa_id UUID NOT NULL REFERENCES chat_conversa (id),
    usuario_id UUID NOT NULL REFERENCES usuario (id),
    admin BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (conversa_id, usuario_id)
);
CREATE INDEX ix_chat_membro_usuario ON chat_membro (usuario_id, ativo, conversa_id);

CREATE TABLE chat_bloqueio (
    autor_id UUID NOT NULL REFERENCES usuario (id),
    alvo_id UUID NOT NULL REFERENCES usuario (id),
    PRIMARY KEY (autor_id, alvo_id),
    CHECK (autor_id <> alvo_id)
);

CREATE TABLE chat_mensagem (
    id UUID PRIMARY KEY,
    conversa_id UUID NOT NULL REFERENCES chat_conversa (id),
    autor_id UUID NOT NULL REFERENCES usuario (id),
    sequencia BIGINT NOT NULL,
    texto VARCHAR(2000) NOT NULL,
    texto_chave VARCHAR(4000) NOT NULL,
    enviada_em TIMESTAMP WITH TIME ZONE NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    editada BOOLEAN NOT NULL DEFAULT FALSE,
    apagada BOOLEAN NOT NULL DEFAULT FALSE,
    processada BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_chat_sequencia UNIQUE (conversa_id, sequencia)
);

CREATE TABLE chat_recibo (
    mensagem_id UUID NOT NULL REFERENCES chat_mensagem (id),
    usuario_id UUID NOT NULL REFERENCES usuario (id),
    entregue_em TIMESTAMP WITH TIME ZONE NOT NULL,
    lida_em TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (mensagem_id, usuario_id)
);

CREATE TABLE chat_anexo (
    id UUID PRIMARY KEY,
    mensagem_id UUID NOT NULL REFERENCES chat_mensagem (id),
    tipo VARCHAR(4) NOT NULL CHECK (tipo IN ('JPEG', 'PNG', 'WEBP')),
    miniatura BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX ix_chat_anexo_mensagem ON chat_anexo (mensagem_id);

CREATE TABLE evento_outbox (
    id UUID PRIMARY KEY,
    tipo VARCHAR(80) NOT NULL,
    agregado UUID NOT NULL,
    referencia UUID,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    publicado_em TIMESTAMP WITH TIME ZONE
);
CREATE INDEX ix_outbox_publicacao ON evento_outbox (publicado_em, criado_em);

CREATE TABLE evento_processado (
    id UUID PRIMARY KEY,
    processado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
