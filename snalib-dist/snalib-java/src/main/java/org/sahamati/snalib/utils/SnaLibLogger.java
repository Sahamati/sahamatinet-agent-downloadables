package org.sahamati.snalib.utils;

import com.google.gson.Gson;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class SnaLibLogger {

    private static final String LOGGER_NAME = "org.sahamati.snalib";

    private final Logger logger;
    private final Gson gson;
    private final boolean enabled;
    private final Consumer<String> onWarning;

    public SnaLibLogger(boolean enabled, Gson gson, Consumer<String> onWarning) {
        this.enabled = enabled;
        this.gson = gson;
        this.onWarning = onWarning;

        // Always initialise the JUL logger — needed for warnings even when logEnabled is false.
        // Skipped only when a custom onWarning handler is set and logEnabled is false (no JUL output at all).
        if (enabled || onWarning == null) {
            logger = Logger.getLogger(LOGGER_NAME);
            logger.setUseParentHandlers(false);
            for (Handler h : logger.getHandlers()) logger.removeHandler(h);
            logger.addHandler(new JsonHandler(System.err));
            logger.setLevel(Level.ALL);
        } else {
            logger = null;
        }
    }

    /** Info logs — only emitted when logEnabled is true. */
    public void info(String msg, Object... kv) {
        if (!enabled) return;
        emit(Level.INFO, "info", msg, kv);
    }

    /** Warnings — always emitted regardless of logEnabled. Routed to onWarning handler if set. */
    public void warn(String msg, Object... kv) {
        String json = buildJson("warn", msg, kv);
        if (onWarning != null) {
            onWarning.accept(json);
        } else {
            logger.log(Level.WARNING, json);
        }
    }

    private void emit(Level level, String levelName, String msg, Object... kv) {
        logger.log(level, buildJson(levelName, msg, kv));
    }

    private String buildJson(String levelName, String msg, Object... kv) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("level", levelName);
        entry.put("msg", msg);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            entry.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return gson.toJson(entry);
    }

    private static final class JsonHandler extends Handler {
        private final PrintStream out;

        JsonHandler(PrintStream out) { this.out = out; }

        @Override
        public void publish(LogRecord record) {
            if (record != null && record.getMessage() != null) out.println(record.getMessage());
        }

        @Override public void flush() { out.flush(); }
        @Override public void close() {}
    }
}
