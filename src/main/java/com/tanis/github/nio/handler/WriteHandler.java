package com.tanis.github.nio.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Queue;

public class WriteHandler implements Handler {

    private final Map<SocketChannel, Queue<ByteBuffer>> pendingData;

    public WriteHandler(final Map<SocketChannel, Queue<ByteBuffer>> pendingData) {
        this.pendingData = pendingData;
    }

    @Override
    public void handle(final SelectionKey selectionKey) throws IOException {
        final SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        final Queue<ByteBuffer> queue = this.pendingData.get(socketChannel);
        while (!queue.isEmpty()) {
            final ByteBuffer buffer = queue.peek();
            buffer.flip(); // pos = 0, limit = received bytes
            final int written = socketChannel.write(buffer);
            System.out.println("Sent " + written + " bytes to " + socketChannel);
            if (written == -1) {
                socketChannel.close();
                this.pendingData.remove(socketChannel);
                return;
            }
            if (buffer.hasRemaining()) {
                return;
            }
            queue.remove();
        }
        selectionKey.interestOps(SelectionKey.OP_READ);
    }

}
