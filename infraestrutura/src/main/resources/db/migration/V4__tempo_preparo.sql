ALTER TABLE receita ADD COLUMN tempo_minutos INTEGER CHECK (tempo_minutos BETWEEN 0 AND 5999);
