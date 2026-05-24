package org.kiwiproject.samples.activemq.consumer.model;

import lombok.Value;

import java.util.List;

@Value
public class ReceivedMessagesResponse {
    String serviceName;
    String instanceId;
    List<ReceivedMessage> messages;
}
