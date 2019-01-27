package com.github.tanisperez.echo.nio.server;

import java.io.IOException;
import java.io.UncheckedIOException;
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
import java.util.concurrent.*;

import com.github.tanisperez.echo.nio.handler.AcceptHandler;
import com.github.tanisperez.echo.nio.handler.Handler;
import com.github.tanisperez.echo.nio.handler.ReadHandler;
import com.github.tanisperez.echo.nio.handler.WriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoNonBlockingServer extends Thread {

    /**
     * LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(EchoNonBlockingServer.class);

    private final int port;
    private final int numberOfThreads;

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    private ExecutorService threadPool;
    private Queue<Runnable> selectorActions;
    private Map<SocketChannel, Queue<ByteBuffer>> pendingData;

    private Handler acceptHandler;
    private Handler readHandler;
    private Handler writeHandler;

    /**
     * Constructor of the {@code EchoNonBlockinServer}.
     *
     * @param port            The port.
     * @param numberOfThreads ThreadPool number of threads.
     */
    public EchoNonBlockingServer(final int port, final int numberOfThreads) {
        this.port = port;
        this.numberOfThreads = numberOfThreads;
    }

    @Override
    public void run() {
        LOGGER.info("Starting up EchoNonBlockingServer at port {} with {} threads", this.port, this.numberOfThreads);
        initServer();

        while (!this.isInterrupted()) {
            try {
                this.selector.select();
                processSelectorActions();

                final Set<SelectionKey> keys = selector.selectedKeys();
                for (final Iterator<SelectionKey> iterator = keys.iterator(); iterator.hasNext(); ) {
                    final SelectionKey key = iterator.next();
                    iterator.remove();

                    evaluateSelectionKey(key);
                }
            } catch (final IOException exception) {
                LOGGER.error(exception.getMessage());
            }
        }

        cleanUpServer();
    }

    /**
     * CleanUp the server resources gracefully.
     */
    private void cleanUpServer() {
        LOGGER.info("Stopping ThreadPool....");
        this.threadPool.shutdownNow();

        LOGGER.info("Cleaning server structures...");
        this.selectorActions.clear();
        this.pendingData.clear();

        LOGGER.info("Closing selector and stopping ServerSocketChannel...");
        try {
            this.selector.close();
            this.serverSocketChannel.close();
        } catch (final IOException exception) {
            LOGGER.error(exception.getMessage());
        }
    }

    /**
     * Initialize the {@code ServerSocketChannel}, register the {@Selector}, create the ThreadPool and the
     * non blocking structures.
     *
     * @throws UncheckedIOException when an {@code IOException} happens.
     */
    private void initServer() {
        try {
            createServerSocket();
            registerSelector();

            this.threadPool = Executors.newFixedThreadPool(this.numberOfThreads);
            createNonBlockingStructures();
        } catch (final IOException exception) {
            LOGGER.error(exception.getMessage());
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * Create the {@code ServerSocketChannel} at a specified port without blocking.
     *
     * @throws IOException
     */
    private void createServerSocket() throws IOException {
        this.serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(8080));
        serverSocketChannel.configureBlocking(false);
    }

    /**
     * Register a Selector for the {@code ServerSocketChannel} for the {@code SelectionKey.OP_ACCEPT}.
     *
     * @throws IOException
     */
    private void registerSelector() throws IOException {
        this.selector = Selector.open();
        this.serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
    }

    /**
     * Create server structures such as {@code ConcurrentLinkedQueue}, {@code ConcurrentHashMap} and handlers.
     */
    private void createNonBlockingStructures() {
        this.selectorActions = new ConcurrentLinkedQueue<>();
        this.pendingData = new ConcurrentHashMap<>();

        this.acceptHandler = new AcceptHandler(pendingData);
        this.readHandler = new ReadHandler(threadPool, selectorActions, pendingData);
        this.writeHandler = new WriteHandler(pendingData);
    }

    /**
     * Execute pending actions in a different thread.
     */
    private void processSelectorActions() {
        while (!this.selectorActions.isEmpty()) {
            final Runnable action = selectorActions.poll();
            action.run();
        }
    }

    /**
     * Evaluates a {@code SelectionKey} and try to handle it.
     *
     * @param key The {@code SelectionKey}
     * @throws IOException
     */
    private void evaluateSelectionKey(SelectionKey key) throws IOException {
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

    public static void main(final String[] args) throws InterruptedException {
        LOGGER.info("Starting...");

        final Thread nonBlockingServer = new EchoNonBlockingServer(8080, 10);
        nonBlockingServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Stopping EchoNonBlockingServer...");
            nonBlockingServer.interrupt();
        }));

        nonBlockingServer.join();

        Thread.sleep(500);
    }


}
