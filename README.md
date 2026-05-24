# dw-activemq-sample-apps

Sample Dropwizard services for functional testing of dropwizard-activemq before release.

An IntelliJ HTTP client file covering all endpoints is at [`etc/dw_activemq_sample_apps-endpoints.http`](etc/dw_activemq_sample_apps-endpoints.http).

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

| Destination | Type | Producer | Consumers | Behavior |
|---|---|---|---|---|
| `topic:orders` | Virtual topic | producer-service | consumer-alpha | Each subscriber gets its own durable queue copy; consumer-alpha receives via `Consumer.consumer-alpha.VirtualTopic.orders` |
| `fixedtopic:announcements` | Plain JMS topic | producer-service | consumer-alpha, consumer-beta | Every subscriber receives every message |
| `queue:notifications` | Queue | producer-service | consumer-alpha, consumer-beta | Competing consumers — each message delivered to exactly one instance |
| `queue:all_events` | Queue | producer-service (side effect) | consumer-beta | Receives a copy of every message sent with `sendToAllEventsQueue: true` |
| `queue:poison_pill` | Queue | producer-service | none | No consumer; messages expire after 30s TTL and move to `ActiveMQ.DLQ` |

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
