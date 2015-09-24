package org.kabuki.perf;

import org.kabuki.Actor;
import org.kabuki.actor.ActorSystem;

import java.util.function.Consumer;

public class TestThroughput {

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
        final int NUMBER_OF_MSGS = 10_000_000;
        final String msg = "msg";

        ActorSystem system = ActorSystem_A.create(args[0], args[1], threadName, queueSize, errorConsumer);

        // === warmup actor and actor system ===
        System.out.println("warmup started.");
        int warmup_cycle = NUMBER_OF_MSGS/ 1000;
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

}
