ALTER TABLE receita ADD COLUMN dono_id UUID REFERENCES usuario (id);
CREATE INDEX ix_receita_dono ON receita (dono_id);
