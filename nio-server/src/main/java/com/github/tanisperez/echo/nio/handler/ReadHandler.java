package com.github.tanisperez.echo.nio.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

public class ReadHandler implements Handler {

    /** LOGGER */
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadHandler.class);

    private final ExecutorService threadPool;
    private final Queue<Runnable> selectorActions;
    private final Map<SocketChannel, Queue<ByteBuffer>> pendingData;

    public ReadHandler(final ExecutorService threadPool, final Queue<Runnable> selectorActions,
                       final Map<SocketChannel, Queue<ByteBuffer>> pendingData) {
        this.threadPool = threadPool;
        this.selectorActions = selectorActions;
        this.pendingData = pendingData;
    }

    @Override
    public void handle(final SelectionKey selectionKey) throws IOException {
        final SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        final ByteBuffer buffer = ByteBuffer.allocateDirect(80);

        try {
            int read = socketChannel.read(buffer);
            if (read == -1) {
                removeConnection(socketChannel);
            } else if (read > 0) {
                this.threadPool.submit(() -> {
                    LOGGER.info("Read {} bytes from {}", read, socketChannel);

                    this.pendingData.get(socketChannel).add(buffer);
                    this.selectorActions.add(() -> selectionKey.interestOps(SelectionKey.OP_WRITE));
                    selectionKey.selector().wakeup();
                });
            }
        } catch (final IOException exception) {
            removeConnection(socketChannel); // Client forced to disconnect
        }
    }

    private void removeConnection(final SocketChannel socketChannel) throws IOException {
        this.pendingData.remove(socketChannel);
        socketChannel.close();

        LOGGER.info("Disconnected {}", socketChannel);
    }

}
