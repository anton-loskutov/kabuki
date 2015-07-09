package org.kabuki.actor;

import org.kabuki.queues.Queue;
import org.kabuki.utils.concurrent.AgentThread;

import java.util.function.Consumer;

abstract class ActorSystemMPSC implements ActorSystem {

    public static final int DEFAULT_QUEUE_SIZE = 1024;

    protected final AgentThread thread;
    protected final Consumer<Throwable> errorConsumer;
    protected boolean started;
    protected volatile Queue queue;

    @SuppressWarnings("Convert2MethodRef")
    ActorSystemMPSC(String threadName, Consumer<Throwable> errorConsumer) {
        this.thread = new AgentThread(threadName, (Runnable) (() -> queue.consume()), error -> {
            queue = null;
            errorConsumer.accept(error);
        });
        this.errorConsumer = errorConsumer;
    }

    public synchronized void start(boolean daemon) {
        if (started) {
            throw new IllegalStateException("Actor system already started!");
        }

        initBeforeStart();

        if (queue == null) {
            throw new IllegalStateException("Actor system can not be started after error raised!");
        }
        thread.start(daemon);
        started = true;
    }

    @Override
    public void start() {
        start(false);
    }

    public void startAsDaemon() {
        start(true);
    }

    @Override
    public synchronized void stop() {
        if (!started) {
            throw new IllegalStateException("Actor system is not started!");
        }
        Queue queue = this.queue;
        if (queue != null) {
            queue.consumeShutdown();
        }
        thread.stop();
        started = false;
    }

    @Override
    public <I> I asynchronize(Class<I> i, I object) {
        return asynchronize(i, null, object);
    }

    protected void initBeforeStart() {

    }
}
