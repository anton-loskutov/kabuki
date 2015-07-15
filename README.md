# Kabuki

Kabuki is a library for java that allows to use [Actor model](https://en.wikipedia.org/wiki/Actor_model) implementation with a high-performance core but simple and typed API, which gives ability to easily build efficient [Staged event-driven architecture](https://en.wikipedia.org/wiki/Staged_event-driven_architecture).

Kabuki's core focus is a high-throughput. The crucial part of Kabuki is Multiple-Producers Single-Consumer variation of famous [Dmitry Vyukov's Bounded MPMC Queue](http://www.1024cores.net/home/lock-free-algorithms/queues/bounded-mpmc-queue) (variation was inspired by [David Dice's PTLQueue](https://blogs.oracle.com/dave/entry/ptlqueue_a_scalable_bounded_capacity#comments)). In stationary condition Kabuki becomes garbage-free.

Kabuki's API focus is simplicity and type safety. To achieve it without impacting performance kabuki uses code generation with [Metaja](https://github.com/anton-loskutov/metaja) under the hood.

## Kabuki usage

Usage of kabuki is dramatically simple:
```java
import org.kabuki.actor.ActorSystem;
import org.kabuki.actor.ActorSystemMPSC_Dynamic;

public class ActorSystemSample {

    public interface I {
        void onMessage(String message);
    }

    public static class Printer implements I {
        @Override public void onMessage(String message) {
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
```

## Kabuki actor system

Technically, actor system is just a thread which can process asynchronous messages with provided objects (actors) and is able to dynamically create interfaces for sending such messages. 

**org.kabuki.actor.ActorSystemMPSC_Dynamic** allows creation of actors only before start.

**org.kabuki.actor.ActorSystemMPSC_Static** allows creation of actors at any time, but their types must be defined at construction time.

**org.kabuki.actor.ActorSystemProxy** allows creation of actors at any time and of any type, but is much slower and is not garbage-free.

All actor systems are based on bounded queues, so there is a back pressure and producers can become blocked by consumers when sending a message if consumers are slow; queue size can be specified with a constructor.

MPSC actor systems support two wait strategies - _SPIN_ (for low-latency) and _LOCK_ (for accurate resource management); strategy can be specified with a constructor. 

## Kabuki dependencies 

Kabuki was written in java 8 and requires jdk 1.8 and [Metaja](https://github.com/anton-loskutov/metaja) (metaja uses [java compiler api](http://docs.oracle.com/javase/8/docs/api/javax/tools/JavaCompiler.html) from **tools.jar** therefore you can not use it with jre)

## Kabuki benchmarks

Environment: MacBook Pro (2.4 GHz Intel Core i7) and oracle jdk1.8.0_45

Source code: [ActorPerformanceTest.java](https://github.com/anton-loskutov/kabuki/blob/master/test/com/kabuki/actor/ActorPerformanceTest.java)

Results:
 | Engine | Throughput (millions/sec)| 
 | ------------- | ------------- | 
 | ArrayBlockingQueue (spin) | ~4 | 
 | Disruptor (lock) | ~10 | 
 | Disruptor (spin) | ~17 | 
 | JCTools (mpsc spin) | ~22 | 
 | Kabuki (lock) | ~24 | 
 | JCTools (mpmc spin) | ~25 | 
 | Kabuki (spin) | ~32 |
