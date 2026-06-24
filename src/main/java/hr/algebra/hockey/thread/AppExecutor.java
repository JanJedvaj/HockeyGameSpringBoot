package hr.algebra.hockey.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class AppExecutor {
    private static final Logger LOGGER = Logger.getLogger(AppExecutor.class.getName());
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "hockey-worker");
        thread.setDaemon(true);
        return thread;
    });

    private AppExecutor() {
    }

    public static ExecutorService getExecutor() {
        return EXECUTOR;
    }

    public static void execute(Runnable task) {
        EXECUTOR.execute(task);
    }

    public static void shutdown() {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(2, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException exception) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("Application executor shut down.");
    }
}
