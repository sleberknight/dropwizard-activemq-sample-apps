package org.kiwiproject.samples.activemq.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;
import org.kiwiproject.dropwizard.activemq.config.ActiveMqConfig;
import org.kiwiproject.dropwizard.activemq.config.ActiveMqConfigured;

@Getter
@Setter
public class ConsumerConfig extends Configuration implements ActiveMqConfigured {

    @NotBlank
    private String serviceName;

    private String instanceId;

    @Valid
    @NotNull
    @JsonProperty("activeMq")
    private ActiveMqConfig activeMqConfig = new ActiveMqConfig();

    private boolean registerDlqHealthCheck = false;

    @NonNull
    @Override
    public ActiveMqConfig getActiveMqConfig() {
        return activeMqConfig;
    }

    @Override
    public boolean isElucidationEnabled() {
        return false;
    }
}
