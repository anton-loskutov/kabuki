package org.kabuki.perf;

import org.kabuki.Actor;
import org.kabuki.actor.ActorSystem;

import java.util.function.Consumer;

public class TestLatency {

    public static class Ping implements Actor<String> {
        private final int cycle;
        private int counter = 0;
        private long t = System.currentTimeMillis();
        private Actor<String> pong;

        public Ping(int cycle) {
            this.cycle = cycle;
        }

        public void setPong(Actor<String> pong) {
            this.pong = pong;
        }

        @Override
        public void onMessage(String msg) {
            if (++counter == cycle) {
                counter = 0;
                System.out.println(((System.currentTimeMillis() - t) * 1_000_000L) / (2L * cycle));
                t = System.currentTimeMillis();
                counter = 0;
            }
            pong.onMessage(msg);
        }
    }

    public static class Pong implements Actor<String> {
        public Pong(Actor<String> ping) {
            this.ping = ping;
        }

        private final Actor<String> ping;

        @Override
        public void onMessage(String message) {
            ping.onMessage(message);
        }
    }

    public static void main(String[] args) {
        final int queueSize = 1024;
        final Consumer<Throwable> errorConsumer = Throwable::printStackTrace;
        final int NUMBER_OF_MSGS = 10_000_000;
        final String msg = "msg";

        ActorSystem pingThread = ActorSystem_A.create(args[0], args[1], "TEST-PingThread", queueSize, errorConsumer);
        ActorSystem pongThread = ActorSystem_A.create(args[0], args[1], "TEST-PongThread", queueSize, errorConsumer);


        Ping ping = new Ping(NUMBER_OF_MSGS);

        Actor<String> iping = pingThread.asynchronize(Actor.unchecked(String.class), ping);
        Actor<String> ipong = pongThread.asynchronize(Actor.unchecked(String.class), new Pong(iping));
        ping.setPong(ipong);

        pingThread.start();
        pongThread.start();

        iping.onMessage(msg);

    }
}
