# fdm-products

## Описание

`fdm-products` — backend-сервис на Spring Boot, предоставляющий REST API для:
- **управления продуктами** (информация о продукте, доступность, связи с технологиями и инфраструктурой);
- **инфраструктуры продукта** (синхронизация инфраструктуры, поиск по параметрам);
- **аналитики и интеграций** (интерфейсы из архитектурных систем / Mapic, e2e-процессы, fitness functions, паттерны и т.д.).

Основные REST эндпоинты доступны по префиксу `/api/v1/**` и документируются через Swagger.

## Технологический стек

- **Язык**: Java 17
- **Фреймворк**: Spring Boot 2.7.x
- **База данных**: PostgreSQL (Spring Data JPA)
- **Миграции БД**: Flyway
- **Очереди/сообщения**: RabbitMQ (Spring AMQP)
- **Документация API**: Springfox Swagger
- **Метрики и мониторинг**: Spring Boot Actuator, Micrometer, Prometheus
- **Трейсинг и профилирование**: OpenTelemetry, actuator-profiling

## Требования

- JDK **17**
- Maven **3.8+**
- Доступ к PostgreSQL (URL/учётные данные настраиваются в `application*.yml` или через переменные окружения)
- (Опционально) доступ к RabbitMQ и внешним системам, с которыми интегрируется сервис

## Сборка и запуск

### Локальный запуск через Maven

```bash
mvn clean install
mvn spring-boot:run
```

После успешного запуска приложение (по умолчанию) доступно по адресам:

- app: `http://localhost:8080`
- Swagger UI (Springfox): `http://localhost:8080/swagger-ui/index.html`

### Запуск JAR напрямую

После сборки в директории `target` будет артефакт вида `fdm-products-<version>.jar`:

```bash
java -jar target/fdm-products-<version>.jar
```

### Запуск в контейнере (Docker / Podman)

В проекте используется многоступенчатый `Dockerfile`:
- стадия сборки использует образ `eclipse-temurin:17-jdk-jammy` и устанавливает Maven через `apt-get`;
- стадия запуска использует образ `eclipse-temurin:17-jre-jammy`;
- приложение запускается от непривилегированного пользователя `appuser`.

#### Сборка образа

```bash
docker build -t fdm-products .
# or
podman build -t fdm-products .
```

#### Простой запуск (со значениями по умолчанию из `application.yml`)

```bash
docker run --rm -p 8080:8080 fdm-products
# or
podman run --rm -p 8080:8080 fdm-products
```

#### Запуск с профилем и переменными окружения

Приложение может читать конфигурацию (БД, очереди, интеграции) из переменных окружения, если они используются в `application.properties`/`application.yml` в формате `${VAR_NAME:default}`.

Пример:

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL="jdbc:postgresql://postgres:5432/fdm" \
  -e DB_USER="fdm_user" \
  -e DB_PASSWORD="secret" \
  -e RABBIT_HOST="rabbitmq" \
  fdm-products
```

Эту же конфигурацию можно описать в `docker-compose.yml` или в манифестах Kubernetes.

### Локальный запуск через Docker Compose

В `docker-compose.yml` поднимаются:
- PostgreSQL
- RabbitMQ (с web UI по `http://localhost:15672`, логин/пароль по умолчанию: `guest` / `guest`)
- сам сервис `fdm-products`

Запуск:

```bash
docker compose up --build
```

## Основные REST эндпоинты

Ниже приведён неполный список ключевых эндпоинтов (полный список см. в Swagger).

- **Продукты** (`ProductController`, префикс `/api/v1`):
  - `GET /api/v1/user/product` — получить продукты текущего пользователя;
  - `GET /api/v1/product/{code}` — получить детальную информацию о продукте по alias;
  - `GET /api/v1/product/{id}/availability` — получить данные о доступности продукта;
  - `GET /api/v1/product/by-ids` — получить продукты по списку ID;
  - `PUT /api/v1/product/{code}` — создать/обновить продукт;
  - `PUT /api/v1/product/{code}/relations` — создать/обновить связи продукта (контейнеры, интерфейсы и т.д.).

- **Инфраструктура продукта** (`InfraController`, префикс `/api/v1/infra`):
  - `POST /api/v1/infra?product={product}` — синхронизировать инфраструктуру продукта.

- **Технологии** (`TechController`, префикс `/api/v1/tech`):
  - `GET /api/v1/tech/{techId}/product` — получить все продукты, использующие технологию.

- **Интерфейсы и Mapic / архитектура**:
  - `/api/v1/product/{cmdb}/interface/arch` — интерфейсы продукта из архитектурной модели;
  - `/api/v1/product/{cmdb}/interface/mapic` — интерфейсы продукта из Mapic;
  - дополнительные контроллеры `MapicController`, `DiscoveredInterfaceController`, `InterfaceController` описывают сценарии интеграций.

- **Fitness functions и паттерны**:
  - `GET /api/v1/product/{alias}/fitness-function` — получить результаты fitness-функций;
  - `POST /api/v1/product/{alias}/fitness-function/{source_type}` — опубликовать результаты fitness-функций;
  - `GET /api/v1/product/{alias}/patterns` — паттерны, реализованные в продукте;
  - `POST /api/v1/product/{alias}/patterns/{source-type}` — привязать паттерны к продукту.

## Конфигурация

Основные параметры приложения задаются в `application.yml` / `application-*.yml`:

- параметры подключения к PostgreSQL;
- параметры подключения и очередей RabbitMQ;
- параметры интеграций с внешними системами (Structurizr, Mapic и т.д.);
- настройки OpenTelemetry, метрик и прочие технические параметры.

Многие параметры можно переопределять через переменные окружения в dev/test/prod средах.

## Тесты и качество кода

- Юнит- и интеграционные тесты:

```bash
mvn test
```

- Покрытие кода собирается через **JaCoCo** и интегрируется с **SonarQube**.

## Лицензия

См. файл `LICENSE` в корне проекта.

