package com.kabuki.actor;

import org.kabuki.actor.ActorSystem;
import org.kabuki.actor.ActorSystemMPSC_Dynamic;

public class ActorSystemSample {

    public interface I {
        void onMessage(String message);
    }

    public static class Printer implements I {
        @Override
        public void onMessage(String message) {
            System.out.println("Message received: " + message);
        }
    }

    public static void main(String[] args) {
        ActorSystem thread = new ActorSystemMPSC_Dynamic();

        I actor = thread.asynchronize(I.class, new Printer());

        thread.start();

        actor.onMessage("Hello!");

        thread.stop();
    }
}
