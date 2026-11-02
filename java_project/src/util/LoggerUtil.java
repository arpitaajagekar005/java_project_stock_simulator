package util;

import java.io.IOException;
import java.util.logging.*;

/**
 * Central logger for the entire application.
 *
 * Every class gets a logger via LoggerUtil.getLogger().
 * All logs go to TWO places simultaneously:
 *   1. Console  (so you see them while running)
 *   2. logs/app.log (permanent file — survives app restart)
 *
 * Log levels used in this project:
 *   INFO  → normal events  (trade executed, DB connected)
 *   SEVERE → errors        (DB failed, bad input)
 */
public class LoggerUtil {

    private static final Logger logger = Logger.getLogger("StockSimulator");
    private static boolean initialized = false;

    /**
     * Returns the shared logger, setting it up on first call.
     * All subsequent calls return the same logger instance.
     */
    public static Logger getLogger() {
        if (!initialized) {
            setup();
            initialized = true;
        }
        return logger;
    }

    private static void setup() {
        try {
            // Remove default console handler — we'll add our own
            Logger rootLogger = Logger.getLogger("");
            for (Handler h : rootLogger.getHandlers()) {
                rootLogger.removeHandler(h);
            }

            // --- File Handler — writes to logs/app.log ---
            // append = true means logs ACCUMULATE across runs, not overwrite
            FileHandler fileHandler = new FileHandler("logs/app.log", true);
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(new SimpleFormatter());

            // --- Console Handler — prints to terminal ---
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.SEVERE); // only errors on console
                                                   // INFO goes to file only

            logger.addHandler(fileHandler);
            logger.addHandler(consoleHandler);
            logger.setLevel(Level.ALL);

            // Stop logs bubbling up to root logger (avoids duplicates)
            logger.setUseParentHandlers(false);

            logger.info("=== Logger initialized ===");

        } catch (IOException e) {
            System.out.println("[ERROR] Could not create log file: " + e.getMessage());
        }
    }
}