CREATE TABLE categoria (
    id UUID PRIMARY KEY,
    nome VARCHAR(30) NOT NULL CHECK (char_length(nome) BETWEEN 2 AND 30),
    nome_chave VARCHAR(60) NOT NULL,
    CONSTRAINT uk_categoria_nome_chave UNIQUE (nome_chave)
);
