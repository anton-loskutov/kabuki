package com.kabuki.actor;

import junit.framework.TestCase;
import org.kabuki.actor.ActorSystem;
import org.kabuki.actor.ActorSystemMPSC_Dynamic;
import org.kabuki.actor.ActorSystemMPSC_Static;
import org.kabuki.utils.concurrent.WaitType;
import static org.kabuki.utils.concurrent.WaitType.*;

public class ActorSystemTest extends TestCase {

    public interface I {
        void onMessage(String msg);
        void commit();
    }

    public static class R implements I {
        public static final String msg = "msg";

        public int msgs;
        public int commits;

        @Override
        public void onMessage(String msg) {
            msgs++;
            if (!R.msg.equals(msg)) {
                fail("Unexpected message received!");
            }
        }

        @Override
        public void commit() {
            commits++;
        }
    }

    public static void assertException(Runnable run) {
        try {
            run.run();
            fail();
        } catch (Exception e) {
            // ok
        }
    }

    // ===== TESTS ======

    public static void testDynamic() {
        testDynamic(SPIN);
        testDynamic(LOCK);
    }

    public static void testStatic() {
        testStatic(SPIN, 1);
        testStatic(SPIN, 2);
        testStatic(SPIN, 3);
        testStatic(LOCK, 1);
        testStatic(LOCK, 2);
        testStatic(LOCK, 3);
    }

    public static void testStatic(WaitType type, int startMode) {
        R r = new R();

        ActorSystem as = new ActorSystemMPSC_Static(type, I.class);
        if (startMode == 1) {
            as.start();
        }
        I i = as.asynchronize(I.class, "commit", r);
        if (startMode == 2) {
            as.start();
        }

        i.onMessage(R.msg);

        if (startMode == 3) {
            as.start();
        }

        // can not start twice
        assertException(as::start);

        as.stop();

        assertEquals(1, r.msgs);
        assertEquals(1, r.commits);

        // can not stop twice
        assertException(as::stop);

        // test restart
        if (startMode == 1 || startMode == 2) {
            as.start();
        }
        i.onMessage(R.msg);
        if (startMode == 3) {
            as.start();
        }
        as.stop();
        assertEquals(2, r.msgs);
        assertEquals(2, r.commits);
    }

    public static void testDynamic(WaitType type) {
        R r = new R();

        ActorSystem as = new ActorSystemMPSC_Dynamic(type);
        I i = as.asynchronize(I.class, "commit", r);

        // can not execute not started
        assertException(() -> i.onMessage(R.msg));

        as.start();

        // can not start twice
        assertException(as::start);

        i.onMessage(R.msg);

        // can not create actor after first start
        assertException(() -> as.asynchronize(I.class, r));

        as.stop();

        assertEquals(1, r.msgs);
        assertEquals(1, r.commits);

        // test restart
        assertException(() -> as.asynchronize(I.class, r));
        as.start();
        i.onMessage(R.msg);
        as.stop();
        assertEquals(2, r.msgs);
        assertEquals(2, r.commits);
    }
}
