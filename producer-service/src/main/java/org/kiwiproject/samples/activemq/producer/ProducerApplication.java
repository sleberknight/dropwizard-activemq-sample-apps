package org.kiwiproject.samples.activemq.producer;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.kiwiproject.dropwizard.activemq.DropwizardActiveMq;
import org.kiwiproject.dropwizard.activemq.health.DeadLetterQueueHealthCheck;
import org.kiwiproject.samples.activemq.producer.model.MessageEnvelopeBuilder;
import org.kiwiproject.samples.activemq.producer.resource.ProducerResource;

public class ProducerApplication extends Application<ProducerConfig> {

    public static void main(String[] args) throws Exception {
        new ProducerApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<ProducerConfig> bootstrap) {
    }

    @Override
    public void run(ProducerConfig config, Environment environment) {
        var dropwizardActiveMq = DropwizardActiveMq.<ProducerConfig>builder()
                .configuration(config)
                .environment(environment)
                .build();

        var producer = dropwizardActiveMq.startProducers();

        environment.healthChecks().register("dead-letter-queue",
                new DeadLetterQueueHealthCheck(config.getActiveMqConfig()));

        var envelopeBuilder = new MessageEnvelopeBuilder(environment.getObjectMapper());
        environment.jersey().register(new ProducerResource(config.getServiceName(), producer, envelopeBuilder));
    }
}
