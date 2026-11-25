CREATE TABLE receita (
    id UUID PRIMARY KEY,
    versao BIGINT NOT NULL,
    titulo VARCHAR(120) NOT NULL,
    descricao VARCHAR(1000) NOT NULL
);

CREATE TABLE receita_ingrediente (
    receita_id UUID NOT NULL REFERENCES receita (id) ON DELETE CASCADE,
    posicao INTEGER NOT NULL CHECK (posicao >= 0),
    texto VARCHAR(300) NOT NULL,
    PRIMARY KEY (receita_id, posicao)
);

CREATE TABLE receita_etapa (
    receita_id UUID NOT NULL REFERENCES receita (id) ON DELETE CASCADE,
    posicao INTEGER NOT NULL CHECK (posicao >= 0),
    texto VARCHAR(1000) NOT NULL,
    PRIMARY KEY (receita_id, posicao)
);
