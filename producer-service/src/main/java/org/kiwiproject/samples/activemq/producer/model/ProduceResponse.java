package org.kiwiproject.samples.activemq.producer.model;

public record ProduceResponse(
        String serviceName,
        int sent,
        String destination,
        boolean sentToAllEventsQueue
) {
}
