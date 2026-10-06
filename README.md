# API de produtos (Java)

API REST de produtos que substitui o backend Spring Boot 1.5 de [rcroce/springboot-product](https://github.com/rcroce/springboot-product). É consumida pelos apps web e mobile de [rcroce/claude-products-app-web](https://github.com/rcroce/claude-products-app-web).

Há uma segunda implementação do mesmo contrato em Node.js, em [rcroce/claude-products-api-node](https://github.com/rcroce/claude-products-api-node). As duas atendem o mesmo `openapi.yaml`, então o frontend troca de uma para a outra só mudando a URL.

## Stack

Java 25, Spring Boot 4, PostgreSQL 18, Flyway, Maven.
