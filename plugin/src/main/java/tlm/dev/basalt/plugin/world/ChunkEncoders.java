package tlm.dev.basalt.plugin.world;

import tlm.dev.basalt.plugin.lifecycle.Lifecycle;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public final class ChunkEncoders implements Executor, Lifecycle {

    private final int threads;
    private final ExecutorService pool;
    private final Logger logger;
    private final AtomicLong encoded = new AtomicLong();

    public ChunkEncoders(int threads, Logger logger) {
        this.threads = threads;
        this.pool = Executors.newFixedThreadPool(threads, daemons());
        this.logger = logger;
    }

    @Override
    public void execute(Runnable encoding) {
        encoded.incrementAndGet();
        pool.execute(encoding);
    }

    @Override
    public void onDisable() {
        pool.shutdownNow();
        logger.info("Encoded " + encoded.get() + " virtual chunks on " + threads + " thread" + (threads == 1 ? "" : "s"));
    }

    private static ThreadFactory daemons() {
        AtomicInteger next = new AtomicInteger();
        return encoding -> {
            Thread thread = new Thread(encoding, "Basalt chunk encoder #" + next.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
