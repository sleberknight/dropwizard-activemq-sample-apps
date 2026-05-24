package org.kiwiproject.samples.activemq.consumer.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class ReceivedMessage {
    String destination;
    String rawPayload;
    String parsedMessageType;
    String contentType;
    Instant receivedAt;
}
