package org.kabuki.utils.concurrent;

import org.kabuki.utils.ThreadUtils;

import java.util.function.Consumer;

public class AgentThread implements Agent {

    private final String threadName;
    private final GenericRunnable runnable;
    private final Consumer<Throwable> errorConsumer;

    // STATE
    private long timestamp;
    private Thread thread;
    private Throwable error;
    private Long nid;

    public AgentThread(String threadName, GenericRunnable repeatable, Consumer<Throwable> errorConsumer) {
        this.threadName = threadName;
        this.runnable = repeatable;
        this.errorConsumer = errorConsumer;

        AgentThreadRegistry.register(this);
    }

    public AgentThread(String threadName, Runnable repeatable, Consumer<Throwable> errorConsumer) {
        this(threadName, (GenericRunnable) repeatable::run, errorConsumer);
    }

    public synchronized void startThread(boolean daemon) {
        if (thread != null) {
            throw new IllegalStateException();
        }

        thread = new Thread(() -> {
            try {
                runnable.run();
                markStopped(null);
            } catch (Throwable error) {
                try {
                    errorConsumer.accept(error);
                } catch (Throwable e) {
                    new Error("Exception while error(" + e.getMessage() + ") processing!", e).printStackTrace(System.err);
                }
                markStopped(error);
            }
        });
        thread.setName(threadName);
        thread.setDaemon(daemon);
        thread.start();
        timestamp = System.currentTimeMillis();
    }

    public synchronized boolean stopThread() {
        while (thread != null) {
            thread.interrupt();
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return thread == null;
            }
        }
        return true;
    }

    @Override
    public void start() {
        startThread(true);
    }

    @Override
    public void stop() {
        stopThread();
    }

    public String getName() {
        return threadName;
    }

    public synchronized AgentThreadState getState() {
        if (thread != null) {
            if (nid == null) {
                nid = ThreadUtils.getNativeId(threadName);
            }
            return new AgentThreadState.Started(timestamp, threadName, ThreadUtils.getInfo(thread.getId()), nid);
        } else if (error == null) {
            return new AgentThreadState.Stopped(timestamp, threadName);
        } else {
            return new AgentThreadState.StoppedWithError(timestamp, threadName, error);
        }
    }

    // ----- private -----

    private synchronized void markStopped(Throwable error) {
        this.thread = null;
        this.error = error;
        this.nid = null;
        this.timestamp = System.currentTimeMillis();
        notifyAll();
    }
}
