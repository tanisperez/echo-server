package com.tanis.github.nio.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AcceptHandler implements Handler {

	private final Map<SocketChannel, Queue<ByteBuffer>> pendingData;

	public AcceptHandler(final Map<SocketChannel, Queue<ByteBuffer>> pendingData) {
		this.pendingData = pendingData;
	}

	@Override
	public void handle(final SelectionKey selectionKey) throws IOException {
		final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();

		final SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		System.out.println("Connection accepted from " + socketChannel);

		this.pendingData.put(socketChannel, new ConcurrentLinkedQueue<ByteBuffer>());
		socketChannel.register(selectionKey.selector(), SelectionKey.OP_READ);
	}

}
