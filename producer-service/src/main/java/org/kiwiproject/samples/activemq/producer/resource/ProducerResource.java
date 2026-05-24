package org.kiwiproject.samples.activemq.producer.resource;

import static java.nio.charset.StandardCharsets.UTF_8;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.kiwiproject.dropwizard.activemq.ActiveMqProducer;
import org.kiwiproject.samples.activemq.producer.model.MessageEnvelopeBuilder;
import org.kiwiproject.samples.activemq.producer.model.ProduceRequest;
import org.kiwiproject.samples.activemq.producer.model.ProduceRequest.Format;
import org.kiwiproject.samples.activemq.producer.model.ProduceResponse;

@Path("/produce")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ProducerResource {

    private final String serviceName;
    private final ActiveMqProducer producer;
    private final MessageEnvelopeBuilder envelopeBuilder;

    public ProducerResource(String serviceName, ActiveMqProducer producer, MessageEnvelopeBuilder envelopeBuilder) {
        this.serviceName = serviceName;
        this.producer = producer;
        this.envelopeBuilder = envelopeBuilder;
    }

    @POST
    public Response produce(@Valid @NotNull ProduceRequest request) {
        var payload = envelopeBuilder.build(request);
        var destination = request.getDestination();
        var format = request.getFormat();

        for (int i = 0; i < request.getCount(); i++) {
            if (format == Format.BYTES) {
                producer.produceBytesMessage(destination, payload.getBytes(UTF_8));
            } else if (request.isSendToAllEventsQueue()) {
                producer.produceToDestinationAndAllEventsQueue(destination, payload);
            } else {
                producer.produceToDestination(destination, payload);
            }
        }

        return Response.ok(new ProduceResponse(serviceName, request.getCount(), destination, request.isSendToAllEventsQueue())).build();
    }
}
