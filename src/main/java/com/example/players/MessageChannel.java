package com.example.players;

import java.io.IOException;

/**
 * Exchanges complete messages with one peer, hiding delivery and framing.
 * Each endpoint has one owning player. Operations may block and must surface
 * delivery failures; a successful send does not imply peer acknowledgement.
 * Resource lifetime belongs to the transport runner, not the player.
 */
public interface MessageChannel {
    void send(String message) throws IOException, InterruptedException;

    String receive() throws IOException, InterruptedException;
}
