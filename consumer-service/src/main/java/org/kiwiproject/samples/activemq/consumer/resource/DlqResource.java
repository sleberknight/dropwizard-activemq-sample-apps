package org.kiwiproject.samples.activemq.consumer.resource;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.kiwiproject.dropwizard.activemq.config.ActiveMqConfig;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Path("/dlq")
@Produces(MediaType.APPLICATION_JSON)
public class DlqResource {

    private static final Pattern HOST_PATTERN = Pattern.compile(".*?//(.*?)[:?/].*");

    private final WebTarget purgeTarget;
    private final String basicAuth;

    public DlqResource(ActiveMqConfig config, Client httpClient) {
        var scheme = config.isUseSecureRestConnections() ? "https" : "http";
        var host = extractHost(config.getBrokerUri());
        var port = config.getJolokiaPort();
        var dlqName = config.getHealthConfig().getDlqName();

        var purgeUrl = "%s://%s:%d/api/jolokia/exec/org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Queue,destinationName=%s/purge()"
                .formatted(scheme, host, port, dlqName);

        var credentials = config.getHealthConfig().getJmxUser() + ":" + config.getHealthConfig().getJmxCred();
        this.basicAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        this.purgeTarget = httpClient.target(purgeUrl);
    }

    @DELETE
    public Response clear() {
        try {
            var response = purgeTarget.request()
                    .header("Authorization", basicAuth)
                    .get();
            int status = response.getStatus();
            if (status == 200) {
                return Response.noContent().build();
            }
            LOG.warn("Jolokia purge returned unexpected status {}", status);
            return Response.serverError()
                    .entity(Map.of("error", "Jolokia returned status " + status))
                    .build();
        } catch (Exception e) {
            LOG.error("Failed to purge DLQ", e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    private static String extractHost(String brokerUri) {
        var matcher = HOST_PATTERN.matcher(brokerUri);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Cannot extract host from brokerUri: " + brokerUri);
    }
}
