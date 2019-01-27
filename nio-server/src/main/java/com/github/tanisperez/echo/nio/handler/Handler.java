package com.github.tanisperez.echo.nio.handler;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * Handler interface for the multiple SelectionKey states.
 */
public interface Handler {

    /**
     * Handles a {@code SelectionKey} state.
     * @param selectionKey The {@code SelectionKey}
     * @throws IOException
     */
    void handle(final SelectionKey selectionKey) throws IOException;

}
