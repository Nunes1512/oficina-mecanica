-- Rode no banco "oficina" com o usuário postgres. Troque a senha antes de executar
-- e NÃO salve a senha real neste arquivo.
CREATE USER oficina_app WITH PASSWORD 'troque_esta_senha';
GRANT CONNECT ON DATABASE oficina TO oficina_app;
GRANT USAGE ON SCHEMA public TO oficina_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO oficina_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO oficina_app;
REVOKE UPDATE, DELETE ON auditoria FROM oficina_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO oficina_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO oficina_app;
