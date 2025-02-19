package com.robsutar.rnu.fabric;

import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class SimpleScheduler {
    private final MinecraftServer server;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final HashMap<Integer, Future<?>> pendingTasks = new HashMap<>();
    private int openPendingTaskId = 1;
    private boolean closed = false;

    public SimpleScheduler(MinecraftServer server) {
        this.server = server;
    }

    public void runSync(Runnable runnable) {
        if (closed)
            throw new IllegalStateException("Attempt to schedule an sync task with the closed scheduler");
        server.execute(() -> {
            if (closed) return;
            runnable.run();
        });
    }

    public void runAsync(Runnable runnable) {
        if (closed)
            throw new IllegalStateException("Attempt to schedule an async task with the closed scheduler");

        int id = openPendingTaskId++;
        CompletableFuture<Void> taskHandle = CompletableFuture.runAsync(() -> {
            if (closed) return;
            runnable.run();
        });
        pendingTasks.put(id, taskHandle);
    }

    public void repeat(Runnable runnable, int delay, int period) {
        if (closed)
            throw new IllegalStateException("Attempt to schedule an repeating task with the closed scheduler");

        int id = openPendingTaskId++;
        ScheduledFuture<?> taskHandle = scheduler.scheduleAtFixedRate(
                () -> server.executeBlocking(runnable),
                delay * 50L, period * 50L, TimeUnit.MILLISECONDS
        );
        pendingTasks.put(id, taskHandle);
    }

    public void runLater(Runnable runnable, int delay) {
        if (closed)
            throw new IllegalStateException("Attempt to schedule an later task with the closed scheduler");

        int id = openPendingTaskId++;
        ScheduledFuture<?> taskHandle = scheduler.schedule(
                () -> server.executeBlocking(runnable),
                delay * 50L, TimeUnit.MILLISECONDS
        );
        pendingTasks.put(id, taskHandle);
    }

    public void closeAndCancelPending() {
        if (closed) throw new IllegalStateException("Scheduler already closed");
        closed = true;

        for (Map.Entry<Integer, Future<?>> entry : new HashMap<>(pendingTasks).entrySet()) {
            entry.getValue().cancel(false);
            pendingTasks.remove(entry.getKey());
        }
    }
}
