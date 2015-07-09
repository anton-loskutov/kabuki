package com.kabuki.queues;

import com.kabuki.Actor;
import junit.framework.TestCase;
import org.kabuki.queues.Queue;
import org.kabuki.queues.Queues;
import org.kabuki.queues.mpsc.MPSC_SlotType;
import org.kabuki.utils.concurrent.WaitType;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.kabuki.utils.concurrent.WaitType.LOCK;
import static org.kabuki.utils.concurrent.WaitType.SPIN;

public class QueueTest extends TestCase {

    public static void testSimpleMessage() {
        testSimpleMessage(LOCK);
        testSimpleMessage(SPIN);
    }

    public static void testSimpleMessage(WaitType type) {
        Queue mpsc = Queues.mpsc(type, new MPSC_SlotType(1,1), 1024, Throwable::printStackTrace);

        String msg = "msg";
        AtomicBoolean msgReceived = new AtomicBoolean(false);

        Actor<String> s = mpsc.asynchronize(Actor.unchecked(String.class), incomingMsg -> {
            msgReceived.set(true);
            if (!incomingMsg.equals(msg)) {
                fail("Unexpected message received!");
            }
        });
        new Thread() {
            @Override
            public void run() {
                s.onMessage(msg);
                mpsc.consumeShutdown();
            }
        }.start();

        mpsc.consume();

        assertTrue(msgReceived.get());
    }
}
