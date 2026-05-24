# dw-activemq-sample-apps

Sample Dropwizard services for functional testing of dropwizard-activemq before release.

## Prerequisites

- Java 17
- Maven 3.9+
- Docker with Compose

## Setup

**1. Install dropwizard-activemq to your local Maven repo:**

```bash
cd /path/to/dropwizard-activemq
mvn install -DskipTests
```

**2. Build the fat JARs:**

```bash
cd dw-activemq-sample-apps
mvn package -DskipTests
```

**3. Start everything:**

```bash
cd docker
docker compose up --build
```

## Services

| Service | App port | Admin port |
|---|---|---|
| ActiveMQ | 61616 (TCP), 8161 (UI) | — |
| producer | 8080 | 8090 |
| consumer-alpha | 8081 | 8091 |
| consumer-beta | 8082 | 8092 |

## Message Topology

- **Producer** sends to `topic:orders`, `fixedtopic:announcements`, and `queue:poison_pill`
- **Consumer Alpha** subscribes to `topic:orders` and `fixedtopic:announcements`
- **Consumer Beta** subscribes to `queue:all_events` and `fixedtopic:announcements`
- `topic:orders` is a virtual topic: consumer-alpha receives via its own durable queue; `queue:all_events`
  gets a copy when `sendToAllEventsQueue: true`
- `fixedtopic:announcements` is a plain JMS topic: both consumer-alpha and consumer-beta receive every message
- `queue:poison_pill` has no consumer; messages expire after 30 seconds (configured TTL) and move to `ActiveMQ.DLQ`

## Producer API

### POST /produce

Request:

```json
{
  "destination": "topic:orders",
  "sendToAllEventsQueue": true,
  "count": 1,
  "format": "JSON_CURRENT",
  "messageType": "ORDER_PLACED",
  "data": { "orderId": "123", "total": 49.99 }
}
```

Response:

```json
{
  "serviceName": "producer-service",
  "sent": 1,
  "destination": "topic:orders",
  "sentToAllEventsQueue": true
}
```

**format values:**

| Format | Description |
|---|---|
| `JSON_CURRENT` | `{"messageType":"...", "data":{...}}` |
| `JSON_LEGACY` | `{"metaData":{"type":"..."}, "data":{...}}` |
| `JSON_ECHOED_CURRENT` | `{"messageType":"ECHO_MESSAGE","echoedMessage":{"messageType":"...",...}}` |
| `JSON_ECHOED_LEGACY` | `{"messageType":"ECHO_MESSAGE","echoedMessage":{"metaData":{"type":"..."},...}}` |
| `XML` | `data` sent as TEXT_MESSAGE (pass XML string as `"data"` value) |
| `TEXT` | `data` sent as TEXT_MESSAGE (pass plain string as `"data"` value) |
| `BYTES` | `data` sent as BytesMessage (pass string as `"data"` value, UTF-8 encoded) |

For BYTES format, `sendToAllEventsQueue` is ignored — bytes go to the specified destination only.

### Example: send an XML message

```json
{
  "destination": "topic:orders",
  "sendToAllEventsQueue": false,
  "format": "XML",
  "data": "<order><id>123</id></order>"
}
```

### Example: send an announcement (plain JMS topic, fan-out to both consumers)

```json
{
  "destination": "fixedtopic:announcements",
  "sendToAllEventsQueue": false,
  "format": "JSON_CURRENT",
  "messageType": "SYSTEM_NOTICE",
  "data": { "message": "Scheduled maintenance at midnight" }
}
```

### Example: trigger DLQ (poison pill)

```json
{
  "destination": "queue:poison_pill",
  "sendToAllEventsQueue": false,
  "format": "TEXT",
  "data": "this will expire"
}
```

`queue:poison_pill` has no consumer. Messages expire after 30 seconds and move to `ActiveMQ.DLQ`.
Consumer Alpha's `dead-letter-queue` health check at `http://localhost:8091/healthcheck` will go
unhealthy once the message appears there.

## Consumer API

### GET /received

Returns all received messages. Optional `?destination=topic:orders` filter.

```json
{
  "serviceName": "consumer-alpha",
  "messages": [
    {
      "destination": "topic:orders",
      "rawPayload": "{\"messageType\":\"ORDER_PLACED\",\"data\":{\"orderId\":\"123\"}}",
      "parsedMessageType": "ORDER_PLACED",
      "contentType": "JSON",
      "receivedAt": "2026-05-23T14:30:00Z"
    }
  ]
}
```

### DELETE /received

Clears all received messages.

### DELETE /dlq

Purges all messages from `ActiveMQ.DLQ` via Jolokia. Use this to reset the dead-letter queue after
testing DLQ behavior so the `dead-letter-queue` health check returns to healthy.

## Health Checks

- Producer: `http://localhost:8090/healthcheck` — includes `dead-letter-queue` check
- Consumer Alpha: `http://localhost:8091/healthcheck` — includes `dead-letter-queue` check
- Consumer Beta: `http://localhost:8092/healthcheck`

## ActiveMQ Admin UI

`http://localhost:8161` — username: `admin`, password: `admin`
