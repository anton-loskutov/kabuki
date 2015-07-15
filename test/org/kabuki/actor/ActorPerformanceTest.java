package org.kabuki.actor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.jctools.queues.MpmcArrayQueue;
import org.jctools.queues.MpscArrayQueue;
import org.kabuki.Actor;
import org.kabuki.utils.concurrent.AgentThread;
import org.kabuki.utils.concurrent.GenericRunnable;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static org.kabuki.utils.concurrent.WaitType.LOCK;
import static org.kabuki.utils.concurrent.WaitType.SPIN;

public class ActorPerformanceTest {

    public static class CyclesCounter implements Actor<String> {
        private final int cycle;
        private int counter = 0;
        private volatile int cycles = 0;

        public CyclesCounter(int cycle) {
            this.cycle = cycle;
        }

        public int cycles() {
            return cycles;
        }

        @Override
        public void onMessage(String msg) {
            if (++counter == cycle) {
                counter = 0;
                cycles++;
            }
        }
    }

    public static class CyclesPrinter implements Actor<String> {
        private final int cycle;
        private int counter = 0;
        private long t = System.currentTimeMillis();

        public CyclesPrinter(int cycle) {
            this.cycle = cycle;
        }

        @Override
        public void onMessage(String msg) {
            if (++counter == cycle) {
                System.out.println(1000 * (cycle / (System.currentTimeMillis() - t)));
                t = System.currentTimeMillis();
                counter = 0;
            }
        }
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        final String threadName = "TEST-ConsumerThread";
        final int queueSize = 1024;
        final int writers = 1;
        final Consumer<Throwable> errorConsumer = Throwable::printStackTrace;

        ActorSystem system;

        if (args.length == 0) {
            throw new IllegalArgumentException("No arguments specified!");
        }
        else if (args[0].equalsIgnoreCase("static") && args[1].equalsIgnoreCase("spin")) {
            system = new ActorSystemMPSC_Static(SPIN, queueSize, threadName, errorConsumer, Actor.class);
        }
        else if (args[0].equalsIgnoreCase("static") && args[1].equalsIgnoreCase("lock")) {
            system = new ActorSystemMPSC_Static(LOCK, queueSize, threadName, errorConsumer, Actor.class);
        }
        else if (args[0].equalsIgnoreCase("dynamic") && args[1].equalsIgnoreCase("spin")) {
            system = new ActorSystemMPSC_Dynamic(SPIN, queueSize, threadName, errorConsumer);
        }
        else if (args[0].equalsIgnoreCase("dynamic") && args[1].equalsIgnoreCase("lock")) {
            system = new ActorSystemMPSC_Dynamic(LOCK, queueSize, threadName, errorConsumer);
        }
        else if (args[0].equalsIgnoreCase("abq")) {
            system = new ActorSystem_A_QueueBased(threadName, new ArrayBlockingQueue<>(queueSize), errorConsumer);
        }
        else if (args[0].equalsIgnoreCase("jctools") && args[1].equalsIgnoreCase("mpsc")) {
            system = new ActorSystem_A_QueueBased(threadName, new MpscArrayQueue<>(queueSize), errorConsumer);
        }
        else if (args[0].equalsIgnoreCase("jctools") && args[1].equalsIgnoreCase("mpmc")) {
            system = new ActorSystem_A_QueueBased(threadName, new MpmcArrayQueue<>(queueSize), errorConsumer);
        }
        else if (args[0].equalsIgnoreCase("disruptor") && args[1].equalsIgnoreCase("spin")) {
            system = new ActorSystem_A_DisruptorBased(threadName, queueSize, new BusySpinWaitStrategy());
        }
        else if (args[0].equalsIgnoreCase("disruptor") && args[1].equalsIgnoreCase("lock")) {
            system = new ActorSystem_A_DisruptorBased(threadName, queueSize, new BlockingWaitStrategy());
        }
        else {
            throw new IllegalArgumentException();
        }

        // ======= RUN =======

        final int NUMBER_OF_MSGS = 10_000_000;
        final String msg = "msg";

        // === warmup actor and actor system ===
        System.out.println("warmup started.");
        int warmup_cycle = 10_000;
        int warmup_cycles = 5 * (NUMBER_OF_MSGS / warmup_cycle);
        CyclesCounter consumer = new CyclesCounter(warmup_cycle);
        final Actor<String> actorWarmup = system.asynchronize(Actor.unchecked(String.class), consumer);
        for (int w = 1; w <= warmup_cycles; w++) {
            system.start();
            for (int j = 0; j < warmup_cycle; j++) {
                actorWarmup.onMessage(msg);
            }
            while (consumer.cycles() != w);
            system.stop();
        }
        System.out.println("warmup complete.");


        system.start();
        final Actor<String> actor = system.asynchronize(Actor.unchecked(String.class), new CyclesPrinter(NUMBER_OF_MSGS));

        for (int i = 1; i <= writers; i ++) {
            new Thread(() -> {
                for (;;) {
                    actor.onMessage(msg);
                }
            }, "TEST-ProducerThread-" + i).start();
        }
    }

    private abstract static class ActorSystem_A implements ActorSystem, Actor<String> {
        protected Actor<String> a;

        @Override
        public <I> I asynchronize(Class<I> i, I a) {
            this.a = (Actor<String>) a;
            return (I) this;
        }

        @Override
        public <I> I asynchronize(Class<I> i, String commitMethodName, I object) {
            throw new AssertionError();
        }
    }

    private static class ActorSystem_A_QueueBased extends ActorSystem_A {

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

    private static class ActorSystem_A_DisruptorBased extends ActorSystem_A {
        private Disruptor<Event> disruptor;
        private RingBuffer<Event> ringBuffer;

        private final int queueSize;
        private final WaitStrategy ws;
        private ExecutorService executor;

        public ActorSystem_A_DisruptorBased(String threadName, int queueSize, WaitStrategy ws) {
            this.executor = Executors.newSingleThreadExecutor(r -> new Thread(null, r, threadName));
            this.queueSize = queueSize;
            this.ws = ws;
        }

        public class Event {
            public String msg;
        }

        @Override
        public void start() {
            disruptor = new Disruptor<>(Event::new, queueSize, executor, ProducerType.MULTI, ws);
            disruptor.handleEventsWith((event, sequence, endOfBatch) -> a.onMessage(event.msg));
            ringBuffer = disruptor.getRingBuffer();
            disruptor.start();
        }

        @Override
        public void stop() {
            disruptor.shutdown();
        }

        @Override
        public void onMessage(String msg) {
            long seq = ringBuffer.next();
            ringBuffer.get(seq).msg = msg;
            ringBuffer.publish(seq);
        }
    }
}
