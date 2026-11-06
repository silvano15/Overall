-- =====================================================================
--  Grupos de pelada: cada grupo tem seu próprio elenco de jogadores,
--  suas próprias peladas e seu próprio ranking, isolados dos demais
--  (ex.: "Pelada do trabalho" e "Pelada da rua" não se misturam).
--
--  Migra o que já existia (jogadores e peladas cadastrados antes dos
--  grupos existirem) para um grupo padrão, criado abaixo.
-- =====================================================================

CREATE TABLE grupo_pelada (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    nome       VARCHAR(80)  NOT NULL,
    criado_em  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_grupo_pelada PRIMARY KEY (id)
);

INSERT INTO grupo_pelada (nome) VALUES ('Minha Pelada');

-- ---------------------------------------------------------------- jogador
ALTER TABLE jogador ADD COLUMN grupo_id BIGINT NULL;
UPDATE jogador SET grupo_id = (SELECT id FROM grupo_pelada ORDER BY id ASC LIMIT 1) WHERE grupo_id IS NULL;
ALTER TABLE jogador MODIFY COLUMN grupo_id BIGINT NOT NULL;
ALTER TABLE jogador ADD CONSTRAINT fk_jogador_grupo FOREIGN KEY (grupo_id) REFERENCES grupo_pelada (id);
CREATE INDEX ix_jogador_grupo ON jogador (grupo_id);

-- ---------------------------------------------------------------- pelada
ALTER TABLE pelada ADD COLUMN grupo_id BIGINT NULL;
UPDATE pelada SET grupo_id = (SELECT id FROM grupo_pelada ORDER BY id ASC LIMIT 1) WHERE grupo_id IS NULL;
ALTER TABLE pelada MODIFY COLUMN grupo_id BIGINT NOT NULL;
ALTER TABLE pelada ADD CONSTRAINT fk_pelada_grupo FOREIGN KEY (grupo_id) REFERENCES grupo_pelada (id);
CREATE INDEX ix_pelada_grupo ON pelada (grupo_id);
