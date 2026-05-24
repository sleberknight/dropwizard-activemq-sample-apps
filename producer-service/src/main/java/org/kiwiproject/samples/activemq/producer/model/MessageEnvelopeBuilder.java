package org.kiwiproject.samples.activemq.producer.model;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MessageEnvelopeBuilder {

    private static final String MESSAGE_TYPE = "messageType";
    private final ObjectMapper mapper;

    public MessageEnvelopeBuilder(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String build(ProduceRequest request) {
        return switch (request.getFormat()) {
            case JSON_CURRENT -> buildJsonCurrent(request);
            case JSON_LEGACY -> buildJsonLegacy(request);
            case JSON_ECHOED_CURRENT -> buildJsonEchoedCurrent(request);
            case JSON_ECHOED_LEGACY -> buildJsonEchoedLegacy(request);
            case XML, TEXT, BYTES -> textPayload(request);
        };
    }

    private String buildJsonCurrent(ProduceRequest request) {
        ObjectNode envelope = mapper.createObjectNode();
        envelope.put(MESSAGE_TYPE, request.getMessageType());
        if (nonNull(request.getData())) {
            envelope.set("data", request.getData());
        }
        return envelope.toString();
    }

    private String buildJsonLegacy(ProduceRequest request) {
        ObjectNode envelope = mapper.createObjectNode();
        ObjectNode metaData = mapper.createObjectNode();
        metaData.put("type", request.getMessageType());
        envelope.set("metaData", metaData);
        if (nonNull(request.getData())) {
            envelope.set("data", request.getData());
        }
        return envelope.toString();
    }

    private String buildJsonEchoedCurrent(ProduceRequest request) {
        ObjectNode inner = mapper.createObjectNode();
        inner.put(MESSAGE_TYPE, request.getMessageType());
        if (nonNull(request.getData())) {
            inner.set("data", request.getData());
        }
        ObjectNode envelope = mapper.createObjectNode();
        envelope.put(MESSAGE_TYPE, "ECHO_MESSAGE");
        envelope.set("echoedMessage", inner);
        return envelope.toString();
    }

    private String buildJsonEchoedLegacy(ProduceRequest request) {
        ObjectNode metaData = mapper.createObjectNode();
        metaData.put("type", request.getMessageType());
        ObjectNode inner = mapper.createObjectNode();
        inner.set("metaData", metaData);
        if (nonNull(request.getData())) {
            inner.set("data", request.getData());
        }
        ObjectNode envelope = mapper.createObjectNode();
        envelope.put(MESSAGE_TYPE, "ECHO_MESSAGE");
        envelope.set("echoedMessage", inner);
        return envelope.toString();
    }

    private static String textPayload(ProduceRequest request) {
        if (isNull(request.getData())) {
            return "";
        }
        return request.getData().isTextual() ? request.getData().asText() : request.getData().toString();
    }
}
