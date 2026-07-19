package org.kiwiproject.samples.activemq.producer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProduceRequest {

    public enum Format {
        JSON_CURRENT,
        JSON_LEGACY,
        JSON_ECHOED_CURRENT,
        JSON_ECHOED_LEGACY,
        JSON_CONFLICTING_TYPES,
        XML,
        TEXT,
        BYTES
    }

    @NotBlank
    private String destination;

    @JsonProperty("sendToAllEventsQueue")
    private boolean sendToAllEventsQueue = true;

    @Min(1)
    @Max(100)
    private int count = 1;

    @NotNull
    private Format format = Format.JSON_CURRENT;

    private String messageType;

    private JsonNode data;
}
