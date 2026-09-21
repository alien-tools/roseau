package io.github.alien.roseau.cli;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.util.ArrayList;
import java.util.List;

final class LogCapture extends AbstractAppender implements AutoCloseable {
	private static final String NAME = "roseau-test-capture";

	private final List<LogEvent> events = new ArrayList<>();
	private final LoggerContext context;

	private LogCapture(LoggerContext context) {
		super(NAME, null, null, true, Property.EMPTY_ARRAY);
		this.context = context;
	}

	static LogCapture install() {
		var context = (LoggerContext) LogManager.getContext(false);
		var capture = new LogCapture(context);
		capture.start();
		var configuration = context.getConfiguration();
		configuration.addAppender(capture);
		configuration.getRootLogger().addAppender(capture, Level.ALL, null);
		context.updateLoggers();
		return capture;
	}

	@Override
	public void append(LogEvent event) {
		synchronized (events) {
			events.add(event.toImmutable());
		}
	}

	List<LogEvent> events() {
		synchronized (events) {
			return List.copyOf(events);
		}
	}

	@Override
	public void close() {
		context.getConfiguration().getRootLogger().removeAppender(NAME);
		context.updateLoggers();
		stop();
	}
}
