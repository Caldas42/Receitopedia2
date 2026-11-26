CREATE TABLE seguimento (
    seguidor_id UUID NOT NULL REFERENCES usuario (id),
    seguido_id UUID NOT NULL REFERENCES usuario (id),
    PRIMARY KEY (seguidor_id, seguido_id),
    CHECK (seguidor_id <> seguido_id)
);

ALTER TABLE chat_conversa ADD COLUMN dm_ativa BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE chat_conversa
SET dm_primeiro = dm_segundo, dm_segundo = dm_primeiro
WHERE tipo = 'DM' AND dm_primeiro > dm_segundo;
ALTER TABLE chat_conversa ADD CONSTRAINT ck_chat_dm_par_ordenado
    CHECK (tipo <> 'DM' OR dm_primeiro < dm_segundo);
