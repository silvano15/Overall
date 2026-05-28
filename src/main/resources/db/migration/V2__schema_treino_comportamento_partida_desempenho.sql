CREATE TABLE treino (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    jogador_id    BIGINT       NOT NULL,
    data_treino   DATE         NOT NULL,
    acertos       INT          NOT NULL,
    erros         INT          NOT NULL,
    criado_em     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_treino PRIMARY KEY (id),
    CONSTRAINT fk_treino_jogador FOREIGN KEY (jogador_id) REFERENCES jogador (id)
        ON DELETE RESTRICT,
    CONSTRAINT uk_treino_jogador_data UNIQUE (jogador_id, data_treino)
);

CREATE TABLE comportamento (
    id              BIGINT     NOT NULL AUTO_INCREMENT,
    jogador_id      BIGINT     NOT NULL,
    data_avaliacao  DATE       NOT NULL,
    nota_foco       INT        NOT NULL,
    nota_disciplina INT        NOT NULL,
    nota_atitude    INT        NOT NULL,
    criado_em       TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_comportamento PRIMARY KEY (id),
    CONSTRAINT fk_comportamento_jogador FOREIGN KEY (jogador_id) REFERENCES jogador (id)
        ON DELETE RESTRICT,
    CONSTRAINT uk_comportamento_jogador_data UNIQUE (jogador_id, data_avaliacao)
);

CREATE TABLE partida (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    adversario    VARCHAR(120)  NOT NULL,
    data_jogo     DATE          NOT NULL,
    gols_pro      INT           NULL,
    gols_contra   INT           NULL,
    local_jogo    VARCHAR(120)  NULL,
    criado_em     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_partida PRIMARY KEY (id),
    CONSTRAINT uk_partida_data UNIQUE (data_jogo)
);

CREATE TABLE desempenho (
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    partida_id          BIGINT         NOT NULL,
    jogador_id          BIGINT         NOT NULL,
    nota                DECIMAL(4,2)   NOT NULL,
    gols                INT            NOT NULL DEFAULT 0,
    assistencias        INT            NOT NULL DEFAULT 0,
    cartoes_amarelos    INT            NOT NULL DEFAULT 0,
    cartoes_vermelhos   INT            NOT NULL DEFAULT 0,
    criado_em           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_desempenho PRIMARY KEY (id),
    CONSTRAINT fk_desempenho_partida FOREIGN KEY (partida_id) REFERENCES partida (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_desempenho_jogador FOREIGN KEY (jogador_id) REFERENCES jogador (id)
        ON DELETE RESTRICT,
    CONSTRAINT uk_desempenho_partida_jogador UNIQUE (partida_id, jogador_id)
);
