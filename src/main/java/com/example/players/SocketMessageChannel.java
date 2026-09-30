package com.example.players;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * Exchanges length-prefixed modified-UTF messages over a loopback TCP connection.
 * A frame is limited by writeUTF to 65,535 encoded bytes; this conversation is
 * much smaller. One caller owns each channel. Session methods own all sockets
 * and streams, close them on every exit, and bound connection/read inactivity.
 */
public final class SocketMessageChannel implements MessageChannel {
    private static final int TIMEOUT_MS = 10_000;
    private final DataInputStream input;
    private final DataOutputStream output;

    private SocketMessageChannel(DataInputStream input, DataOutputStream output) {
        this.input = input;
        this.output = output;
    }

    /** Runs protocol code while the transport's resources remain open. */
    @FunctionalInterface
    public interface Session {
        void run(MessageChannel channel) throws IOException, InterruptedException;
    }

    public static void serve(int port, Session session)
            throws IOException, InterruptedException {
        try (ServerSocket listener = new ServerSocket()) {
            listener.setReuseAddress(true);
            listener.bind(address(port));
            listener.setSoTimeout(TIMEOUT_MS);
            try (Socket socket = listener.accept()) {
                exchange(socket, session);
            }
        }
    }

    public static void connect(int port, Session session)
            throws IOException, InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MS);
        while (true) {
            try (Socket socket = new Socket()) {
                try {
                    socket.connect(address(port), TIMEOUT_MS);
                } catch (ConnectException refused) {
                    if (System.nanoTime() >= deadline) {
                        throw refused;
                    }
                    Thread.sleep(25);
                    continue;
                }
                // Retry only connection establishment, never a started conversation.
                exchange(socket, session);
                return;
            }
        }
    }

    private static InetSocketAddress address(int port) throws IOException {
        return new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port);
    }

    private static void exchange(Socket socket, Session session)
            throws IOException, InterruptedException {
        socket.setSoTimeout(TIMEOUT_MS);
        socket.setTcpNoDelay(true);
        try (DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
            session.run(new SocketMessageChannel(input, output));
        }
    }

    @Override
    public void send(String message) throws IOException {
        output.writeUTF(message);
        output.flush();
    }

    @Override
    public String receive() throws IOException {
        return input.readUTF();
    }
}
