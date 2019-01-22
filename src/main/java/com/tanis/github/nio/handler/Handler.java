package com.tanis.github.nio.handler;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface Handler {

	void handle(final SelectionKey selectionKey) throws IOException;

}
