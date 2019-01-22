package com.tanis.github.nio.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.tanis.github.nio.handler.AcceptHandler;
import com.tanis.github.nio.handler.Handler;
import com.tanis.github.nio.handler.ReadHandler;
import com.tanis.github.nio.handler.WriteHandler;

public class EchoNonBlockingServer {

	public static void main(final String[] args) throws IOException {
		System.out.println("Starting up EchoNonBlockingServer");

		final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(8080));
		serverSocketChannel.configureBlocking(false);

		final Selector selector = Selector.open();
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

		final ExecutorService threadPool = Executors.newFixedThreadPool(10);

		final Map<SocketChannel, Queue<ByteBuffer>> pendingData = new ConcurrentHashMap<>();
		final Queue<Runnable> selectorActions = new ConcurrentLinkedQueue<>();

		final Handler acceptHandler = new AcceptHandler(pendingData);
		final Handler readHandler = new ReadHandler(threadPool, selectorActions, pendingData);
		final Handler writeHandler = new WriteHandler(pendingData);

		while (true) {
			selector.select();
			processSelectorActions(selectorActions);

			final Set<SelectionKey> keys = selector.selectedKeys();
			for (final Iterator<SelectionKey> iterator = keys.iterator(); iterator.hasNext();) {
				final SelectionKey key = iterator.next();
				iterator.remove();

				if (key.isValid()) {
					if (key.isAcceptable()) {
						acceptHandler.handle(key);
					} else if (key.isReadable()) {
						readHandler.handle(key);
					} else if (key.isWritable()) {
						writeHandler.handle(key);
					}
				}
			}

		}
	}

	private static void processSelectorActions(final Queue<Runnable> selectorActions) {
		while (!selectorActions.isEmpty()) {
			final Runnable action = selectorActions.poll();
			action.run();
		}
	}

}
