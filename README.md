# Digital Bank API - Teste Técnico Realize

API REST simplificada para banco digital, desenvolvida com Java 17, Spring Boot, Maven e PostgreSQL.

## Funcionalidades

- Cadastro e consulta de contas com saldo inicial.
- Transferência de fundos entre contas.
- Registro de movimentações financeiras por conta.
- Registro de transferências e notificações pós-transferência.
- Swagger/OpenAPI.
- Segurança stateless por token Bearer/API Token.
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

O PostgreSQL do container ficará exposto em `localhost:5433`, evitando conflito com uma instalação local na porta padrão `5432`.

Execute a aplicação:

```bash
mvn spring-boot:run
```

A API ficará disponível em:

- http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html

No Swagger, os endpoints protegidos aparecem com autenticação `bearerAuth`. Para testar pelo Swagger:

1. Execute `POST /api/auth/login` e copie o campo `accessToken` da resposta.
2. Clique no botão `Authorize` no topo da página do Swagger.
3. Cole apenas o token, sem escrever `Bearer` antes dele.
4. Confirme em `Authorize`/`Close`.

Depois disso, o Swagger enviará automaticamente o header de autenticação nos endpoints protegidos.

Execute os testes:

```bash
mvn test
```

## Segurança

O endpoint `POST /api/auth/login` é público. Ele recebe `username` e `password`, valida as credenciais gravadas na tabela `users` e retorna um token de acesso.

A senha do usuário deve estar gravada criptografada com BCrypt no campo `password`. O token não fica em variável de ambiente nem em arquivo de configuração; ele é gerado somente após login válido, retornado uma única vez na resposta e armazenado na base apenas como hash SHA-256 para validação posterior.

Login:

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "usuario.teste",
  "password": "senha-segura"
}
```

Resposta:

```json
{
  "accessToken": "token-gerado-no-login",
  "tokenType": "Bearer",
  "expiresAt": "2026-05-26T12:00:00Z"
}
```

Todos os demais endpoints sob `/api/**` exigem o token no header `Authorization`:

```http
Authorization: Bearer token-gerado-no-login
```

Exemplo com curl:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"usuario.teste","password":"senha-segura"}' | jq -r .accessToken)

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/accounts
```

## Endpoints principais

### Login público

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "usuario.teste",
  "password": "senha-segura"
}
```

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
- A autenticação usa login público com usuário/senha, senha armazenada com BCrypt e token aleatório armazenado na base apenas como hash SHA-256.
- O perfil de teste usa H2 em modo compatível com PostgreSQL para testes rápidos.

## Usuários

A tabela `users` é criada pelo Hibernate com os principais campos:

- `name`: nome completo.
- `email`: email único.
- `username`: usuário único usado no login.
- `password`: senha criptografada com BCrypt.
- `current_token_hash`: hash SHA-256 do token ativo, usado apenas para validação.
- `token_expires_at`: expiração do token ativo.

Não existe token fixo em variável de ambiente nem em arquivo de configuração. Para autenticar, primeiro deve existir um usuário na base com senha em BCrypt.

## Dados iniciais

Na primeira execução, a aplicação cria três contas:

- ID 1: Ana Souza, saldo 1000.00
- ID 2: Bruno Lima, saldo 500.00
- ID 3: Carla Martins, saldo 750.00

A aplicação também cria automaticamente um usuário de teste, caso ele ainda não exista na tabela `users`:

- Nome: Usuário Teste
- Email: usuario.teste@example.com
- Username: usuario.teste
- Senha: senha-segura

A senha é gravada no banco com BCrypt. Esse usuário fica disponível de duas formas:

- Na primeira criação do container PostgreSQL, pelo script `docker/init/01-create-test-user.sql` montado em `/docker-entrypoint-initdb.d`.
- Em toda inicialização da aplicação, pelo `DataInitializer`, caso o usuário ainda não exista.

Se o volume `postgres_data` já existir, o PostgreSQL não executa novamente os scripts de `/docker-entrypoint-initdb.d`. Nesse caso, basta iniciar a aplicação que o `DataInitializer` garante a criação do usuário ausente.

Para gerar token após subir o PostgreSQL com Docker e iniciar a aplicação, use:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"usuario.teste","password":"senha-segura"}'
```
