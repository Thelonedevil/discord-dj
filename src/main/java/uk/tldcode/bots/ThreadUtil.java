package uk.tldcode.bots;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ThreadUtil {
    public static final Thread.UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER;

    private ThreadUtil() {}

    static {
        UNCAUGHT_EXCEPTION_HANDLER = (t, e) -> {
        };
        Thread.setDefaultUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER);
    }

    public static ExecutorService newSingleThreadExecutor() {
        return Executors.newSingleThreadExecutor(newThreadFactory());
    }

    public static ExecutorService newSingleThreadExecutor(String nameFormat) {
        return Executors.newSingleThreadExecutor(newThreadFactory(nameFormat));
    }

    public static ExecutorService newCachedThreadPool() {
        return Executors.newCachedThreadPool(newThreadFactory());
    }

    public static ExecutorService newCachedThreadPool(String nameFormat) {
        return Executors.newCachedThreadPool(newThreadFactory(nameFormat));
    }

    public static ThreadFactory newThreadFactory() {
        return new ThreadFactoryBuilder()
                .setUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER)
                .build();
    }

    public static ThreadFactory newThreadFactory(String nameFormat) {
        return new ThreadFactoryBuilder()
                .setNameFormat(nameFormat)
                .setUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER)
                .build();
    }


    public static void interruptIfInterruptedException(Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}