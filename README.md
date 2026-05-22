# DeliveryFlow

Система управления доставкой в реальном времени.

## Архитектура

- **Java 21** с виртуальными потоками (Project Loom)
- **Spring Boot 3.3.x** + Spring Security 6.x (JWT)
- **Apache Kafka 3.7** для событийной архитектуры
- **Redis 7.x** для геолокации курьеров и распределённых блокировок
- **PostgreSQL 16** как основное хранилище
- **WebSocket (STOMP)** для real-time уведомлений

## Роли

| Роль | Возможности |
|------|-------------|
| CLIENT | Создание заказов, отслеживание статуса |
| COURIER | Принятие заказов, обновление статуса, трансляция локации |
| ADMIN | Просмотр всех заказов и курьеров, история статусов |

## Быстрый старт

```bash
docker-compose up -d
./gradlew bootRun
```

## API Endpoints

### Authentication
- `POST /api/v1/auth/register` — Регистрация
- `POST /api/v1/auth/login` — Вход

### Orders (CLIENT)
- `POST /api/v1/orders` — Создать заказ
- `GET /api/v1/orders/{id}` — Получить заказ
- `GET /api/v1/orders/me` — Мои заказы
- `DELETE /api/v1/orders/{id}` — Отменить заказ

### Courier (COURIER)
- `GET /api/v1/courier/orders/available` — Доступные заказы
- `PATCH /api/v1/courier/orders/{id}/accept` — Принять заказ
- `PATCH /api/v1/courier/orders/{id}/status` — Обновить статус
- `POST /api/v1/courier/location` — Отправить локацию

### Admin (ADMIN)
- `GET /api/v1/admin/orders` — Все заказы (paginated)
- `GET /api/v1/admin/couriers` — Все курьеры
- `GET /api/v1/admin/orders/{id}/history` — История статуса заказа

## WebSocket

- Connect: `/ws`
- Subscribe: `/topic/orders/{orderId}/status`
- Send: `/app/courier/location`

## Метрики

Prometheus: `http://localhost:9090`
Grafana: `http://localhost:3000` (admin/admin)

Kafka UI: `http://localhost:8090`

## Тестирование

```bash
./gradlew test
./gradlew integrationTest
```

## Структура проекта

```
com.deliveryflow/
├── api/           # Контроллеры, DTO, исключения
├── config/        # Конфигурация безопасности, Kafka, Redis, WebSocket
├── domain/        # Сущности, репозитории, enums
├── infrastructure/# Redis Geo, Distributed Lock, WebSocket
├── mapper/        # MapStruct мапперы
├── messaging/     # Kafka producer/consumer, события
├── security/      # JWT провайдер, фильтр, UserDetails
└── service/       # Бизнес-логика
```

## Статусы заказа

CREATED → ASSIGNED → PICKED_UP → IN_TRANSIT → DELIVERED
CREATED/ASSIGNED → CANCELLED

## Лицензия

MIT
