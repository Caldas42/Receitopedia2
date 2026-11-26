CREATE TABLE receita_foto (
    receita_id UUID NOT NULL REFERENCES receita (id) ON DELETE CASCADE,
    posicao INTEGER NOT NULL CHECK (posicao BETWEEN 0 AND 4),
    foto_id UUID NOT NULL,
    tipo VARCHAR(4) NOT NULL CHECK (tipo IN ('JPEG', 'PNG', 'WEBP')),
    PRIMARY KEY (receita_id, posicao)
);

CREATE TABLE foto_exclusao_pendente (
    id UUID PRIMARY KEY
);
