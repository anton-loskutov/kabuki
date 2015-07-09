package com.kabuki.actor;

import com.kabuki.Actor;
import junit.framework.TestCase;
import org.kabuki.actor.ActorSystemMPSC_Static;

public class ActorSystemDaemonTest extends TestCase {

    public void test() {
        Actor<String> async = ActorSystemMPSC_Static.startAsDaemon(Actor.unchecked(String.class), System.out::println);
        async.onMessage("Hello!");
    }
}
