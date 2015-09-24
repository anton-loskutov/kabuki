package org.kabuki.perf;

import org.kabuki.utils.concurrent.AgentThread;
import org.kabuki.utils.concurrent.GenericRunnable;

import java.util.Queue;
import java.util.function.Consumer;

class ActorSystem_A_QueueBased extends ActorSystem_A {

    private final Queue<String> queue;
    private final AgentThread thread;

    public ActorSystem_A_QueueBased(String threadName, Queue<String> queue, Consumer<Throwable> errorConsumer) {
        this.queue = queue;
        this.thread = new AgentThread(threadName, (GenericRunnable) this::run, errorConsumer);
    }

    public void run() {
        for (;;) {
            String msg = queue.poll();
            if (msg == null) {
                if (Thread.interrupted()) {
                    return;
                }
            } else {
                a.onMessage(msg);
            }
        }
    }

    @Override
    public void onMessage(String msg) {
        for (;;) {
            if (queue.offer(msg)) {
                return;
            }
        }
    }

    @Override
    public void start() {
        thread.start();
    }

    @Override
    public void stop() {
        thread.stop();
    }
}
