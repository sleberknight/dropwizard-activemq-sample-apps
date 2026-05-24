package org.kiwiproject.samples.activemq.producer.model;

import lombok.Value;

@Value
public class ProduceResponse {
    String serviceName;
    int sent;
    String destination;
    boolean sentToAllEventsQueue;
}
