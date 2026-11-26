ALTER TABLE categoria ADD COLUMN posicao BIGINT;

UPDATE categoria c SET posicao = (
    SELECT ordem.posicao
    FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY nome_chave) - 1 AS posicao FROM categoria) ordem
    WHERE ordem.id = c.id
);

ALTER TABLE categoria ALTER COLUMN posicao SET NOT NULL;

CREATE TABLE catalogo_categorias (
    id INTEGER PRIMARY KEY CHECK (id = 1)
);

INSERT INTO catalogo_categorias (id) VALUES (1);
