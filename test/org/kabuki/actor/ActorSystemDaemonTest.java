package org.kabuki.actor;

import org.kabuki.Actor;
import junit.framework.TestCase;

public class ActorSystemDaemonTest extends TestCase {

    public void test() {
        Actor<String> async = ActorSystemMPSC_Static.startAsDaemon(Actor.unchecked(String.class), System.out::println);
        async.onMessage("Hello!");
    }
}
