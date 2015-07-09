package org.kabuki.utils.concurrent;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GenericRunnableQueue implements GenericRunnable {

    private final BlockingQueue<GenericRunnable> queue;

    public GenericRunnableQueue(int queueSize) {
        this.queue = queueSize > 0 ? new ArrayBlockingQueue<>(queueSize) : new LinkedBlockingQueue<>();
    }

    public void put(GenericRunnable runnable) {
        try {
            queue.put(runnable);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void add(GenericRunnable runnable) throws IllegalStateException {
        queue.add(runnable);
    }

    public boolean offer(GenericRunnable runnable) {
        return queue.offer(runnable);
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() throws Throwable {
        try {
            GenericRunnable runnable = queue.take();

            for (;;) {
                runnable.run();

                runnable = queue.poll();
                if (runnable == null) {
                    onSleep();
                    runnable = queue.take();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void onSleep() {

    }
}
