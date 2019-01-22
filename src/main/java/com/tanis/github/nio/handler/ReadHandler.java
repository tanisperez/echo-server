package com.tanis.github.nio.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

public class ReadHandler implements Handler {

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
		final int read = socketChannel.read(buffer);
		if (read == -1) {
			this.pendingData.remove(socketChannel);
			socketChannel.close();
			System.out.println("Disconnected " + socketChannel);
			return;
		}

		if (read > 0) {
			this.threadPool.submit(() -> {
				System.out.println("Read from " + socketChannel + " in thread " + Thread.currentThread());
				this.pendingData.get(socketChannel).add(buffer);
				this.selectorActions.add(() -> selectionKey.interestOps(SelectionKey.OP_WRITE));
				selectionKey.selector().wakeup();
			});
		}
	}

}
