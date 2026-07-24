package org.kiwiproject.samples.activemq.producer.jms;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Session;
import org.kiwiproject.dropwizard.activemq.ActiveMqHelper;
import org.kiwiproject.dropwizard.activemq.config.ActiveMqConfig;

/**
 * Sends raw JMS message types that {@code ActiveMqProducer} cannot produce (it only supports
 * {@code TextMessage} and {@code BytesMessage}), so that dropwizard-activemq's handling of
 * unsupported message types can be functionally tested. Bypasses the library's producer/destination
 * abstractions entirely and talks directly to the broker.
 */
public class RawJmsMessageSender {

    private final ActiveMqHelper activeMqHelper = new ActiveMqHelper();
    private final ActiveMqConfig activeMqConfig;

    public RawJmsMessageSender(ActiveMqConfig activeMqConfig) {
        this.activeMqConfig = activeMqConfig;
    }

    /**
     * Sends a {@code MapMessage} directly to the physical queue backing the given destination string
     * (a "queue:" prefixed destination, matching the convention used elsewhere in this sample app).
     *
     * @param destination a destination string, e.g. "queue:notifications"
     * @param data        fields to set on the MapMessage as strings; may be null for an empty message
     */
    public void sendMapMessage(String destination, JsonNode data) {
        var physicalName = stripQueuePrefix(destination);
        var connectionFactory = activeMqHelper.newPooledConnectionFactory(activeMqConfig);

        try (var connection = connectionFactory.createConnection()) {
            connection.start();

            try (var session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                var queue = session.createQueue(physicalName);

                try (var producer = session.createProducer(queue)) {
                    var mapMessage = session.createMapMessage();
                    if (nonNull(data)) {
                        data.fields().forEachRemaining(entry ->
                                setMapMessageField(mapMessage, entry.getKey(), entry.getValue().asText()));
                    }
                    producer.send(mapMessage);
                }
            }
        } catch (JMSException e) {
            throw new RawJmsSendException("Failed to send MapMessage to " + destination, e);
        }
    }

    private static void setMapMessageField(MapMessage mapMessage, String key, String value) {
        try {
            mapMessage.setString(key, value);
        } catch (JMSException e) {
            throw new RawJmsSendException("Failed to set MapMessage field '" + key + "'", e);
        }
    }

    private static String stripQueuePrefix(String destination) {
        var prefix = "queue:";
        if (destination.startsWith(prefix)) {
            return destination.substring(prefix.length());
        }
        return destination;
    }

    public static class RawJmsSendException extends RuntimeException {
        public RawJmsSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
