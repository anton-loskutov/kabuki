package com.kabuki.actor;

import com.kabuki.Actor;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.jctools.queues.MpscArrayQueue;
import org.kabuki.actor.ActorSystem;
import org.kabuki.actor.ActorSystemMPSC_Dynamic;
import org.kabuki.actor.ActorSystemMPSC_Static;
import org.kabuki.utils.concurrent.AgentThread;
import org.kabuki.utils.concurrent.GenericRunnable;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static org.kabuki.utils.concurrent.WaitType.LOCK;
import static org.kabuki.utils.concurrent.WaitType.SPIN;

public class ActorPerformanceTest {

    private static final long NUMBER_OF_MSGS = 10_000_000;
    private static final String msg = "msg";

    public static class SpeedWriter implements Actor<String> {
        private int counter = 0;
        private long t = System.currentTimeMillis();

        @Override
        public void onMessage(String msg) {

            if (++counter == NUMBER_OF_MSGS) {
                System.out.println(1000 * (NUMBER_OF_MSGS / (System.currentTimeMillis() - t)));
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

        args = new String[] { "dynamic" , "spin"};

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
            system = new ActorSystem_A_QueueBased(threadName, new ActorSystem_A_QueueBased.Queue<String>() {
                private ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(queueSize);

                @Override
                public void put(String o) {
                    try {
                        queue.put(o);
                    } catch (InterruptedException e) {
                        throw new AssertionError();
                    }
                }

                @Override
                public String take() {
                    try {
                        return queue.take();
                    } catch (InterruptedException e) {
                        throw new AssertionError();
                    }
                }
            }, errorConsumer);
        }
        else if (args[0].equalsIgnoreCase("jctools")) {
            system = new ActorSystem_A_QueueBased(threadName, new ActorSystem_A_QueueBased.Queue<String>() {
                private MpscArrayQueue<String> queue = new MpscArrayQueue<>(queueSize);

                @Override
                public void put(String o) {
                    queue.offer(o);
                }

                @Override
                public String take() {
                    return queue.poll();
                }
            }, errorConsumer);
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

        Actor<String> actor = system.asynchronize(Actor.unchecked(String.class), new SpeedWriter());

        system.start();

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
        public interface Queue<T> {
            void put(T o);
            T take();
        }

        private final Queue<String> queue;
        private final AgentThread thread;

        public ActorSystem_A_QueueBased(String threadName, Queue<String> queue, Consumer<Throwable> errorConsumer) {
            this.queue = queue;
            this.thread = new AgentThread(threadName, (GenericRunnable) this::run, errorConsumer);
        }

        public void run() throws Throwable {
            for (;;) {
                a.onMessage(queue.take());
            }
        }

        @Override
        public void onMessage(String msg) {
            queue.put(msg);
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
        private final Disruptor<Event> disruptor;
        private final RingBuffer<Event> ringBuffer;

        public ActorSystem_A_DisruptorBased(String threadName, int queueSize, WaitStrategy ws) {
            disruptor = new Disruptor<>(Event::new, queueSize, Executors.newSingleThreadExecutor(r -> new Thread(null,r,threadName)), ProducerType.MULTI, ws);
            disruptor.handleEventsWith((event, sequence, endOfBatch) -> a.onMessage(event.msg));
            ringBuffer = disruptor.getRingBuffer();
        }

        public class Event {
            public String msg;
        }

        @Override
        public void start() {
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
