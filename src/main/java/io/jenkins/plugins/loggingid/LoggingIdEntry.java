package io.jenkins.plugins.loggingid;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.LogRecord;

/**
 * One captured unhandled exception, keyed by the Logging ID that Jenkins showed to the user.
 */
public final class LoggingIdEntry {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());
    private static final int SUMMARY_LIMIT = 200;

    private final String id;
    private final long millis;
    private final String level;
    private final String loggerName;
    private final String message;
    private final String stackTrace;

    private LoggingIdEntry(String id, long millis, String level, String loggerName, String message, String stackTrace) {
        this.id = id;
        this.millis = millis;
        this.level = level;
        this.loggerName = loggerName;
        this.message = message;
        this.stackTrace = stackTrace;
    }

    static LoggingIdEntry from(LogRecord record, String id) {
        return new LoggingIdEntry(
                id,
                record.getMillis(),
                record.getLevel() == null ? "" : record.getLevel().getName(),
                record.getLoggerName() == null ? "" : record.getLoggerName(),
                record.getMessage(),
                formatThrowable(record.getThrown()));
    }

    private static String formatThrowable(Throwable t) {
        if (t == null) {
            return null;
        }
        StringWriter out = new StringWriter();
        t.printStackTrace(new PrintWriter(out));
        return out.toString();
    }

    public String getId() {
        return id;
    }

    public long getMillis() {
        return millis;
    }

    public String getTime() {
        return TIME_FORMAT.format(Instant.ofEpochMilli(millis));
    }

    public String getLevel() {
        return level;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public String getMessage() {
        return message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public String getSummary() {
        if (message == null) {
            return "";
        }
        int newline = message.indexOf('\n');
        String firstLine = newline >= 0 ? message.substring(0, newline) : message;
        return firstLine.length() > SUMMARY_LIMIT ? firstLine.substring(0, SUMMARY_LIMIT) + "..." : firstLine;
    }

    /**
     * First line of the stack trace, i.e. the exception type and its message.
     */
    public String getExceptionType() {
        if (stackTrace == null || stackTrace.isEmpty()) {
            return "";
        }
        int newline = stackTrace.indexOf('\n');
        String firstLine = newline >= 0 ? stackTrace.substring(0, newline) : stackTrace;
        return firstLine.trim();
    }

    /**
     * Innermost {@code Caused by:} line, which is usually what actually broke.
     */
    public String getRootCause() {
        if (stackTrace == null || stackTrace.isEmpty()) {
            return "";
        }
        String cause = "";
        for (String line : stackTrace.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("Caused by:")) {
                cause = trimmed.substring("Caused by:".length()).trim();
            }
        }
        return cause;
    }

    public String getText() {
        StringBuilder text = new StringBuilder();
        text.append(getTime()).append(' ').append(level).append(' ').append(loggerName);
        text.append('\n').append(message == null ? "" : message);
        if (stackTrace != null && !stackTrace.isEmpty()) {
            text.append('\n').append(stackTrace);
        }
        return text.toString();
    }
}
