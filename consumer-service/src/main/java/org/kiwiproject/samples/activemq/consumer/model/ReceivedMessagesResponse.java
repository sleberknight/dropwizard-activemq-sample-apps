package org.kiwiproject.samples.activemq.consumer.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReceivedMessagesResponse(
        String serviceName,
        String instanceId,
        List<ReceivedMessage> messages
) {
}
