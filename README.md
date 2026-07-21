# Dropwizard ActiveMQ Sample Apps

[![build](https://github.com/sleberknight/dropwizard-activemq-sample-apps/actions/workflows/build.yml/badge.svg)](https://github.com/sleberknight/dropwizard-activemq-sample-apps/actions/workflows/build.yml)

Sample Dropwizard services for functional testing of [dropwizard-activemq](https://github.com/kiwiproject/dropwizard-activemq).

API collections covering all endpoints are in the `etc/` directory:

- curl script: [`etc/curl-endpoints.sh`](etc/curl-endpoints.sh) — interactive menu-driven client, useful in Codespaces or anywhere without a GUI tool
- IntelliJ HTTP client: [`etc/dw_activemq_sample_apps-endpoints.http`](etc/dw_activemq_sample_apps-endpoints.http)
- Postman collection: [`etc/dw_activemq_sample_apps.postman_collection.json`](etc/dw_activemq_sample_apps.postman_collection.json)
- Bruno collection: [`etc/bruno/`](etc/bruno/) (open the `etc/bruno` folder in Bruno)

## Prerequisites

- Java 17
- Maven 3.9+
- Docker with Compose

## Setup

### Quick setup

```bash
./bin/setup.sh
cd docker && docker compose up --build
```

`bin/setup.sh` builds the sample app JARs. It optionally clones and locally installs dropwizard-activemq if you want to make or test local changes to it.

### Manual setup

**1. Install dropwizard-activemq to your local Maven repo:**

```bash
cd /path/to/dropwizard-activemq
mvn install -DskipTests
```

**2. Build the fat JARs:**

```bash
cd /path/to/dropwizard-activemq-sample-apps
mvn package -DskipTests
```

**3. Start everything:**

```bash
cd docker
docker compose up --build
```

## Services

| Service          | App port               | Admin port |
|------------------|------------------------|------------|
| ActiveMQ         | 61616 (TCP), 8161 (UI) | —          |
| producer         | 8080                   | 8090       |
| consumer-alpha-1 | 8081                   | 8091       |
| consumer-alpha-2 | 8082                   | 8092       |
| consumer-beta    | 8083                   | 8093       |

## Message Topology

| Destination                | Type            | Producer                       | Consumers                                         | Behavior                                                                                                              |
|----------------------------|-----------------|--------------------------------|---------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| `topic:orders`             | Virtual topic   | producer-service               | consumer-alpha-1, consumer-alpha-2, consumer-beta | Alpha instances compete (only one gets each message); beta gets every message independently via its own durable queue |
| `fixedtopic:announcements` | Plain JMS topic | producer-service               | consumer-alpha-1, consumer-alpha-2, consumer-beta | Every subscriber receives every message                                                                               |
| `queue:notifications`      | Queue           | producer-service               | consumer-alpha-1, consumer-alpha-2, consumer-beta | Competing consumers — each message delivered to exactly one instance                                                  |
| `queue:all_events`         | Queue           | producer-service (side effect) | consumer-beta                                     | Receives a copy of every message sent with `sendToAllEventsQueue: true`                                               |
| `queue:poison_pill`        | Queue           | producer-service               | none                                              | No consumer; messages expire after 30s TTL and move to `ActiveMQ.DLQ`                                                 |

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
  "sentToAllEventsQueue": true,
  "respondedAt": "2026-05-24T19:13:32-04:00"
}
```

**format values:**

| Format                | Description                                                                      |
|-----------------------|----------------------------------------------------------------------------------|
| `JSON_CURRENT`        | `{"messageType":"...", "data":{...}}`                                            |
| `JSON_LEGACY`         | `{"metaData":{"type":"..."}, "data":{...}}`                                      |
| `JSON_ECHOED_CURRENT` | `{"messageType":"ECHO_MESSAGE","echoedMessage":{"messageType":"...",...}}`       |
| `JSON_ECHOED_LEGACY`  | `{"messageType":"ECHO_MESSAGE","echoedMessage":{"metaData":{"type":"..."},...}}` |
| `JSON_CONFLICTING_TYPES` | `{"messageType":"...", "metaData":{"type":"..._LEGACY"}, "data":{...}}` — sets both to different values, triggering `MessageTypeParser`'s `VerifyException` |
| `XML`                 | `data` sent as TEXT_MESSAGE (pass XML string as `"data"` value)                  |
| `TEXT`                | `data` sent as TEXT_MESSAGE (pass plain string as `"data"` value)                |
| `BYTES`               | `data` sent as BytesMessage (pass string as `"data"` value, UTF-8 encoded)       |

For BYTES format, `sendToAllEventsQueue` is ignored — bytes go to the specified destination only.
When a consumer receives a BYTES message, `rawPayload` in the response is base64-encoded.

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

### Example: trigger a message-processing failure (conflicting types)

```json
{
  "destination": "topic:orders",
  "sendToAllEventsQueue": false,
  "format": "JSON_CONFLICTING_TYPES",
  "messageType": "ORDER_PLACED",
  "data": { "orderId": "789", "total": 29.99 }
}
```

Sets both the current-format `messageType` and the legacy `metaData.type` to different values,
so `MessageTypeParser` finds more than one distinct type and throws `VerifyException`. Sent to
`topic:orders`, which has active consumers, so this exercises dropwizard-activemq's live-consumption
failure handling (as opposed to `queue:poison_pill`, which tests TTL-expiry dead-lettering — a
different mechanism entirely). Watch `docker compose logs -f consumer-alpha-1` for the failure and
recovery cycle, and consumer-alpha's health check for a brief unhealthy blip while it recovers.

## Consumer API

### GET /received

Returns all received messages. Optional `?destination=topic:orders` filter.

`instanceId` is included when set via the `instanceId` config property (or `INSTANCE_ID` environment
variable). It is omitted when not configured — useful for single-instance services like consumer-beta.

```json
{
  "serviceName": "consumer-alpha",
  "instanceId": "consumer-alpha-1",
  "messageCount": 1,
  "messages": [
    {
      "destination": "topic:orders",
      "rawPayload": "{\"messageType\":\"ORDER_PLACED\",\"data\":{\"orderId\":\"123\"}}",
      "parsedMessageType": "ORDER_PLACED",
      "contentType": "JSON",
      "receivedAt": "2026-05-23T14:30:00Z"
    }
  ],
  "respondedAt": "2026-05-24T19:13:32-04:00"
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
- Consumer Beta: `http://localhost:8093/healthcheck`

## ActiveMQ Admin UI

`http://localhost:8161` — username: `admin`, password: `admin`
