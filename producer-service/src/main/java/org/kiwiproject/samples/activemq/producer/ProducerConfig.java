package org.kiwiproject.samples.activemq.producer;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.kiwiproject.dropwizard.activemq.config.ActiveMqConfig;
import org.kiwiproject.dropwizard.activemq.config.ActiveMqConfigured;

@Getter
@Setter
public class ProducerConfig extends Configuration implements ActiveMqConfigured {

    @NotBlank
    private String serviceName;

    @Valid
    @NotNull
    @JsonProperty("activeMq")
    private ActiveMqConfig activeMqConfig = new ActiveMqConfig();

    @Override
    public ActiveMqConfig getActiveMqConfig() {
        return activeMqConfig;
    }

    @Override
    public boolean isElucidationEnabled() {
        return false;
    }
}
