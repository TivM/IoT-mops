# MOPS IoT Demo (Java / Spring Boot)

Пример многоуровневой IoT-системы: приём сообщений, постановка в очередь, обработка правил, сохранение в MongoDB, метрики и наблюдаемость.

## Сервисы
- **iot-controller** — HTTP вход, валидация, запись в MongoDB (`iot_messages`), публикация в RabbitMQ (`iot.events` topic, routing `device.{deviceId}`).
- **rule-engine** — читает очередь (`iot.rule-engine` с routing `device.*`), применяет правила:  
  - мгновенное: `payload.a > 5`;  
  - длящееся окно: последние N=10 пакетов за не более 60s, условие `payload.a > 5` для всех;  
  алерты пишет в MongoDB (`alerts`).
- **data-simulator** — CLI-генератор трафика, шлёт HTTP в `iot-controller`.
- **infra** — MongoDB, RabbitMQ, Prometheus, Grafana, Elasticsearch+Kibana (логирование заготовлено, Filebeat/Logstash пайплайн можно добавить позже).

## Требования
- JDK 17, Maven.
- Docker + Docker Compose.

## Сборка
```bash
mvn clean package
```
JAR-файлы появятся в `iot-controller/target`, `rule-engine/target`, `data-simulator/target`.

## Запуск через Docker Compose
```bash
docker compose up --build
```
Сервисы поднимаются в одной сети. Порты:
- HTTP вход: `http://localhost:8080` (iot-controller)
- Rule engine actuator: `http://localhost:8081`
- RabbitMQ UI: `http://localhost:15672` (guest/guest)
- MongoDB: `localhost:27017` (db `iot`)
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (admin/admin)
- Kibana: `http://localhost:5601`

## Ручной запуск без Docker
```bash
# отдельными окнами, при уже запущенных Mongo+Rabbit
java -jar iot-controller/target/iot-controller-0.0.1-SNAPSHOT.jar
java -jar rule-engine/target/rule-engine-0.0.1-SNAPSHOT.jar
java -jar data-simulator/target/data-simulator-0.0.1-SNAPSHOT.jar
```
Настройки можно менять через переменные окружения Spring (например `SPRING_DATA_MONGODB_URI`, `SPRING_RABBITMQ_HOST`).

## API iot-controller
### POST `/api/ingest`
- **Описание:** принять пакет от устройства.
- **Request body (application/json):**
```json
{
  "deviceId": "device-1",
  "ts": "2024-01-01T12:00:00Z",
  "payload": {
    "a": 4.2,
    "b": 1
  }
}
```
- **Ответ:** `202 Accepted` без тела.
- **Валидация:** `deviceId` не пустой, `ts` и `payload` обязательны.
- **Что происходит дальше:** запись в Mongo `iot_messages`, отправка в RabbitMQ exchange `iot.events` с routing `device.{deviceId}`.

### Метрики/здоровье
- `GET /actuator/health`
- `GET /actuator/prometheus`

## Rule Engine
- Слушает очередь `iot.rule-engine`.
- Правила:
  - **Мгновенное:** `payload.a > 5` → алерт в `alerts`.
  - **Длящееся:** окно N=10 последних пакетов устройства за не более 60s, если все `payload.a > 5` → алерт.
- Очистка окон по расписанию (каждые 60s) от старых пакетов.
- Конфигурация по умолчанию в `rule-engine/src/main/resources/application.yml`:
  - `app.rules.window-size` (по умолчанию 10)
  - `app.rules.max-window-age-sec` (по умолчанию 60)

## Data Simulator
Параметры (application.yml или env):
- `SIMULATOR_DEVICES` — число устройств (по умолчанию 10)
- `SIMULATOR_RATE` — сообщений в секунду с устройства (по умолчанию 1)
- `SIMULATOR_DURATION_SECONDS` — длительность работы (по умолчанию 60)
- `SIMULATOR_TARGET_URL` — URL для POST (по умолчанию `http://iot-controller:8080/api/ingest`)

Пример запуска с override:
```bash
SIMULATOR_DEVICES=100 SIMULATOR_RATE=2 java -jar data-simulator/target/data-simulator-0.0.1-SNAPSHOT.jar
```

## Хранилище
- MongoDB коллекции:
  - `iot_messages`: `{ deviceId, ts, payload, ingestedAt, correlationId }`
  - `alerts`: `{ ruleId, deviceId, kind, windowSize, condition, triggeredAt, payloadSnapshot, correlationId }`

## Наблюдаемость
- Prometheus собирает метрики с `/actuator/prometheus` у controller и rule-engine (конфиг `monitoring/prometheus.yml`).
- Grafana подключена к Prometheus (порт 3000).
- ELK: Elasticsearch+Kibana запущены; добавление Filebeat/Logstash-конфига для JSON-логов возможно отдельно.

## Очереди
- Exchange: `iot.events` (topic)
- Queue: `iot.rule-engine`
- Routing: `device.*`

## Типичные проблемы
- При сборке Docker убедитесь, что JARы собраны (иначе `COPY target/...` не найдёт файл).

