package com.example.players;

import java.io.IOException;
import java.util.Objects;

/**
 * Owns one player's protocol state, counting successful sends and receives.
 * Confined to one caller thread and usable for exactly one conversation.
 * Replies append the outgoing message's one-based ordinal. The initiator's
 * terminal receive takes precedence over the normal reply rule.
 */
public final class Player {
    private static final int MESSAGE_LIMIT = 10;
    private final MessageChannel channel;
    private int sent;
    private int received;
    private boolean started;

    public Player(MessageChannel channel) {
        this.channel = Objects.requireNonNull(channel);
    }

    public void initiate(String initialMessage) throws IOException, InterruptedException {
        Objects.requireNonNull(initialMessage);
        converse(true, initialMessage);
    }

    public void respond() throws IOException, InterruptedException {
        converse(false, null);
    }

    private void converse(boolean initiator, String initialMessage)
            throws IOException, InterruptedException {
        if (started) {
            throw new IllegalStateException("A player can participate only once");
        }
        started = true;
        if (initiator) {
            send(initialMessage);
        }
        while (received < MESSAGE_LIMIT) {
            String message = channel.receive();
            received++;
            if (!initiator || sent < MESSAGE_LIMIT) {
                send(message + (sent + 1));
            }
        }
    }

    private void send(String message) throws IOException, InterruptedException {
        channel.send(message);
        sent++;
    }

    public int sentCount() {
        return sent;
    }

    public int receivedCount() {
        return received;
    }
}
