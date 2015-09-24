package org.kabuki.utils;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;

public class SystemUtils {

    private static final CountDownLatch shutdownAwait = new CountDownLatch(1);
    private static final ConcurrentLinkedDeque<Thread> shutdownAwaitingThreads = new ConcurrentLinkedDeque<>();

    private SystemUtils() {
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    shutdownAwait.countDown();
                    for (Thread awaitingThread : shutdownAwaitingThreads) {
                        awaitingThread.join();
                    }
                } catch (InterruptedException ignore) {
                }
            }
        });
    }

    public static void awaitShutdown() throws InterruptedException {
        try {
            shutdownAwaitingThreads.add(Thread.currentThread());
            shutdownAwait.await();
        } catch (InterruptedException e) {
            shutdownAwaitingThreads.remove(Thread.currentThread());
            throw new InterruptedException();
        }
    }
}
