# 🚀 NexusHub API

Motor Back-end B2B (SaaS API-First) para Polos de Inovação, Parques Tecnológicos e Redes de Empresas Parceiras, desenvolvido com **Java 21 LTS**, **Spring Boot 3.3**, **PostgreSQL 16**, **Redis 7** e **Flyway**.

---

## 🛠️ Stack Principal

- **Linguagem & Framework:** Java 21 LTS (Records & Virtual Threads) + Spring Boot 3.3+
- **Persistência & Migrações:** Spring Data JPA (`ddl-auto: validate`), Hibernate, Flyway Migrations e PostgreSQL 16 (H2 em `MODE=PostgreSQL` nos testes)
- **Qualidade & Contratos:** Bean Validation, `ProblemDetail` (RFC 7807), SpringDoc OpenAPI (`/swagger-ui.html`) e JUnit 5 + Mockito

---

## 📌 Endpoints da API

### Healthcheck & Planos de Credenciamento

| Método | Rota | Descrição |
| :--- | :--- | :--- |
| `GET` | `/api/v1/health` | Verifica saúde da aplicação (`UP`, versão Java e timestamp) |
| `POST` | `/api/v1/planos` | Cadastra um plano de credenciamento (`SEED`, `GROWTH`, `CORPORATE`) |
| `GET` | `/api/v1/planos` | Lista planos ativos ordenados por mensalidade |
| `GET` | `/api/v1/planos/{id}` | Consulta detalhes de um plano por `UUID` |

### Organizações Parceiras (`/api/v1/organizacoes`)

| Método | Rota | Descrição | Status de Sucesso |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/organizacoes` | Credencia organização com CNPJ validado/sanitizado e vínculo a plano ativo | `201 Created` (`Location`) |
| `GET` | `/api/v1/organizacoes` | Busca paginada (`size` máx. `50`, `criadoEm desc`) com filtros `verticalTech`, `status`, `planoId` e `nome` | `200 OK` |
| `GET` | `/api/v1/organizacoes/{id}` | Consulta detalhes de uma organização por `UUID` | `200 OK` |
| `PATCH` | `/api/v1/organizacoes/{id}/status` | Executa transição na máquina de estados (`ATIVAR`, `SUSPENDER`, `REATIVAR`, `DESCREDENCIAR`) | `200 OK` |

📄 Consulte os payloads, exemplos `cURL` e contratos de erro RFC 7807 na [documentação completa da API de Organizações](docs/api-organizacoes.md).

---

## ▶️ Execução Local & Testes

```powershell
# Subir PostgreSQL 16 e Redis 7
docker compose up -d

# Executar suíte completa de testes
./mvnw clean test
```
