# Parte 1: base do projeto e entidades

Esta parte contém o `pom.xml`, a configuração, as migrations do banco (V1 a V7 e V9) e as
13 entidades JPA, com os mesmos nomes de tabelas e colunas do banco.

## Como usar
1. Descompacte na raiz do repositório novo (o `pom.xml` fica ao lado da pasta `database/`).
2. Copie `.env.example` para `.env` e preencha:
   - `DB_PASSWORD`: a senha do usuário `oficina_app`.
   - `FLYWAY_PASSWORD`: a senha do `postgres` (quem cria tabelas e triggers).
   - `ADMIN_PASSWORD` pode ficar vazio nesta parte.
3. Crie o usuário da aplicação (se ainda não existe): troque a senha em `database/criar_usuario_app.sql`
   e rode no Query Tool do banco `oficina`.
4. Rode a aplicação (`OficinaApplication` no IntelliJ ou `mvn spring-boot:run` na raiz).

## O que esperar no console
- Banco que já tem V1 a V8 rodados à mão: `Successfully baselined schema with version: 8` e depois
  `Migrating schema "public" to version "9 - estoque item os alteracao exclusao"`.
- Banco vazio: o Flyway aplica V1 a V7 e V9 sozinho (sem os dados de exemplo).
- `Started OficinaApplication`: o Hibernate conferiu todas as entidades contra o banco (validate).
- A linha `Using generated security password` é normal agora: o login próprio vem na Parte 2.
- Ainda não há telas. Nesta parte o teste é a aplicação iniciar sem erro.

## Dados de exemplo (só desenvolvimento)
`database/dev/V8__dados_exemplo.sql` fica fora das migrations de propósito. Rode à mão no pgAdmin, com o
usuário `postgres`, se quiser dados para testar.

## Erros comuns
| Mensagem | O que fazer |
|---|---|
| `Could not resolve placeholder 'DB_PASSWORD'` | O `.env` não está na raiz do projeto ou falta a linha `DB_PASSWORD=` |
| `password authentication failed` | Senha errada no `.env` (`DB_PASSWORD` ou `FLYWAY_PASSWORD`) |
| `permission denied for schema public` | Faltou criar o usuário (passo 3) ou rodar com `FLYWAY_USER=postgres` |
| `Schema-validation: missing column` | Banco desatualizado; confira se V1 a V8 foram rodados |
| `Found non-empty schema(s) without schema history table` | Confira `baseline-on-migrate` no `application.yml` |
