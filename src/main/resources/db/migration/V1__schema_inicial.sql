CREATE TABLE usuario (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)     NOT NULL,
    senha           VARCHAR(100)    NOT NULL,           -- hash BCrypt (~60 chars)
    nome_completo   VARCHAR(120)    NOT NULL,
    perfil          VARCHAR(30)     NOT NULL,           -- TREINADOR | AUXILIAR_TECNICO
    ativo           BOOLEAN         NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_username UNIQUE (username)
);

CREATE TABLE jogador (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    nome             VARCHAR(120)   NOT NULL,
    documento        VARCHAR(20)    NOT NULL,           -- CPF, RG ou registro do atleta
    posicao          VARCHAR(30)    NOT NULL,           -- GOLEIRO, ZAGUEIRO, ...
    altura_cm        INT            NULL,
    peso_kg          DECIMAL(5,2)   NULL,
    data_nascimento  DATE           NULL,
    ativo            BOOLEAN        NOT NULL DEFAULT TRUE,
    criado_em        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_jogador PRIMARY KEY (id),
    CONSTRAINT uk_jogador_documento UNIQUE (documento)
);

-- ----------------------------------------------------------
--  Usuário admin de bootstrap.
--  Login: admin
--  Senha: admin123
-- ----------------------------------------------------------
INSERT INTO usuario (username, senha, nome_completo, perfil, ativo)
VALUES (
    'admin',
    '$2b$10$FSbSAnHCWRhSoQr7ZPnbCOgNtiFhU7G9foOZjWCRZnCYsEoKyjgNa',
    'Administrador do Sistema',
    'TREINADOR',
    TRUE
);
