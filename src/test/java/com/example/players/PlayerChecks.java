package com.example.players;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Checks protocol boundaries and state mutation without a test framework. */
public final class PlayerChecks {
    private PlayerChecks() { }

    public static void main(String[] args) throws Exception {
        ScriptedChannel initiatorChannel = new ScriptedChannel();
        Player initiator = new Player(initiatorChannel);
        initiator.initiate("hello");
        check(initiator.sentCount() == 10 && initiator.receivedCount() == 10);
        check(initiatorChannel.outgoing.size() == 10);
        check(initiatorChannel.outgoing.get(0).equals("hello"));
        for (int ordinal = 2; ordinal <= 10; ordinal++) {
            check(initiatorChannel.outgoing.get(ordinal - 1)
                    .equals("incoming-" + (ordinal - 1) + ":" + ordinal));
        }

        ScriptedChannel responderChannel = new ScriptedChannel();
        Player responder = new Player(responderChannel);
        responder.respond();
        check(responder.sentCount() == 10 && responder.receivedCount() == 10);
        check(responderChannel.outgoing.size() == 10);
        for (int ordinal = 1; ordinal <= 10; ordinal++) {
            check(responderChannel.outgoing.get(ordinal - 1)
                    .equals("incoming-" + ordinal + ":" + ordinal));
        }

        ScriptedChannel failingChannel = new ScriptedChannel();
        failingChannel.failAtSend = 3;
        Player failing = new Player(failingChannel);
        try {
            failing.initiate("hello");
            throw new AssertionError("Expected delivery failure");
        } catch (IOException expected) {
            check(failing.sentCount() == 2 && failing.receivedCount() == 2);
        }
        try {
            initiator.initiate("again");
            throw new AssertionError("Expected reuse rejection");
        } catch (IllegalStateException expected) {
            check(initiatorChannel.outgoing.size() == 10);
        }
        System.out.println("Protocol checks passed");
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError("Protocol check failed");
        }
    }

    /** Records sends and rejects any receive beyond the prescribed ten replies. */
    private static final class ScriptedChannel implements MessageChannel {
        private final List<String> outgoing = new ArrayList<>();
        private int received;
        private int failAtSend = -1;

        @Override
        public void send(String message) throws IOException {
            if (outgoing.size() + 1 == failAtSend) {
                throw new IOException("Simulated delivery failure");
            }
            outgoing.add(message);
        }

        @Override
        public String receive() {
            check(received < 10);
            return "incoming-" + (++received) + ":";
        }
    }
}
