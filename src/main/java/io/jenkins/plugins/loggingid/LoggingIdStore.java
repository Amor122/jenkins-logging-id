package io.jenkins.plugins.loggingid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Captures the "Caught unhandled exception with ID ..." log records emitted by Jenkins core and keeps
 * the most recent ones independently of the System Log ring buffer, which defaults to only 256 records.
 */
public final class LoggingIdStore {

    private static final Logger LOGGER = Logger.getLogger(LoggingIdStore.class.getName());
    private static final Pattern ID_PATTERN =
            Pattern.compile("Caught unhandled exception with ID ([0-9a-fA-F-]{36})");

    private final int maxEntries;
    private final Map<String, LoggingIdEntry> entries;
    private final Handler handler;
    private volatile boolean installed;

    private static final class Holder {
        private static final LoggingIdStore INSTANCE = new LoggingIdStore();
    }

    public static LoggingIdStore get() {
        return Holder.INSTANCE;
    }

    private LoggingIdStore() {
        maxEntries = Math.max(1, Integer.getInteger(LoggingIdStore.class.getName() + ".maxEntries", 200));
        entries = Collections.synchronizedMap(new LinkedHashMap<String, LoggingIdEntry>(16, 0.75f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, LoggingIdEntry> eldest) {
                return size() > maxEntries;
            }
        });
        handler = new CaptureHandler();
        install();
    }

    public synchronized void install() {
        if (installed) {
            return;
        }
        try {
            handler.setLevel(Level.ALL);
            Logger.getLogger("").addHandler(handler);
            installed = true;
            LOGGER.fine("Logging ID capture handler installed");
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.WARNING, "Failed to install Logging ID capture handler", e);
        }
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public int size() {
        return entries.size();
    }

    public LoggingIdEntry get(String id) {
        if (id == null) {
            return null;
        }
        return entries.get(id.trim());
    }

    /**
     * Resolves an exact Logging ID first, then a unique prefix, so partial IDs copied from a screenshot still work.
     */
    public LoggingIdEntry resolve(String query) {
        if (query == null) {
            return null;
        }
        String value = query.trim();
        if (value.isEmpty()) {
            return null;
        }
        LoggingIdEntry exact = entries.get(value);
        if (exact != null) {
            return exact;
        }
        String lower = value.toLowerCase(Locale.ENGLISH);
        LoggingIdEntry match = null;
        for (LoggingIdEntry entry : snapshot()) {
            if (entry.getId().toLowerCase(Locale.ENGLISH).startsWith(lower)) {
                if (match != null) {
                    return null;
                }
                match = entry;
            }
        }
        return match;
    }

    public List<LoggingIdEntry> search(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ENGLISH);
        List<LoggingIdEntry> result = new ArrayList<>();
        for (LoggingIdEntry entry : snapshot()) {
            if (needle.isEmpty() || matches(entry, needle)) {
                result.add(entry);
            }
        }
        return result;
    }

    private static boolean matches(LoggingIdEntry entry, String needle) {
        return contains(entry.getId(), needle)
                || contains(entry.getMessage(), needle)
                || contains(entry.getLoggerName(), needle)
                || contains(entry.getStackTrace(), needle)
                || contains(entry.getLevel(), needle);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ENGLISH).contains(needle);
    }

    private List<LoggingIdEntry> snapshot() {
        List<LoggingIdEntry> copy;
        synchronized (entries) {
            copy = new ArrayList<>(entries.values());
        }
        Collections.reverse(copy);
        return copy;
    }

    private void capture(LogRecord record) {
        String message = record.getMessage();
        if (message == null) {
            return;
        }
        Matcher matcher = ID_PATTERN.matcher(message);
        if (!matcher.find()) {
            return;
        }
        LoggingIdEntry entry = LoggingIdEntry.from(record, matcher.group(1));
        entries.put(entry.getId(), entry);
    }

    private final class CaptureHandler extends Handler {
        @Override
        public void publish(LogRecord record) {
            try {
                capture(record);
            } catch (RuntimeException ignored) {
                // never let diagnostics break Jenkins logging
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
