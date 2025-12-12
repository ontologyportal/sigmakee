package com.articulate.sigma;

import java.util.concurrent.*;

public final class EditorWorkerQueue {

    private static final ExecutorService QUEUE =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "EditorWorker");
                t.setDaemon(true);
                return t;
            });

    private EditorWorkerQueue() {}

    public static <T> T submit(Callable<T> task) throws Exception {
        Future<T> f = QUEUE.submit(task);
        return f.get();
    }
}
