# fdm-products

## Overview

`fdm-products` is a Spring Boot–based backend service that provides a REST API for:
- **product management** (product information, availability, links with technologies and infrastructure);
- **product infrastructure** (synchronization of infrastructure, search by parameters);
- **analytics and integrations** (interfaces from architecture systems / Mapic, e2e processes, fitness functions, patterns, etc.).

Main REST endpoints are available under `/api/v1/**` and are documented via Swagger.

## Technology stack

- **Language**: Java 17
- **Framework**: Spring Boot 2.7.x
- **Database**: PostgreSQL (Spring Data JPA)
- **DB migrations**: Flyway
- **Messaging**: RabbitMQ (Spring AMQP)
- **API documentation**: Springfox Swagger
- **Metrics & monitoring**: Spring Boot Actuator, Micrometer, Prometheus
- **Tracing & profiling**: OpenTelemetry, actuator-profiling

## Requirements

- JDK **17**
- Maven **3.8+**
- Access to PostgreSQL (URL/credentials configured in `application*.yml` or via environment variables)
- (Optional) access to RabbitMQ broker and external systems the service integrates with

## Build and run

### Local run via Maven

```bash
mvn clean install
mvn spring-boot:run
```

After successful startup the application is available (by default) at:

- app: `http://localhost:8080`
- Swagger UI (if enabled): `http://localhost:8080/swagger-ui/`

### Run JAR directly

After the build, the `target` directory contains an artifact like `fdm-products-<version>.jar`:

```bash
java -jar target/fdm-products-<version>.jar
```

### Run in container (Docker / Podman)

The project includes a multi-stage `Dockerfile`:
- build stage uses image `maven:3.9-eclipse-temurin-17`;
- runtime stage uses `eclipse-temurin:17-jre-jammy`;
- the application is run under a non-privileged user `appuser`.

#### Build image

```bash
docker build -t fdm-products .
# or
podman build -t fdm-products .
```

#### Simple run (with defaults from application.yml)

```bash
docker run --rm -p 8080:8080 fdm-products
# or
podman run --rm -p 8080:8080 fdm-products
```

#### Run with profile and environment variables

The application can read configuration (DB, queues, integrations) from environment variables, if they are referenced in `application.yml` using `${VAR_NAME:default}`.

Example:

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL="jdbc:postgresql://postgres:5432/fdm" \
  -e DB_USER="fdm_user" \
  -e DB_PASSWORD="secret" \
  -e RABBIT_HOST="rabbitmq" \
  fdm-products
```

The same configuration can be described in `docker-compose.yml` or Kubernetes manifests.

## Main REST endpoints

Below is a non-exhaustive list of key endpoints (see Swagger for full details).

- **Products** (`ProductController`, prefix `/api/v1`):
  - `GET /api/v1/user/product` — get products for the current user;
  - `GET /api/v1/product/{code}` — get detailed product info by alias;
  - `GET /api/v1/product/{id}/availability` — get product availability data;
  - `GET /api/v1/product/by-ids` — get products by list of IDs;
  - `PUT /api/v1/product/{code}` — create/update product;
  - `PUT /api/v1/product/{code}/relations` — create/update product relations (containers, interfaces, etc.).

- **Product infrastructure** (`InfraController`, prefix `/api/v1/infra`):
  - `POST /api/v1/infra?product={product}` — synchronize product infrastructure.

- **Technologies** (`TechController`, prefix `/api/v1/tech`):
  - `GET /api/v1/tech/{techId}/product` — get all products that use a specific technology.

- **Interfaces & Mapic / architecture**:
  - `/api/v1/product/{cmdb}/interface/arch` — product interfaces from architecture model;
  - `/api/v1/product/{cmdb}/interface/mapic` — product interfaces from Mapic;
  - additional controllers `MapicController`, `DiscoveredInterfaceController`, `InterfaceController` describe detailed integration scenarios.

- **Fitness functions & patterns**:
  - `GET /api/v1/product/{alias}/fitness-function` — get fitness-function results;
  - `POST /api/v1/product/{alias}/fitness-function/{source_type}` — publish fitness-function results;
  - `GET /api/v1/product/{alias}/patterns` — patterns implemented in the product;
  - `POST /api/v1/product/{alias}/patterns/{source-type}` — bind patterns to products.

## Configuration

Core application parameters are configured in `application.yml` / `application-*.yml`:

- PostgreSQL connection settings;
- RabbitMQ queues and connection settings;
- integration parameters for external systems (Structurizr, Mapic, etc.);
- OpenTelemetry, metrics and other technical settings.

Many of these can be overridden via environment variables in dev/test/prod environments.

## Tests and code quality

- Unit and integration tests:

```bash
mvn test
```

- Code coverage is collected via **JaCoCo** and integrated with **SonarQube**.

## License

See `LICENSE` file in the project root.

