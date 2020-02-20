package ml.dent.log;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Universal logger to display information to the screen
 * 
 * @author Ronak Malik
 */
public class Logger {

	private static ConcurrentLinkedQueue<String> messages;

	static {
		messages = new ConcurrentLinkedQueue<>();
	}

	public Logger() {
	}

	public void log(String message) {
		messages.offer(message);
	}

	public String pollMessage() {
		return messages.poll();
	}
}
