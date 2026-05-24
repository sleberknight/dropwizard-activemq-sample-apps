package org.kiwiproject.samples.activemq.consumer.model;

import lombok.Value;

import java.util.List;

@Value
public class ReceivedMessagesResponse {
    String serviceName;
    List<ReceivedMessage> messages;
}
