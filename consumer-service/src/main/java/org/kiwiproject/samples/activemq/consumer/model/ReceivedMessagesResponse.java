package org.kiwiproject.samples.activemq.consumer.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReceivedMessagesResponse(
        String serviceName,
        String instanceId,
        List<ReceivedMessage> messages,
        String respondedAt
) {
    public static ReceivedMessagesResponse of(String serviceName, String instanceId, List<ReceivedMessage> messages) {
        return new ReceivedMessagesResponse(serviceName, instanceId, messages,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now()));
    }
}
