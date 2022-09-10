package com.github.cm360.pixadv.util;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.function.BiConsumer;

public class Logger {

	private static int configLevel = 0;
	
	public static final int DEBUG = 0;
	public static final int INFO = 1;
	public static final int WARNING = 2;
	public static final int ERROR = 3;
	public static final String[] LEVEL_NAMES = {
		"Debug",
		"Info",
		"Warning",
		"Error"
	};
	
	private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
	
	private static ArrayList<BiConsumer<Integer, String>> listeners = new ArrayList<BiConsumer<Integer, String>>();
	
	public synchronized static void logMessage(int level, String message, Object... objects) {
		if (level >= configLevel) {
			// Format log message contents
			String formattedMessage = message.formatted(objects);
			// Set appropriate destination
			PrintStream dest;
			if (level >= WARNING)
				dest = System.err;
			else
				dest = System.out;
			// Send message
			String fullMessage = "[%s] [%s-%s]: %s".formatted(LocalDateTime.now().format(formatter),
					Thread.currentThread().getName(), LEVEL_NAMES[level], formattedMessage);
			dest.println(fullMessage);
			dest.flush();
			// Pass message to listeners
			for (BiConsumer<Integer, String> listener : listeners)
				listener.accept(level, formattedMessage);
		}
	}
	
	public static void logException(String message, Exception exception, Object... objects) {
		if (message != null && !message.isEmpty())
			logMessage(ERROR, message, objects);
		logMessage(ERROR, "%s: %s", exception.getClass().getCanonicalName(), exception.getMessage());
		printStacktrace(exception);
		printCause(exception);
	}
	
	private static void printStacktrace(Throwable throwable) {
		for (StackTraceElement ste : throwable.getStackTrace())
			logMessage(ERROR, "    at %s", ste);
	}
	
	private static void printCause(Throwable throwable) {
		Throwable cause = throwable.getCause();
		if (cause != null) {
			logMessage(ERROR, "Cause: %s: %s", cause.getClass().getCanonicalName(), cause.getMessage());
			printStacktrace(cause);
			printCause(cause);
		}
	}

}
