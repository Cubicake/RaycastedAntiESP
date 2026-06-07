/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.paper;

import games.cubi.logs.CheckPreviousLogForError;
import games.cubi.logs.PlatformLogger;
import games.cubi.raycastedantiesp.core.config.ConfigManager;
import games.cubi.raycastedantiesp.core.config.DebugConfig;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PaperLoggerAdapter implements PlatformLogger {

    /**
     * Set this to true to send logs to the file regardless of the log level.
     */
    private static final boolean LOG_TO_FILE = false;

    /*
     * If too many logs queue up before clear() is called, flush them anyway.
     * This avoids unbounded memory growth.
     */
    private static final int MAX_QUEUED_MESSAGES_BEFORE_FLUSH = 128;

    private final java.util.logging.Logger logger;
    private final Path logFilePath;

    private final ConcurrentLinkedDeque<String> queuedFileMessages = new ConcurrentLinkedDeque<>();
    private final AtomicInteger queuedFileMessageCount = new AtomicInteger(0);

    /*
     * Prevents two threads from flushing the queue to the file at the same time.
     */
    private final AtomicBoolean writingToFile = new AtomicBoolean(false);

    protected PaperLoggerAdapter(java.util.logging.Logger logger, Path logFilePath) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.logFilePath = Objects.requireNonNull(logFilePath, "logFilePath");

        if (LOG_TO_FILE) {
            try {
                Path parent = logFilePath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }

                if (!Files.exists(logFilePath)) {
                    Files.createFile(logFilePath);
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Could not initialise log file at " + logFilePath, exception);
            }
        }
    }

    /*
     * Logging severity is from 0-10. If the configured severity is 0, no logs will be sent. If 1, only most important logs.
     *
     * Therefore, logs with level of 1 are most important and 10 least.
     *
     * Note that by default the log levels are at 5, so any logs which should appear normally should be at 1-5. Additionally, loggers which fire several times a tick should be at 10, once a tick at 9, and loggers firing frequently at 6-8
     * */

    private int getLevel(Level severity, DebugConfig debug) {
        return switch (severity) {
            case INFO -> debug.getInfoLevel();
            case WARN -> debug.getWarnLevel();
            case ERROR -> debug.getErrorLevel();
            default -> 1;
        };
    }

    private DebugConfig.Severity getDebugSeverity(Level severity) {
        return switch (severity) {
            case INFO -> DebugConfig.Severity.INFO;
            case WARN -> DebugConfig.Severity.WARN;
            case ERROR -> DebugConfig.Severity.ERROR;
            default -> DebugConfig.Severity.ERROR;
        };
    }
    /**
     * Logs a warning message and serves as an early return. Nothing called after this method will be executed.
     * @param throwable The throwable to log, used for the included stack trace. The message of the throwable will be used as the warning message
     * @throws CheckPreviousLogForError Always throws this to allow for early return from functions after logging an error
     * **/
    @Override
    public void warningAndReturn(Throwable throwable, @Range(from = 1, to = 10) int level, Class<?>... source) {
        warning(PlatformLogger.processThrowable(throwable), level, source);
        throw earlyReturn;
    }

    @Override
    public void warning(Throwable throwable, @Range(from = 1, to = 10) int level, Class<?>... source) {
        warning(PlatformLogger.processThrowable(throwable), level, source);
    }

    @Deprecated @Override
    public void debug(String message) {
        forwardLog(message, Level.INFO, 10);
    }

    @Override
    public void error(Throwable throwable, @Range(from = 1, to = 10) int level, Class<?>... source) {
        error(PlatformLogger.processThrowable(throwable), level, source);
    }

    @Override
    public void error(String message, Throwable throwable, @Range(from = 1, to = 10) int level, Class<?>... source) {
        error(PlatformLogger.processThrowable(throwable, message), level, source);
    }

    @Override
    public void info(String message, @Range(from = 1, to = 10) int level, @NotNull Class<?>... source) {
        forwardLog(message, Level.INFO, level, source);
    }

    @Override
    public void warning(String message, @Range(from = 1, to = 10) int level, Class<?>... source) {
        forwardLog(message, Level.WARN, level, source);
    }

    @Override
    public void error(String message, @Range(from = 1, to = 10) int level, Class<?>... source) {
        forwardLog(message, Level.ERROR, level, source);
    }

    private void forwardLog(String message, Level severity, int level, Class<?>... source) {
        ConfigManager configManager = RaycastedAntiESP.getConfigManager();
        if (LOG_TO_FILE) {
            queueFileLog(PlatformLogger.constructFileLogMessage(message, severity, level, source), severity);
        }
        if (configManager != null && configManager.getDebugConfig() != null) {
            DebugConfig debug = configManager.getDebugConfig();

            if (debug.isExempted(getDebugSeverity(severity), source)) {
                return;
            }
            if (getLevel(severity, debug) < level) {
                return;
            }
        }

        message = source != null ? PlatformLogger.constructMessage(message, source) : message;

        switch (severity) {
            case INFO:
                logger.info(message);
                break;
            case WARN:
                logger.warning(message);
                break;
            case ERROR:
                logger.severe(message);
                break;
            default:
                logger.severe(message + "| Additionally, severity " + severity + " is not supported by the logger.");
                break;
        }
    }

    private void queueFileLog(String message, Level severity) {
        queuedFileMessages.addLast(message);

        int queuedMessages = queuedFileMessageCount.incrementAndGet();
        if (queuedMessages >= MAX_QUEUED_MESSAGES_BEFORE_FLUSH) {
            flushToFile();
        }
    }

    /**
     * Flushes queued log messages to the log file asynchronously.
     */
    public void flushToFile() {
        if (!LOG_TO_FILE) {
            return;
        }
        Bukkit.getAsyncScheduler().runNow(RaycastedAntiESP.get(), (ignored) -> {
            if (!writingToFile.compareAndSet(false, true)) return;

            try {
                forceFlushToFileNow();
            } finally {
                writingToFile.set(false);
            }
        });
    }

    /**
     * Flushes queued log messages to the log file immediately, on the calling thread. Should only be used directly before shutdown to ensure all logs are flushed, or via <code>flushToFile()</code>.
     */
    public void forceFlushToFileNow() {
        List<String> drainedMessages = new ArrayList<>();

        String message;
        while ((message = queuedFileMessages.pollFirst()) != null) {
            queuedFileMessageCount.decrementAndGet();
            drainedMessages.add(message);
        }

        if (drainedMessages.isEmpty()) {
            return;
        }

        try {
            Files.write(
                    logFilePath,
                    drainedMessages,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            /*
             * Put the messages back at the front of the queue so they are not lost.
             * Reverse order is needed because addFirst() is used.
             */
            for (int index = drainedMessages.size() - 1; index >= 0; index--) {
                queuedFileMessages.addFirst(drainedMessages.get(index));
                queuedFileMessageCount.incrementAndGet();
            }

            /*
             * Last-resort console error. Without this, file logging failures are silent.
             */
            logger.severe("Failed to write queued log messages to " + logFilePath + ": " + exception.getMessage());
        }
    }
}