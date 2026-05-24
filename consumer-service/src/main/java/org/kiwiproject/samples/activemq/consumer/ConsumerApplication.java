package org.kiwiproject.samples.activemq.consumer;

import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.kiwiproject.dropwizard.activemq.DropwizardActiveMq;
import org.kiwiproject.dropwizard.activemq.health.DeadLetterQueueHealthCheck;
import org.kiwiproject.samples.activemq.consumer.handler.MessageStoreConsumer;
import org.kiwiproject.samples.activemq.consumer.resource.DlqResource;
import org.kiwiproject.samples.activemq.consumer.resource.ReceivedMessagesResource;
import org.kiwiproject.samples.activemq.consumer.store.ReceivedMessageStore;

public class ConsumerApplication extends Application<ConsumerConfig> {

    public static void main(String[] args) throws Exception {
        new ConsumerApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<ConsumerConfig> bootstrap) {
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(),
                new EnvironmentVariableSubstitutor(false)));
    }

    @Override
    public void run(ConsumerConfig config, Environment environment) {
        var store = new ReceivedMessageStore();

        var dropwizardActiveMq = DropwizardActiveMq.<ConsumerConfig>builder()
                .configuration(config)
                .environment(environment)
                .build();

        config.getActiveMqConfig().getConsumers().forEach(destination ->
                dropwizardActiveMq.startConsumer(new MessageStoreConsumer(destination, store), destination));

        if (config.isRegisterDlqHealthCheck()) {
            environment.healthChecks().register("dead-letter-queue",
                    new DeadLetterQueueHealthCheck(config.getActiveMqConfig()));
        }

        environment.jersey().register(new ReceivedMessagesResource(config.getServiceName(), config.getInstanceId(), store));
        environment.jersey().register(new DlqResource(config.getActiveMqConfig()));
    }
}
