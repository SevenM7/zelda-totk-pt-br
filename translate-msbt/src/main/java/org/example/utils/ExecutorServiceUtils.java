package org.example.utils;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ExecutorServiceUtils {
    public static Executor EXECUTOR;
    public static ScheduledExecutorService SCHEDULER;

    static {
        EXECUTOR = Executors.newCachedThreadPool();
        SCHEDULER = Executors.newScheduledThreadPool(1);
    }
}
