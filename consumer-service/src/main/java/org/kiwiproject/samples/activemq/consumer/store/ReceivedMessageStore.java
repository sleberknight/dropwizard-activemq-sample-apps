package org.kiwiproject.samples.activemq.consumer.store;

import org.kiwiproject.samples.activemq.consumer.model.ReceivedMessage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReceivedMessageStore {

    private final CopyOnWriteArrayList<ReceivedMessage> messages = new CopyOnWriteArrayList<>();

    public void add(ReceivedMessage message) {
        messages.add(message);
    }

    public List<ReceivedMessage> getAll() {
        return List.copyOf(messages);
    }

    public List<ReceivedMessage> getByDestination(String destination) {
        return messages.stream()
                .filter(m -> destination.equals(m.getDestination()))
                .toList();
    }

    public void clear() {
        messages.clear();
    }

    public int size() {
        return messages.size();
    }
}
