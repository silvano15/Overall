-- =====================================================================
--  Schema do app de pelada.
--  SQL escrito no subconjunto comum de MySQL e H2 (modo MySQL), para que
--  os testes de integração exercitem exatamente estas migrations.
-- =====================================================================

CREATE TABLE usuario (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)     NOT NULL,
    senha           VARCHAR(100)    NOT NULL,           -- hash BCrypt (~60 chars)
    nome_completo   VARCHAR(120)    NOT NULL,
    perfil          VARCHAR(30)     NOT NULL,           -- ADMIN
    ativo           BOOLEAN         NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_username UNIQUE (username)
);

-- Jogador da pelada: só nome e foto (miniatura em data URL). 'goleiro' muda a fórmula do overall.
CREATE TABLE jogador (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    nome        VARCHAR(80)  NOT NULL,
    foto        MEDIUMTEXT   NULL,
    goleiro     BOOLEAN      NOT NULL DEFAULT FALSE,
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    overall     INT          NOT NULL DEFAULT 60,       -- cache: atualizado ao finalizar cada pelada
    criado_em   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_jogador PRIMARY KEY (id)
);

-- Uma "pelada" = um dia de jogo (a lista da semana).
CREATE TABLE pelada (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    data_jogo           DATE         NOT NULL,
    hora_jogo           TIME         NOT NULL,
    status              VARCHAR(20)  NOT NULL,          -- ABERTA | SORTEADA | EM_ANDAMENTO | FINALIZADA
    jogadores_por_time  INT          NULL,
    modo_sorteio        VARCHAR(20)  NULL,              -- ALEATORIO | OVERALL
    duracao_minutos     INT          NULL,              -- última configuração usada (pré-preenche a próxima partida)
    gols_para_encerrar  INT          NULL,
    criado_em           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finalizada_em       TIMESTAMP    NULL,

    CONSTRAINT pk_pelada PRIMARY KEY (id)
);

-- A lista da pelada. Ao finalizar guardamos o overall antes/depois e a nota média do dia.
CREATE TABLE pelada_jogador (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    pelada_id       BIGINT        NOT NULL,
    jogador_id      BIGINT        NOT NULL,
    overall_antes   INT           NULL,
    overall_depois  INT           NULL,
    nota            DECIMAL(4,2)  NULL,

    CONSTRAINT pk_pelada_jogador PRIMARY KEY (id),
    CONSTRAINT fk_pj_pelada  FOREIGN KEY (pelada_id)  REFERENCES pelada (id),
    CONSTRAINT fk_pj_jogador FOREIGN KEY (jogador_id) REFERENCES jogador (id),
    CONSTRAINT uk_pj UNIQUE (pelada_id, jogador_id)
);

CREATE TABLE time_pelada (
    id         BIGINT  NOT NULL AUTO_INCREMENT,
    pelada_id  BIGINT  NOT NULL,
    numero     INT     NOT NULL,

    CONSTRAINT pk_time_pelada PRIMARY KEY (id),
    CONSTRAINT fk_time_pelada FOREIGN KEY (pelada_id) REFERENCES pelada (id),
    CONSTRAINT uk_time_numero UNIQUE (pelada_id, numero)
);

CREATE TABLE time_pelada_jogador (
    id          BIGINT  NOT NULL AUTO_INCREMENT,
    time_id     BIGINT  NOT NULL,
    jogador_id  BIGINT  NOT NULL,

    CONSTRAINT pk_tpj PRIMARY KEY (id),
    CONSTRAINT fk_tpj_time    FOREIGN KEY (time_id)    REFERENCES time_pelada (id),
    CONSTRAINT fk_tpj_jogador FOREIGN KEY (jogador_id) REFERENCES jogador (id),
    CONSTRAINT uk_tpj UNIQUE (time_id, jogador_id)
);

CREATE TABLE partida (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    pelada_id             BIGINT       NOT NULL,
    ordem                 INT          NOT NULL,
    time_a_id             BIGINT       NOT NULL,
    time_b_id             BIGINT       NOT NULL,
    duracao_segundos      INT          NOT NULL,
    gols_para_encerrar    INT          NOT NULL,
    -- AGUARDANDO | EM_ANDAMENTO | PAUSADA | EMPATE_PENDENTE | PENALTIS | ENCERRADA
    status                VARCHAR(20)  NOT NULL,
    acumulado_ms          BIGINT       NOT NULL DEFAULT 0,   -- tempo já jogado, sem contar o trecho em curso
    retomada_em_ms        BIGINT       NULL,                 -- epoch ms de quando o relógio voltou a correr (NULL = parado)
    vencedor_id           BIGINT       NULL,                 -- vencedor no placar (NULL = empate)
    vencedor_penaltis_id  BIGINT       NULL,                 -- quem ficou na quadra após os pênaltis
    penaltis_modo         VARCHAR(20)  NULL,                 -- NORMAL | PREDETERMINADO | ALTERNADO
    penaltis_rodadas      INT          NULL,                 -- cobranças por time antes das alternadas (0 = direto nas alternadas)
    encerrada_em          TIMESTAMP    NULL,

    CONSTRAINT pk_partida PRIMARY KEY (id),
    CONSTRAINT fk_partida_pelada FOREIGN KEY (pelada_id) REFERENCES pelada (id),
    CONSTRAINT fk_partida_time_a FOREIGN KEY (time_a_id) REFERENCES time_pelada (id),
    CONSTRAINT fk_partida_time_b FOREIGN KEY (time_b_id) REFERENCES time_pelada (id),
    CONSTRAINT uk_partida_ordem UNIQUE (pelada_id, ordem)
);

-- Quem participou da partida (escalação inicial + quem entrou depois).
CREATE TABLE partida_jogador (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    partida_id  BIGINT   NOT NULL,
    time_id     BIGINT   NOT NULL,
    jogador_id  BIGINT   NOT NULL,
    goleiro     BOOLEAN  NOT NULL DEFAULT FALSE,
    em_campo    BOOLEAN  NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_partida_jogador PRIMARY KEY (id),
    CONSTRAINT fk_pjg_partida FOREIGN KEY (partida_id) REFERENCES partida (id),
    CONSTRAINT fk_pjg_time    FOREIGN KEY (time_id)    REFERENCES time_pelada (id),
    CONSTRAINT fk_pjg_jogador FOREIGN KEY (jogador_id) REFERENCES jogador (id),
    CONSTRAINT uk_pjg UNIQUE (partida_id, jogador_id)
);

CREATE TABLE gol (
    id             BIGINT     NOT NULL AUTO_INCREMENT,
    partida_id     BIGINT     NOT NULL,
    time_id        BIGINT     NOT NULL,
    autor_id       BIGINT     NOT NULL,
    assistente_id  BIGINT     NULL,                      -- NULL = sem assistência (ex.: pênalti)
    tempo_ms       BIGINT     NOT NULL,
    criado_em      TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_gol PRIMARY KEY (id),
    CONSTRAINT fk_gol_partida FOREIGN KEY (partida_id) REFERENCES partida (id),
    CONSTRAINT fk_gol_time    FOREIGN KEY (time_id)    REFERENCES time_pelada (id),
    CONSTRAINT fk_gol_autor   FOREIGN KEY (autor_id)   REFERENCES jogador (id),
    CONSTRAINT fk_gol_assist  FOREIGN KEY (assistente_id) REFERENCES jogador (id)
);

CREATE TABLE substituicao (
    id          BIGINT  NOT NULL AUTO_INCREMENT,
    partida_id  BIGINT  NOT NULL,
    time_id     BIGINT  NOT NULL,
    saiu_id     BIGINT  NULL,                            -- NULL = reforço (time incompleto)
    entrou_id   BIGINT  NOT NULL,
    tempo_ms    BIGINT  NOT NULL,

    CONSTRAINT pk_substituicao PRIMARY KEY (id),
    CONSTRAINT fk_sub_partida FOREIGN KEY (partida_id) REFERENCES partida (id),
    CONSTRAINT fk_sub_time    FOREIGN KEY (time_id)    REFERENCES time_pelada (id),
    CONSTRAINT fk_sub_saiu    FOREIGN KEY (saiu_id)    REFERENCES jogador (id),
    CONSTRAINT fk_sub_entrou  FOREIGN KEY (entrou_id)  REFERENCES jogador (id)
);

-- Cobranças da disputa de pênaltis (alternadas: ordem ímpar = time A, par = time B).
-- Não contam como gol/assistência nas estatísticas dos jogadores.
CREATE TABLE penalti (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    partida_id  BIGINT   NOT NULL,
    ordem       INT      NOT NULL,
    time_id     BIGINT   NOT NULL,
    jogador_id  BIGINT   NOT NULL,
    convertido  BOOLEAN  NOT NULL,

    CONSTRAINT pk_penalti PRIMARY KEY (id),
    CONSTRAINT fk_pen_partida FOREIGN KEY (partida_id) REFERENCES partida (id),
    CONSTRAINT fk_pen_time    FOREIGN KEY (time_id)    REFERENCES time_pelada (id),
    CONSTRAINT fk_pen_jogador FOREIGN KEY (jogador_id) REFERENCES jogador (id),
    CONSTRAINT uk_penalti_ordem UNIQUE (partida_id, ordem)
);

-- Tabela-fato: uma linha por jogador por partida encerrada.
-- Overall, rankings e notas são agregações desta tabela.
CREATE TABLE estatistica_partida (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    partida_id    BIGINT        NOT NULL,
    pelada_id     BIGINT        NOT NULL,
    data_jogo     DATE          NOT NULL,
    jogador_id    BIGINT        NOT NULL,
    time_id       BIGINT        NOT NULL,
    resultado     VARCHAR(10)   NOT NULL,                -- VITORIA | EMPATE | DERROTA
    gols          INT           NOT NULL DEFAULT 0,
    assistencias  INT           NOT NULL DEFAULT 0,
    goleiro       BOOLEAN       NOT NULL DEFAULT FALSE,
    gols_sofridos INT           NOT NULL DEFAULT 0,      -- gols do adversário (relevante para goleiros)
    sem_sofrer    BOOLEAN       NOT NULL DEFAULT FALSE,  -- goleiro que não levou gol
    nota          DECIMAL(4,2)  NOT NULL,

    CONSTRAINT pk_estatistica PRIMARY KEY (id),
    CONSTRAINT fk_est_partida FOREIGN KEY (partida_id) REFERENCES partida (id),
    CONSTRAINT fk_est_jogador FOREIGN KEY (jogador_id) REFERENCES jogador (id),
    CONSTRAINT uk_est UNIQUE (partida_id, jogador_id)
);

CREATE INDEX ix_est_data    ON estatistica_partida (data_jogo);
CREATE INDEX ix_est_jogador ON estatistica_partida (jogador_id);
CREATE INDEX ix_est_pelada  ON estatistica_partida (pelada_id);

-- ----------------------------------------------------------
--  Usuário ADM de bootstrap.  Login: admin  /  Senha: admin123
--  Troque a senha na tela "Conta" logo no primeiro acesso.
-- ----------------------------------------------------------
