-- Usuário exclusivo dos testes; nunca é empacotado no artefato de produção.
INSERT INTO usuario (username, senha, nome_completo, perfil, ativo)
VALUES (
    'admin',
    '$2b$10$FSbSAnHCWRhSoQr7ZPnbCOgNtiFhU7G9foOZjWCRZnCYsEoKyjgNa',
    'Administrador de Teste',
    'ADMIN',
    TRUE
);
