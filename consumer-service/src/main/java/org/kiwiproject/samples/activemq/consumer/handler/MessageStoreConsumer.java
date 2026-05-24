package org.kiwiproject.samples.activemq.consumer.handler;

import lombok.extern.slf4j.Slf4j;
import org.kiwiproject.dropwizard.activemq.ActiveMqConsumer;
import org.kiwiproject.dropwizard.activemq.ActiveMqMessage;
import org.kiwiproject.samples.activemq.consumer.model.ReceivedMessage;
import org.kiwiproject.samples.activemq.consumer.store.ReceivedMessageStore;

import java.time.Instant;

@Slf4j
public class MessageStoreConsumer implements ActiveMqConsumer {

    private final String destination;
    private final ReceivedMessageStore store;

    public MessageStoreConsumer(String destination, ReceivedMessageStore store) {
        this.destination = destination;
        this.store = store;
    }

    @Override
    public Result consume(ActiveMqMessage message) {
        var rawPayload = message.getBody().orElse("");
        var parsedMessageType = message.getMessageType().orElse("UNKNOWN");
        var contentType = message.getContentType().map(Enum::name).orElse("UNKNOWN");

        LOG.info("Received {} message on {}: type={}", contentType, destination, parsedMessageType);

        store.add(ReceivedMessage.builder()
                .destination(destination)
                .rawPayload(rawPayload)
                .parsedMessageType(parsedMessageType)
                .contentType(contentType)
                .receivedAt(Instant.now())
                .build());

        return Result.CONSUMED;
    }
}
