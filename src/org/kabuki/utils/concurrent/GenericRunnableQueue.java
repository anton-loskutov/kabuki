package org.kabuki.utils.concurrent;

import java.util.concurrent.*;

public class GenericRunnableQueue implements GenericRunnable {

    private final BlockingQueue<GenericRunnable> queue;

    public GenericRunnableQueue(int queueSize) {
        this.queue = queueSize > 0 ? new ArrayBlockingQueue<>(queueSize) : new LinkedTransferQueue<>();
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
                    long timeout = onSleep();
                    runnable = queue.poll(timeout, TimeUnit.MILLISECONDS);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected long onSleep() {
        return Long.MAX_VALUE;
    }
}
