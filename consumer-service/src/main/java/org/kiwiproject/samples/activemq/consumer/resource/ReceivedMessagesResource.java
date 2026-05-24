package org.kiwiproject.samples.activemq.consumer.resource;

import static java.util.Objects.nonNull;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.kiwiproject.samples.activemq.consumer.model.ReceivedMessagesResponse;
import org.kiwiproject.samples.activemq.consumer.store.ReceivedMessageStore;

@Path("/received")
@Produces(MediaType.APPLICATION_JSON)
public class ReceivedMessagesResource {

    private final String serviceName;
    private final ReceivedMessageStore store;

    public ReceivedMessagesResource(String serviceName, ReceivedMessageStore store) {
        this.serviceName = serviceName;
        this.store = store;
    }

    @GET
    public ReceivedMessagesResponse getReceived(@QueryParam("destination") String destination) {
        var messages = nonNull(destination) ? store.getByDestination(destination) : store.getAll();
        return new ReceivedMessagesResponse(serviceName, messages);
    }

    @DELETE
    public void clear() {
        store.clear();
    }
}
