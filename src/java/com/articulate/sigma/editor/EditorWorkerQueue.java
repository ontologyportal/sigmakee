package com.articulate.sigma.editor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/** */
public final class EditorWorkerQueue {

    /** */
    private EditorWorkerQueue() {}
    /** */
    public static int workers()     { return WORKERS; }
    /** */
    public static int queueDepth()  { return EXEC.getQueue().size(); }
    /** */
    public static int activeCount() { return EXEC.getActiveCount(); }
    /** */
    private static final int WORKERS = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
    /** */
    private static final int QUEUE_CAPACITY = 300;
    /** */
    private static final AtomicInteger TID = new AtomicInteger(1);

    /********************************************************************
     */
    private static final ThreadPoolExecutor EXEC = new ThreadPoolExecutor(
        WORKERS, WORKERS,
        0L, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(QUEUE_CAPACITY),
        r -> {
            Thread t = new Thread(r, "EditorWorker-" + TID.getAndIncrement());
            t.setDaemon(true);
            return t;
        },
        new ThreadPoolExecutor.AbortPolicy()
    );

    /********************************************************************
     */
    public static <T> T submit(Callable<T> task, long timeoutMs) throws InterruptedException, ExecutionException, TimeoutException {
        Future<T> f = EXEC.submit(task);
        try {
            return f.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            f.cancel(true);
            throw te;
        }
    }
}
