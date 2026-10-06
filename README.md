# API de produtos (Java)

API REST de produtos que substitui o backend Spring Boot 1.5 de [rcroce/springboot-product](https://github.com/rcroce/springboot-product). É consumida pelos apps web e mobile de [rcroce/claude-products-app-web](https://github.com/rcroce/claude-products-app-web).

Há uma segunda implementação do mesmo contrato em Node.js, em [rcroce/claude-products-api-node](https://github.com/rcroce/claude-products-api-node). As duas atendem o mesmo [`openapi.yaml`](src/main/resources/static/openapi.yaml), então o frontend troca de uma para a outra só mudando a URL.

## Stack

| Camada | Escolha |
| --- | --- |
| Linguagem | Java 25 (LTS) |
| Framework | Spring Boot 4.1 (Spring MVC com virtual threads) |
| Banco | PostgreSQL 18, migrações com Flyway |
| Acesso a dados | Spring Data JPA (Hibernate 7) |
| Testes | JUnit 5, MockMvc, Testcontainers, validação de contrato OpenAPI, ArchUnit |
| Build e CI | Maven Wrapper, GitHub Actions, CodeQL, Dependabot |

## Requisitos

- JDK 25
- Docker (o Postgres de desenvolvimento e o dos testes rodam em container)

## Como rodar

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

O Spring Boot sobe o Postgres do `compose.yaml` sozinho e aplica as migrações. O perfil `dev` carrega 3 produtos de exemplo. A API responde em http://localhost:8080/api/v1/products e o contrato fica em http://localhost:8080/openapi.yaml.

Para rodar os testes:

```bash
./mvnw verify
```

## Configuração

| Variável | Para quê | Padrão |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Conexão com o Postgres fora do ambiente de desenvolvimento | nenhum |
| `CORS_ALLOWED_ORIGINS` | Origens do navegador que podem chamar a API, separadas por vírgula | `http://localhost:5173,http://localhost:8081` (Vite e Expo web) |

Health checks para a nuvem: `/actuator/health/liveness` e `/actuator/health/readiness`.

## Contrato v1

Base `/api/v1`. Respostas em JSON; erros em `application/problem+json` (RFC 9457) com mensagens em português e, nos erros de validação, a lista `errors` com campo e mensagem.

| Método e rota | Sucesso | Erros |
| --- | --- | --- |
| `GET /products?q=&page=0&size=20&sort=name` | 200, página de produtos | 400 |
| `GET /products/{id}` | 200, produto | 400, 404 |
| `POST /products` | 201, produto e cabeçalho `Location` | 400, 409 nome repetido |
| `PUT /products/{id}` | 200, produto | 400, 404, 409 nome repetido ou versão desatualizada |
| `DELETE /products/{id}` | 204 | 400, 404 |

Produto: `{ "id": 1, "name": "Mouse", "quantity": 50, "version": 0 }`. Página: `{ "items": [...], "page": 0, "size": 20, "totalItems": 3, "totalPages": 1 }`. `sort` aceita `id`, `name` ou `quantity`, com `-` na frente para ordem decrescente.

Regras: nome obrigatório, até 45 caracteres e único sem diferenciar maiúsculas e minúsculas; quantidade inteira de 0 a 999.999.999. No `PUT`, enviar a `version` lida no `GET` faz a API recusar a alteração (409) se outra pessoa mudou o produto nesse meio-tempo.

Os testes de API conferem toda resposta contra o `openapi.yaml`, então uma mudança no código que quebre o contrato falha no CI.

## Organização

```
src/main/java/com/claudeproducts/api
├── product/api          controller, DTOs (records) e paginação
├── product/domain       entidade, regras e serviço transacional
├── product/persistence  repositório Spring Data
└── shared               erros (Problem Details) e CORS
```

O ArchUnit garante que o controller não acessa o repositório direto e que o domínio não depende de HTTP.

## Próximas etapas

1. Integração com o frontend (`packages/core` apontando para `/api/v1`).
2. Login: Spring Security como OAuth2 Resource Server (JWT de um provedor OIDC).
3. Publicação: imagem via `./mvnw spring-boot:build-image` e Postgres gerenciado na nuvem.
