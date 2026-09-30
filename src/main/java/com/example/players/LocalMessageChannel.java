package com.example.players;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Connects two endpoints through bounded, in-memory FIFO queues.
 * Each direction holds at most one message, matching the alternating protocol.
 * Blocking operations are interruptible so the runner can cancel either peer
 * after a failure. No external resources or transport shutdown messages exist.
 */
public final class LocalMessageChannel implements MessageChannel {
    private final ArrayBlockingQueue<String> incoming;
    private final ArrayBlockingQueue<String> outgoing;

    private LocalMessageChannel(ArrayBlockingQueue<String> incoming,
                                ArrayBlockingQueue<String> outgoing) {
        this.incoming = incoming;
        this.outgoing = outgoing;
    }

    public static Pair pair() {
        var firstInbox = new ArrayBlockingQueue<String>(1);
        var secondInbox = new ArrayBlockingQueue<String>(1);
        return new Pair(new LocalMessageChannel(firstInbox, secondInbox),
                        new LocalMessageChannel(secondInbox, firstInbox));
    }

    @Override
    public void send(String message) throws InterruptedException {
        outgoing.put(Objects.requireNonNull(message));
    }

    @Override
    public String receive() throws InterruptedException {
        return incoming.take();
    }

    /** Publishes the two cross-connected endpoints without exposing their queues. */
    public record Pair(MessageChannel first, MessageChannel second) { }
}
