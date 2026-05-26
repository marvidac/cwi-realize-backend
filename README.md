# Digital Bank API - Teste Técnico Realize

API REST simplificada para banco digital, desenvolvida com Java 17, Spring Boot, Maven e PostgreSQL.

## Funcionalidades

- Cadastro e consulta de contas com saldo inicial.
- Transferência de fundos entre contas.
- Registro de movimentações financeiras por conta.
- Registro de transferências e notificações pós-transferência.
- Swagger/OpenAPI.
- Testes unitários e de integração.

## Como rodar

Pré-requisitos:

- Java 17
- Maven 3.9+
- Docker e Docker Compose

Suba o PostgreSQL:

```bash
docker compose up -d
```

Execute a aplicação:

```bash
mvn spring-boot:run
```

A API ficará disponível em:

- http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html

Execute os testes:

```bash
mvn test
```

## Endpoints principais

### Criar conta

```http
POST /api/accounts
Content-Type: application/json

{
  "customerName": "Maria Silva",
  "initialBalance": 1000.00
}
```

### Listar contas

```http
GET /api/accounts
```

### Consultar conta

```http
GET /api/accounts/{id}
```

### Consultar movimentações da conta

```http
GET /api/accounts/{id}/movements
```

### Transferir fundos

```http
POST /api/transfers
Content-Type: application/json

{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 100.00
}
```

## Decisões de design e arquitetura

- Arquitetura em camadas: `controller`, `service`, `repository`, `domain`, `dto` e `exception`.
- `BigDecimal` é usado para valores monetários.
- Transferências são executadas dentro de transação única com `@Transactional`.
- Para consistência em alta concorrência, as contas envolvidas são bloqueadas com `PESSIMISTIC_WRITE` e sempre carregadas em ordem crescente de ID, reduzindo risco de deadlock.
- Cada transferência gera duas movimentações: débito na conta de origem e crédito na conta de destino.
- A notificação é disparada por evento de domínio após commit (`@TransactionalEventListener(phase = AFTER_COMMIT)`), evitando notificar transferências que sofreram rollback.
- O envio real de notificação foi simulado em log e persistido em tabela própria, mantendo a API resiliente e simples para o teste.
- Swagger foi configurado via `springdoc-openapi`.
- O perfil de teste usa H2 em modo compatível com PostgreSQL para testes rápidos.

## Dados iniciais

Na primeira execução, a aplicação cria três contas:

- ID 1: Ana Souza, saldo 1000.00
- ID 2: Bruno Lima, saldo 500.00
- ID 3: Carla Martins, saldo 750.00
