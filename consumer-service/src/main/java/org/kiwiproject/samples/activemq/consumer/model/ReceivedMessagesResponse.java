package org.kiwiproject.samples.activemq.consumer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Value;

import java.util.List;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReceivedMessagesResponse {
    String serviceName;
    String instanceId;
    List<ReceivedMessage> messages;
}
