package org.sahamati.snalib.utils;

import com.google.gson.Gson;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class SnaLibLogger {

    private static final String LOGGER_NAME = "org.sahamati.snalib";

    private final Logger logger;
    private final Gson gson;
    private final boolean enabled;

    public SnaLibLogger(boolean enabled, Gson gson) {
        this.enabled = enabled;
        this.gson = gson;
        if (enabled) {
            logger = Logger.getLogger(LOGGER_NAME);
            logger.setUseParentHandlers(false);
            for (Handler h : logger.getHandlers()) logger.removeHandler(h);
            logger.addHandler(new JsonHandler(System.err));
            logger.setLevel(Level.ALL);
        } else {
            logger = null;
        }
    }

    public void info(String msg, Object... kv) { emit(Level.INFO, "info", msg, kv); }
    public void warn(String msg, Object... kv) { emit(Level.WARNING, "warn", msg, kv); }
    public void error(String msg, Object... kv) { emit(Level.SEVERE, "error", msg, kv); }

    private void emit(Level level, String levelName, String msg, Object... kv) {
        if (!enabled) return;
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("level", levelName);
        entry.put("msg", msg);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            entry.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        logger.log(level, gson.toJson(entry));
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
