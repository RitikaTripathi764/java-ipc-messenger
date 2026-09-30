package com.example.players;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Composes players with transports and owns local worker lifecycle.
 * Successful local completion joins both workers; failure interrupts blocked
 * queue operations and joins before returning. Reports process identity and
 * final counts without putting scheduling or output responsibilities in Player.
 */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        try {
            String mode = args.length == 0 ? "same" : args[0];
            if (args.length > 2 || (mode.equals("same") && args.length > 1)) {
                throw new IllegalArgumentException("Usage: same | server [port] | client [port]");
            }
            int port = args.length == 2 ? Integer.parseInt(args[1]) : 5000;
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535");
            }
            switch (mode) {
                case "same" -> runLocal();
                case "server" -> SocketMessageChannel.serve(port,
                        channel -> System.out.println(play(channel, false)));
                case "client" -> SocketMessageChannel.connect(port,
                        channel -> System.out.println(play(channel, true)));
                default -> throw new IllegalArgumentException("Unknown mode: " + mode);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            System.err.println("Conversation interrupted");
            System.exit(1);
        } catch (IOException | ExecutionException | TimeoutException
                 | IllegalArgumentException failure) {
            Throwable cause = failure instanceof ExecutionException
                    ? failure.getCause() : failure;
            System.err.println("Conversation failed: " + cause);
            System.exit(1);
        }
    }

    private static void runLocal()
            throws InterruptedException, ExecutionException, TimeoutException {
        LocalMessageChannel.Pair channels = LocalMessageChannel.pair();
        var workers = Executors.newFixedThreadPool(2);
        var completed = new ExecutorCompletionService<String>(workers);
        try {
            completed.submit(() -> play(channels.first(), true));
            completed.submit(() -> play(channels.second(), false));
            for (int i = 0; i < 2; i++) {
                var result = completed.poll(15, TimeUnit.SECONDS);
                if (result == null) {
                    throw new TimeoutException("Local conversation did not finish");
                }
                System.out.println(result.get());
            }
        } finally {
            workers.shutdownNow();
            if (!workers.awaitTermination(5, TimeUnit.SECONDS)) {
                throw new TimeoutException("Local workers did not terminate");
            }
        }
    }

    private static String play(MessageChannel channel, boolean initiator)
            throws IOException, InterruptedException {
        Player player = new Player(channel);
        if (initiator) {
            player.initiate("hello");
        } else {
            player.respond();
        }
        return "%s pid=%d sent=%d received=%d".formatted(
                initiator ? "initiator" : "responder",
                ProcessHandle.current().pid(), player.sentCount(), player.receivedCount());
    }
}
