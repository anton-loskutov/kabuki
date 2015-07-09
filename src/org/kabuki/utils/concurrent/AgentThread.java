package org.kabuki.utils.concurrent;

import java.util.function.Consumer;

public class AgentThread implements Agent {

    private final String threadName;
    private final GenericRunnable runnable;
    private final Consumer<Throwable> errorConsumer;
    private Thread thread;

    public AgentThread(String threadName, GenericRunnable repeatable, Consumer<Throwable> errorConsumer) {
        this.threadName = threadName;
        this.runnable = repeatable;
        this.errorConsumer = errorConsumer;
    }

    public AgentThread(String threadName, Runnable repeatable, Consumer<Throwable> errorConsumer) {
        this(threadName, (GenericRunnable) repeatable::run, errorConsumer);
    }

    public synchronized void start(boolean daemon) {
        if (thread != null) {
            throw new IllegalStateException();
        }

        thread = new Thread(()->{
            try {
                runnable.run();
            } catch (Throwable e) {
                errorConsumer.accept(e);
            }
        });
        thread.setName(threadName);
        thread.setDaemon(daemon);
        thread.start();
    }

    @Override
    public void start() {
        start(true);
    }

    @Override
    public synchronized void stop() {
        if (thread == null) {
            throw new IllegalStateException();
        }
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        thread = null;
    }
}
