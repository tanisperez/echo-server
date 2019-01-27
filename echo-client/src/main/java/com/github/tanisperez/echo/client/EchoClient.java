package com.github.tanisperez.echo.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;

public class EchoClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(EchoClient.class);

    private static final int MIN_STRING_LENGTH = 20;
    private static final int MAX_STRING_LENGTH = 180;

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final Random random = new Random();

    public static void main(String[] args) throws IOException {
        connectEchoServer("localhost", 8080);
    }

    private static void connectEchoServer(final String address, final int port) {
        try (final Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(address, port));
            sendEcho(socket);
        } catch (final IOException exception) {
            LOGGER.error(exception.getMessage());
        }
    }

    private static void sendEcho(Socket socket) {
        try (
                final InputStream in = socket.getInputStream();
                final OutputStream out = socket.getOutputStream()) {



            sendRandomMessage(in, out);
            sendRandomMessage(in, out);
            sendRandomMessage(in, out);
        } catch (final IOException exception) {
            LOGGER.error(exception.getMessage());
        }
    }

    private static void sendRandomMessage(InputStream in, OutputStream out) throws IOException {
        final byte[] buffer = new byte[MAX_STRING_LENGTH];

        final String sentMessage = generateRandomString();
        final int sentBytes = sentMessage.length();
        out.write(sentMessage.getBytes());
        LOGGER.info("Sent {} bytes", sentBytes);

        final StringBuilder receivedMessage = new StringBuilder();
        int totalRead= 0;
        while (totalRead < sentBytes) {
            int read = in.read(buffer);
            totalRead += read;
            receivedMessage.append(new String(buffer, 0, read));
        }
        if (sentMessage.equals(receivedMessage.toString())) {
            LOGGER.info("Message received OK");
        } else {
            LOGGER.error("Sent message \"{}\" and received message \"{}\" are not equals!", sentMessage, receivedMessage);
        }
    }

    private static String generateRandomString() {
        final int stringLength = random.nextInt(MAX_STRING_LENGTH - MIN_STRING_LENGTH) + MIN_STRING_LENGTH;
        final StringBuilder string = new StringBuilder(stringLength);
        for (int i = 0; i < stringLength; i++) {
            final int randomLetter = random.nextInt(ALPHABET.length());
            final char letter = ALPHABET.charAt(randomLetter);
            string.append(letter);
        }
        return string.toString();
    }
}
