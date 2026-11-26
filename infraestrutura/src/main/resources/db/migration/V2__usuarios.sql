CREATE TABLE usuario (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL CHECK (email = lower(email)),
    nome_usuario VARCHAR(20) NOT NULL,
    nome_chave VARCHAR(20) NOT NULL CHECK (nome_chave = lower(nome_usuario)),
    senha_hash VARCHAR(100) NOT NULL,
    papel VARCHAR(5) NOT NULL CHECK (papel IN ('USER', 'ADMIN')),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT uk_usuario_nome UNIQUE (nome_chave)
);
