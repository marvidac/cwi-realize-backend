# Observações para entrevista — cwi-realize-backend

Projeto: **Digital Bank API**  
Stack: **Java 17, Spring Boot 3.3.x, Maven, PostgreSQL, Docker Compose, Spring Data JPA, Spring Security, Swagger/OpenAPI, testes com H2**.

---

## 1. Visão geral do projeto

Você pode explicar assim:

> É uma API REST simplificada de banco digital. Ela permite cadastrar e consultar contas, realizar transferências entre contas, consultar movimentações financeiras e registrar notificações após transferências. O projeto foi estruturado em camadas, usando controllers para entrada HTTP, services para regras de negócio, repositories para persistência e entidades de domínio para representar contas, usuários, transferências e movimentações.

Pontos fortes para mencionar:

- API REST com Spring Boot.
- Persistência com JPA/Hibernate.
- PostgreSQL em Docker para ambiente local.
- H2 em perfil de teste.
- Swagger para documentação e testes manuais.
- Autenticação stateless com Bearer token.
- Uso de `BigDecimal` para valores monetários.
- Uso de transações para garantir consistência.
- Bloqueio pessimista para transferências concorrentes.
- Testes unitários e de integração.

---

## 2. Arquitetura em camadas

Uma resposta boa:

> O projeto segue uma arquitetura em camadas. Os controllers recebem as requisições e retornam DTOs. A camada de service concentra as regras de negócio, como validação de transferência, débito, crédito e publicação de evento. A camada repository encapsula o acesso ao banco usando Spring Data JPA. As entidades ficam no domínio e representam o modelo persistido.

Camadas principais:

### `api` / controller

- recebe requisições HTTP;
- valida entrada;
- chama services;
- converte resposta para DTO.

### `api.dto`

- objetos de entrada e saída;
- evita expor diretamente entidades JPA na API.

### `service`

- regras de negócio;
- transações;
- autenticação;
- transferência;
- geração de token.

### `repository`

- interfaces Spring Data JPA;
- consultas ao banco.

### `domain`

- entidades como `Account`, `Transfer`, `Movement`, `User`.

### `config`

- segurança;
- Swagger;
- inicialização de dados.

---

## 3. Por que usar BigDecimal para dinheiro?

Essa é uma pergunta comum.

Resposta:

> Valores monetários não devem ser representados com `double` ou `float` porque esses tipos usam ponto flutuante binário e podem gerar erros de precisão. `BigDecimal` permite trabalhar com escala e precisão decimal, o que é mais adequado para dinheiro.

Exemplo de problema:

```java
double resultado = 0.1 + 0.2;
// pode gerar 0.30000000000000004
```

Com `BigDecimal`, evita esse tipo de inconsistência.

---

## 4. Como a transferência foi pensada

Esse é um dos pontos mais importantes do projeto.

No `TransferService`, a transferência:

- valida se conta origem e destino são diferentes;
- busca as duas contas;
- bloqueia as contas com `PESSIMISTIC_WRITE`;
- debita da origem;
- credita no destino;
- salva a transferência;
- salva duas movimentações:
  - débito na origem;
  - crédito no destino;
- publica evento de transferência concluída.

Boa resposta para entrevista:

> A transferência é feita em uma única transação com `@Transactional`. Isso garante atomicidade: ou todas as alterações acontecem, ou nenhuma acontece. Se houver saldo insuficiente, conta inexistente ou qualquer erro, a transação é revertida.

---

## 5. Por que usar @Transactional?

Resposta:

> Usei `@Transactional` para garantir consistência na operação de transferência. Uma transferência envolve várias mudanças: atualizar saldo da conta origem, atualizar saldo da conta destino, registrar a transferência e registrar movimentações. Todas essas operações precisam ser atômicas.

Ou seja:

- se tudo der certo: commit;
- se algo falhar: rollback.

Isso evita situação como:

- debitou da origem;
- falhou antes de creditar no destino.

---

## 6. Por que usar PESSIMISTIC_WRITE?

Esse é um excelente ponto para entrevista.

Resposta:

> Em um sistema bancário, duas transferências simultâneas podem tentar alterar a mesma conta ao mesmo tempo. Para evitar condição de corrida, o projeto usa bloqueio pessimista com `PESSIMISTIC_WRITE`. Assim, quando uma transação está alterando uma conta, outra transação precisa esperar antes de modificar a mesma linha.

Exemplo de problema sem lock:

Conta tem R$100.

Duas requisições simultâneas tentam transferir R$80.

Sem controle, as duas podem ler saldo 100 e ambas serem aprovadas, gerando saldo inválido.

Com `PESSIMISTIC_WRITE`, a segunda espera a primeira concluir e depois enxerga o saldo atualizado.

---

## 7. Por que carregar contas em ordem crescente de ID?

No projeto, as contas são carregadas em ordem crescente de ID.

Isso é importante.

Resposta:

> Carregar as contas sempre na mesma ordem reduz risco de deadlock. Se uma transferência A bloqueia conta 1 e depois conta 2, e outra transferência B bloqueia conta 2 e depois conta 1, pode ocorrer deadlock. Ao sempre bloquear por ordem crescente de ID, todas as transações seguem a mesma ordem de bloqueio.

Esse ponto demonstra maturidade técnica.

---

## 8. O que é deadlock?

Resposta curta:

> Deadlock acontece quando duas transações ficam esperando uma pela outra indefinidamente.

Exemplo:

- Transação A bloqueia conta 1 e espera conta 2.
- Transação B bloqueia conta 2 e espera conta 1.
- Nenhuma consegue continuar.

Como o projeto mitiga:

- bloqueando as contas sempre em ordem crescente de ID.

---

## 9. Por que criar duas movimentações por transferência?

Resposta:

> Uma transferência tem dois impactos contábeis: débito na conta de origem e crédito na conta de destino. Por isso, o projeto registra duas movimentações, permitindo consultar o extrato de cada conta separadamente.

Exemplo:

Transferência de R$100 da conta 1 para conta 2:

- `Movement 1`:
  - conta 1;
  - tipo `DEBIT`;
  - valor 100.

- `Movement 2`:
  - conta 2;
  - tipo `CREDIT`;
  - valor 100.

---

## 10. Notificação após transferência

O README menciona notificação pós-transferência usando evento após commit.

Resposta boa:

> A notificação é disparada após a transação ser confirmada, usando evento de domínio e `@TransactionalEventListener` com phase `AFTER_COMMIT`. Isso evita enviar notificação de uma transferência que depois seria revertida.

Por que isso é bom?

Imagine:

- sistema envia notificação;
- depois a transação falha;
- usuário recebe aviso de transferência que não aconteceu.

Usar `AFTER_COMMIT` evita esse problema.

---

## 11. Segurança/autenticação

O projeto tem:

- `POST /api/auth/login` público;
- usuário e senha;
- senha com BCrypt;
- token gerado após login;
- endpoints `/api/**` protegidos;
- token enviado por `Authorization: Bearer`;
- token armazenado no banco como hash SHA-256.

Resposta para entrevista:

> A autenticação é stateless. O cliente faz login com usuário e senha, recebe um token e usa esse token nas próximas requisições. A senha é armazenada com BCrypt e o token não é salvo em texto puro no banco; é salvo apenas o hash, reduzindo impacto em caso de vazamento da base.

Ponto importante:

O projeto não usa token fixo em variável de ambiente. Isso é bom porque:

- evita segredo hardcoded;
- permite gerar token por login real;
- permite expiração e invalidação.

---

## 12. Por que BCrypt para senha?

Resposta:

> BCrypt é próprio para armazenamento de senhas porque é um algoritmo lento de propósito e usa salt. Isso dificulta ataques de força bruta caso a base de dados seja vazada.

Não confundir:

- BCrypt: bom para senha.
- SHA-256: bom para hash rápido de token, checksum etc., mas não ideal para senha.

No projeto:

- senha -> BCrypt;
- token aleatório -> SHA-256 no banco.

---

## 13. Por que o token é salvo como hash?

Resposta:

> Se alguém acessar a tabela de usuários, não consegue usar diretamente o token, porque o valor original não está gravado. Quando uma requisição chega, a aplicação calcula o hash do token recebido e compara com o hash salvo.

Fluxo:

1. Login gera token aleatório.
2. Retorna token ao cliente.
3. Salva somente hash no banco.
4. Nas próximas requisições:
   - recebe token;
   - calcula SHA-256;
   - compara com `current_token_hash`.

---

## 14. Spring Security configurado como stateless

No `SecurityConfig`:

- CSRF desabilitado;
- formLogin desabilitado;
- httpBasic desabilitado;
- logout desabilitado;
- `SessionCreationPolicy.STATELESS`.

Resposta:

> Como a API usa token Bearer, não há sessão HTTP tradicional. Cada requisição precisa carregar suas credenciais no header `Authorization`. Por isso a configuração é stateless.

---

## 15. Por que CSRF foi desabilitado?

Resposta:

> CSRF é uma preocupação maior em aplicações baseadas em sessão e cookies. Como a API é stateless e usa `Authorization` header com Bearer token, CSRF pode ser desabilitado nesse contexto.

Cuidado: essa resposta é válida desde que o token não esteja sendo enviado automaticamente por cookie.

---

## 16. O que é OncePerRequestFilter?

No projeto, `ApiTokenAuthenticationFilter` estende `OncePerRequestFilter`.

Resposta:

> `OncePerRequestFilter` garante que o filtro seja executado apenas uma vez por requisição. Ele é útil para autenticação customizada, porque permite interceptar a requisição, extrair o token, validar o usuário e preencher o `SecurityContext` do Spring.

Fluxo do filtro:

1. Verifica se a URL precisa de autenticação.
2. Lê header `Authorization`.
3. Extrai token Bearer.
4. Calcula hash.
5. Busca usuário pelo hash.
6. Verifica expiração.
7. Preenche `SecurityContext`.
8. Continua a cadeia de filtros.

---

## 17. Swagger/OpenAPI

Resposta:

> O Swagger foi usado para facilitar documentação e teste manual da API. Como os endpoints são protegidos, foi configurado um esquema `bearerAuth` para permitir colar o token no botão `Authorize` e testar os endpoints autenticados pela interface.

Também vale mencionar o problema que você enfrentou:

> O endpoint de listagem usa `Pageable`. No Swagger, o `sort` precisa ser enviado como `id,asc` e não como `["id"]`. Caso contrário, o Spring Data tenta ordenar por uma propriedade literal chamada `["id"]`, gerando `PropertyReferenceException`.

---

## 18. DTOs

Pergunta comum: por que usar DTO e não entidade direto?

Resposta:

> DTOs evitam expor diretamente o modelo interno da aplicação. Eles permitem controlar quais campos entram e saem da API, aplicar validações específicas e reduzir acoplamento entre a API pública e as entidades JPA.

Benefícios:

- segurança;
- validação;
- clareza de contrato;
- evita expor campos internos;
- evita problemas de serialização com relacionamentos JPA.

---

## 19. Validação com Jakarta Validation

Se o projeto usa `@Valid`, `@NotNull`, `@Positive` etc., você pode dizer:

> As validações de entrada são feitas nos DTOs com Jakarta Validation. O controller recebe o request com `@Valid`, e erros de validação são tratados de forma centralizada pelo `GlobalExceptionHandler`.

Exemplo:

- valor da transferência precisa ser positivo;
- ids das contas não podem ser nulos;
- nome do cliente não pode ser vazio.

---

## 20. Tratamento centralizado de exceções

`GlobalExceptionHandler` usa `@RestControllerAdvice`.

Resposta:

> O tratamento de erro fica centralizado com `@RestControllerAdvice`. Isso evita try/catch espalhado nos controllers e padroniza as respostas de erro da API.

Exemplos de respostas:

- 400 Bad Request para entrada inválida;
- 401 Unauthorized para token inválido;
- 404 Not Found para conta inexistente;
- 422 Unprocessable Entity para regras de negócio, como saldo insuficiente.

---

## 21. Por que 422 para saldo insuficiente?

Resposta:

> O JSON pode estar sintaticamente correto, mas a operação viola uma regra de negócio. Por isso 422 Unprocessable Entity é adequado para casos como saldo insuficiente ou tentativa de transferência para a mesma conta.

---

## 22. PostgreSQL com Docker Compose

Resposta:

> O Docker Compose facilita subir um PostgreSQL local sem depender de uma instalação manual. Isso torna o projeto mais reproduzível para avaliadores ou outros desenvolvedores.

No projeto, a porta foi ajustada para evitar conflito:

- container: 5432;
- host: 5433.

Então a aplicação conecta em `localhost:5433`.

---

## 23. H2 nos testes

Resposta:

> O perfil de teste usa H2 em modo compatível com PostgreSQL para que os testes sejam rápidos e não dependam de um banco externo. Para cenários mais próximos de produção, Testcontainers também seria uma boa opção.

Se perguntarem “H2 é igual PostgreSQL?”:

> Não totalmente. H2 é útil para testes rápidos, mas pode haver diferenças de dialeto. Para máxima fidelidade, eu usaria Testcontainers com PostgreSQL real.

---

## 24. Testes

Você pode dizer:

> O projeto possui testes unitários para regras de negócio e testes de integração com MockMvc para validar endpoints, autenticação, transferências, erros de negócio e cenários sem token.

Pontos bons para mencionar:

- teste de login válido;
- teste de credenciais inválidas;
- teste de token ausente;
- teste de transferência com saldo suficiente;
- teste de transferência com saldo insuficiente;
- teste de listagem com paginação/sort.

---

## 25. O que é uma FunctionalInterface?

Uma `FunctionalInterface` em Java é uma interface que possui exatamente um método abstrato.

Ela pode ter:

- um único método abstrato;
- métodos default;
- métodos static;
- métodos private.

Mas só pode ter um método abstrato.

Exemplo:

```java
@FunctionalInterface
public interface Calculadora {
    int calcular(int a, int b);
}
```

Como ela tem apenas um método abstrato, pode ser usada com lambda:

```java
Calculadora soma = (a, b) -> a + b;

int resultado = soma.calcular(2, 3);
```

Resultado:

```text
5
```

A anotação `@FunctionalInterface` não é obrigatória, mas é recomendada. Ela faz o compilador validar que a interface realmente tem apenas um método abstrato.

Se você adicionar outro método abstrato, dá erro de compilação:

```java
@FunctionalInterface
public interface Calculadora {
    int calcular(int a, int b);
    int outroMetodo(); // erro
}
```

---

## 26. Exemplos comuns de FunctionalInterface no Java

O pacote `java.util.function` tem várias interfaces funcionais prontas.

### Function<T, R>

Recebe `T` e retorna `R`.

```java
Function<String, Integer> tamanho = texto -> texto.length();
```

### Predicate<T>

Recebe `T` e retorna boolean.

```java
Predicate<Integer> ehMaiorDeIdade = idade -> idade >= 18;
```

### Consumer<T>

Recebe `T` e não retorna nada.

```java
Consumer<String> imprimir = texto -> System.out.println(texto);
```

### Supplier<T>

Não recebe nada e retorna `T`.

```java
Supplier<LocalDateTime> agora = () -> LocalDateTime.now();
```

### BiFunction<T, U, R>

Recebe dois parâmetros e retorna um resultado.

```java
BiFunction<Integer, Integer, Integer> soma = (a, b) -> a + b;
```

---

## 27. Onde aparece FunctionalInterface no projeto?

No `TransferService` existe este trecho:

```java
Map<Long, Account> accountsById = lockedAccounts.stream()
        .collect(Collectors.toMap(Account::getId, Function.identity()));
```

Aqui aparece:

```java
Function.identity()
```

`Function` é uma `FunctionalInterface` do Java.

Ela representa uma função que recebe um valor e retorna outro. No caso de `Function.identity()`, ela retorna o próprio objeto recebido.

Esse trecho transforma uma lista de contas em um mapa:

- chave: `Account::getId`
- valor: a própria `Account`

Ou seja:

```text
List<Account> -> Map<Long, Account>
```

Exemplo conceitual:

```text
Conta ID 1 -> objeto Account 1
Conta ID 2 -> objeto Account 2
```

Assim o service consegue depois fazer:

```java
Account source = accountsById.get(request.sourceAccountId());
Account target = accountsById.get(request.targetAccountId());
```

---

## 28. O que significa Account::getId?

Isso é method reference.

É uma forma curta de escrever uma lambda.

```java
Account::getId
```

equivale a:

```java
account -> account.getId()
```

Então:

```java
Collectors.toMap(Account::getId, Function.identity())
```

equivale conceitualmente a:

```java
Collectors.toMap(
    account -> account.getId(),
    account -> account
)
```

Essa é uma ótima explicação para entrevista.

---

## 29. Diferença entre lambda e FunctionalInterface

Boa resposta:

> Lambda é a implementação de uma `FunctionalInterface`. A interface define o contrato, e a lambda fornece o comportamento.

Exemplo:

```java
Predicate<Integer> positivo = numero -> numero > 0;
```

`Predicate` é a `FunctionalInterface`.

`numero -> numero > 0` é a lambda.

---

## 30. Por que FunctionalInterface é importante?

Resposta:

> `FunctionalInterface` permite tratar comportamento como parâmetro. Isso facilita programação funcional em Java, uso de streams, callbacks, filtros, mapeamentos e composição de funções.

Exemplos práticos:

- filtrar lista;
- transformar dados;
- passar regra como parâmetro;
- evitar classes anônimas verbosas;
- usar Streams API.

Antes do Java 8:

```java
Collections.sort(lista, new Comparator<Pessoa>() {
    @Override
    public int compare(Pessoa a, Pessoa b) {
        return a.getNome().compareTo(b.getNome());
    }
});
```

Com lambda:

```java
lista.sort((a, b) -> a.getNome().compareTo(b.getNome()));
```

---

## 31. FunctionalInterface pode ter default methods?

Sim.

Exemplo:

```java
@FunctionalInterface
public interface Validador {
    boolean validar(String valor);

    default boolean validarNaoNulo(String valor) {
        return valor != null && validar(valor);
    }
}
```

Continua sendo `FunctionalInterface` porque só tem um método abstrato: `validar`.

---

## 32. Perguntas prováveis de entrevista e respostas curtas

### Por que Spring Boot?

> Porque reduz configuração manual, fornece autoconfiguração, integração com Spring Data, Spring Security, validação, actuator se necessário, e facilita criar APIs REST rapidamente.

### Por que usar repository interface?

> Spring Data JPA gera a implementação em tempo de execução. Isso reduz código boilerplate e deixa a camada de persistência mais declarativa.

### Por que não colocar regra de negócio no controller?

> Controller deve orquestrar entrada e saída HTTP. Regra de negócio fica no service para facilitar teste, reutilização e manutenção.

### Por que usar DTO?

> Para proteger o domínio, controlar contrato da API e aplicar validações específicas.

### O que garante que a transferência não fica pela metade?

> `@Transactional`. Se qualquer etapa falhar, o Spring reverte a transação.

### Como evitar saldo negativo em concorrência?

> Além da validação de saldo, o projeto usa bloqueio pessimista na leitura das contas durante a transferência.

### Por que ordenar locks por ID?

> Para reduzir risco de deadlock.

### Por que publicar evento após transferência?

> Para desacoplar a transferência da notificação e permitir executar ações pós-transferência de forma mais organizada.

### Por que AFTER_COMMIT?

> Para evitar notificar uma transferência que foi revertida.

### Por que token stateless?

> Cada requisição carrega a credencial no header, evitando sessão no servidor e facilitando escalabilidade.

### O token é JWT?

Resposta sugerida:

> Não necessariamente. Pelo desenho atual, é um token opaco gerado no login. A API salva apenas o hash no banco e valida o token recebido. JWT seria uma alternativa, mas o token opaco permite invalidação mais simples pelo servidor.

### Diferença entre token opaco e JWT?

> JWT carrega claims assinadas e pode ser validado sem consultar banco. Token opaco não carrega informação útil por si só; precisa ser validado no servidor. A vantagem do token opaco é facilitar revogação imediata.

### Por que usar BCrypt e não SHA-256 para senha?

> SHA-256 é rápido demais para senha. BCrypt é lento de propósito e usa salt, dificultando ataques de força bruta.

### O que melhoraria no projeto?

Boas respostas:

- adicionar Testcontainers com PostgreSQL real;
- adicionar Flyway ou Liquibase para versionamento de schema;
- adicionar refresh token;
- adicionar roles/perfis de autorização;
- adicionar auditoria mais detalhada;
- adicionar idempotência em transferências;
- adicionar observabilidade com Actuator/Micrometer;
- adicionar paginação e filtros mais ricos;
- adicionar tratamento mais robusto para concorrência e retries em deadlock;
- adicionar CI/CD com GitHub Actions.

---

## 33. Observações críticas que demonstram senioridade

Você pode usar estas frases se quiser impressionar:

> Em operações financeiras, atomicidade e isolamento são tão importantes quanto validação de entrada.

> Não basta verificar saldo; é necessário considerar concorrência.

> O uso de `BigDecimal` evita problemas de precisão, mas também é importante definir escala e arredondamento quando aplicável.

> Eventos pós-commit evitam efeitos colaterais inconsistentes.

> Swagger é documentação útil, mas os contratos principais devem ser validados por testes automatizados.

> H2 acelera os testes, mas para garantir compatibilidade real com PostgreSQL eu consideraria Testcontainers.

> Token opaco com hash em banco facilita revogação, enquanto JWT favorece validação distribuída sem consulta ao banco.

> Controllers finos e services com regra de negócio tornam o projeto mais testável.

---

## 34. Explicação curta de FunctionalInterface para memorizar

Se quiser uma resposta de entrevista bem direta:

> Uma `FunctionalInterface` é uma interface com exatamente um método abstrato. Ela pode ser implementada por lambda ou method reference. É a base da programação funcional em Java, usada em APIs como Stream, Predicate, Function, Consumer e Supplier.

Exemplo rápido:

```java
@FunctionalInterface
interface Operacao {
    int executar(int a, int b);
}

Operacao soma = (a, b) -> a + b;

System.out.println(soma.executar(2, 3));
```

Resultado:

```text
5
```

---

## 35. Relação com Stream API

`FunctionalInterface` aparece muito em Stream.

Exemplo:

```java
List<String> nomes = List.of("Ana", "Bruno", "Carla");

List<String> filtrados = nomes.stream()
        .filter(nome -> nome.startsWith("A"))
        .toList();
```

`filter` recebe um `Predicate<String>`, que é uma `FunctionalInterface`.

`map` recebe uma `Function<T, R>`:

```java
List<Integer> tamanhos = nomes.stream()
        .map(nome -> nome.length())
        .toList();
```

`map` poderia ser escrito com method reference:

```java
List<Integer> tamanhos = nomes.stream()
        .map(String::length)
        .toList();
```

---

## Resumo final para usar na entrevista

> O projeto foi desenhado como uma API REST bancária em Spring Boot, com separação em camadas, persistência JPA, segurança stateless via Bearer token, senha com BCrypt, token opaco armazenado como hash, PostgreSQL em Docker e testes automatizados. A parte mais sensível é a transferência: ela roda dentro de uma transação, usa BigDecimal para dinheiro, bloqueia contas com PESSIMISTIC_WRITE e sempre carrega as contas em ordem crescente de ID para reduzir deadlocks. Após a transferência, publica evento para notificação depois do commit, evitando efeitos colaterais em transações que falhem.

E sobre `FunctionalInterface`:

> `FunctionalInterface` é uma interface com um único método abstrato, permitindo implementação com lambda ou method reference. No projeto, um exemplo é `Function.identity()` usado com `Collectors.toMap` para transformar uma lista de contas em um mapa por ID.
