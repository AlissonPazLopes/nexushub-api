# API de Organizações Parceiras (`/api/v1/organizacoes`)

Documentação dos endpoints REST de credenciamento, consulta paginada e transições de status do ciclo de vida B2B no **NexusHub API**.

---

## Endpoints Disponíveis

| Método | Rota | Descrição | Status de Sucesso |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/organizacoes` | Credencia uma nova organização parceira (status inicial `PENDENTE`) vinculada a um plano ativo. | `201 Created` + header `Location` |
| `GET` | `/api/v1/organizacoes` | Lista organizações com paginação (`size` máx. `50`, ordenado por `criadoEm desc`) e filtros dinâmicos. | `200 OK` (`PageResponse`) |
| `GET` | `/api/v1/organizacoes/{id}` | Consulta os detalhes de uma organização pelo seu `UUID`. | `200 OK` |
| `PATCH` | `/api/v1/organizacoes/{id}/status` | Executa uma transição válida na máquina de estados de credenciamento. | `200 OK` |

---

## Filtros e Paginação (`GET /api/v1/organizacoes`)

Todos os parâmetros de query são opcionais:

- `verticalTech`: `FINTECH`, `IOT_HARDWARE`, `AI_DATA`, `DEVTOOLS` ou `AGRITECH`
- `status`: `PENDENTE`, `ATIVA`, `SUSPENSA` ou `DESCREDENCIADA`
- `planoId`: `UUID` do plano de credenciamento
- `nome`: filtro parcial (*case-insensitive*) sobre `nomeFantasia`
- `page`: índice da página baseado em zero (padrão `0`)
- `size`: quantidade de itens por página (padrão `20`, máximo permitido `50`)

---

## Máquina de Estados de Credenciamento

Comandos aceitos em `PATCH /api/v1/organizacoes/{id}/status` via campo `acao`:

| Ação | Origem Permitida | Destino | Regra |
| :--- | :--- | :--- | :--- |
| `ATIVAR` | `PENDENTE` | `ATIVA` | Ativa a organização recém-credenciada. |
| `SUSPENDER` | `ATIVA` | `SUSPENSA` | Suspende temporariamente uma organização ativa. |
| `REATIVAR` | `SUSPENSA` | `ATIVA` | Restaura uma organização previamente suspensa. |
| `DESCREDENCIAR` | `PENDENTE`, `ATIVA`, `SUSPENSA` | `DESCREDENCIADA` | Estado terminal irreversível. |

---

## Exemplos `cURL`

### 1. Credenciar Organização (`POST /api/v1/organizacoes`)

```bash
curl -i -X POST http://localhost:8080/api/v1/organizacoes \
  -H "Content-Type: application/json" \
  -d '{
    "cnpj": "11.222.333/0001-81",
    "nomeFantasia": "Nexus Cloud",
    "razaoSocial": "Nexus Cloud Tecnologia Ltda",
    "emailContato": "contato@nexuscloud.io",
    "verticalTech": "DEVTOOLS",
    "planoId": "00000000-0000-0000-0000-000000000000"
  }'
```

### 2. Buscar com Filtros e Paginação (`GET /api/v1/organizacoes`)

```bash
curl -s "http://localhost:8080/api/v1/organizacoes?verticalTech=DEVTOOLS&status=ATIVA&nome=nexus&page=0&size=20"
```

### 3. Consultar por ID (`GET /api/v1/organizacoes/{id}`)

```bash
curl -s http://localhost:8080/api/v1/organizacoes/{id}
```

### 4. Alterar Status (`PATCH /api/v1/organizacoes/{id}/status`)

```bash
curl -s -X PATCH http://localhost:8080/api/v1/organizacoes/{id}/status \
  -H "Content-Type: application/json" \
  -d '{
    "acao": "ATIVAR"
  }'
```

---

## Códigos de Erro (RFC 7807 `ProblemDetail`)

Todas as respostas de erro retornam `Content-Type: application/problem+json` contendo `title`, `status`, `detail`, `instance` e `timestamp`.

| HTTP Status | Causa | Exemplo / Observação |
| :--- | :--- | :--- |
| `400 Bad Request` | Erro de validação (*Bean Validation*) ou JSON malformado | Inclui propriedade `errors` com lista de `{ "field", "message" }` nos erros de campo (ex.: CNPJ inválido). |
| `404 Not Found` | Organização ou plano de credenciamento não encontrado | `ResourceNotFoundException`. |
| `409 Conflict` | CNPJ já cadastrado ou conflito de concorrência (`@Version`) | `ConflictException` ou `ObjectOptimisticLockingFailureException`. |
| `422 Unprocessable Entity` | Plano inativo ou transição de status inválida | `BusinessRuleException`. |

### Exemplo de Resposta `400 Bad Request` (`ProblemDetail`)

```json
{
  "type": "about:blank",
  "title": "Dados de entrada invalidos",
  "status": 400,
  "detail": "Um ou mais campos falharam na validacao.",
  "instance": "/api/v1/organizacoes",
  "timestamp": "2026-11-15T20:00:00Z",
  "errors": [
    {
      "field": "cnpj",
      "message": "CNPJ invalido."
    }
  ]
}
```
