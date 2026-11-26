CREATE TABLE receita_categoria (
    receita_id UUID NOT NULL REFERENCES receita (id) ON DELETE CASCADE,
    categoria_id UUID NOT NULL REFERENCES categoria (id),
    PRIMARY KEY (receita_id, categoria_id)
);

CREATE INDEX ix_receita_categoria_categoria ON receita_categoria (categoria_id, receita_id);
