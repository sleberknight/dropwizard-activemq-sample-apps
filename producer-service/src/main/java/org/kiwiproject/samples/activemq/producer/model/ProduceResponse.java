package org.kiwiproject.samples.activemq.producer.model;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public record ProduceResponse(
        String serviceName,
        int sent,
        String destination,
        boolean sentToAllEventsQueue,
        String respondedAt
) {
    public static ProduceResponse of(String serviceName, int sent, String destination, boolean sentToAllEventsQueue) {
        return new ProduceResponse(serviceName, sent, destination, sentToAllEventsQueue,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now()));
    }
}
