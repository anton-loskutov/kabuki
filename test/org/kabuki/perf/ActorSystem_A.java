package org.kabuki.perf;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import org.jctools.queues.MpmcArrayQueue;
import org.jctools.queues.MpscArrayQueue;
import org.kabuki.Actor;
import org.kabuki.actor.ActorSystem;
import org.kabuki.actor.ActorSystemMPSC_Dynamic;
import org.kabuki.actor.ActorSystemMPSC_Static;
import org.kabuki.utils.concurrent.WaitType;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Consumer;

import static org.kabuki.utils.concurrent.WaitType.LOCK;
import static org.kabuki.utils.concurrent.WaitType.SPIN;

abstract class ActorSystem_A implements ActorSystem, Actor<String> {
    protected Actor<String> a;

    @SuppressWarnings("unchecked")
    @Override
    public <I> I asynchronize(Class<I> i, I a) {
        if (!i.equals(Actor.class)) {
            throw new IllegalArgumentException();
        }
        this.a = (Actor<String>) a;
        return (I) this;
    }

    @Override
    public <I> I asynchronize(Class<I> i, String commitMethodName, I object) {
        throw new AssertionError();
    }
    
    public static ActorSystem create(String type, String args, String threadName, int queueSize, Consumer<Throwable> errorConsumer) {
        if (type.equalsIgnoreCase("static") && args.equals("spin")) {
            return new ActorSystemMPSC_Static(SPIN, queueSize, threadName, errorConsumer, Actor.class);
        }
        else if (type.equalsIgnoreCase("static") && args.equalsIgnoreCase("lock")) {
            return new ActorSystemMPSC_Static(LOCK, queueSize, threadName, errorConsumer, Actor.class);
        }
        else if (type.equalsIgnoreCase("dynamic") && args.equalsIgnoreCase("spin")) {
            return new ActorSystemMPSC_Dynamic(SPIN, queueSize, threadName, errorConsumer);
        }
        else if (type.equalsIgnoreCase("dynamic") && args.equalsIgnoreCase("lock")) {
            return new ActorSystemMPSC_Dynamic(LOCK, queueSize, threadName, errorConsumer);
        }
        else if (type.equalsIgnoreCase("abq")) {
            return new ActorSystem_A_QueueBased(threadName, new ArrayBlockingQueue<>(queueSize), errorConsumer);
        }
        else if (type.equalsIgnoreCase("jctools") && args.equalsIgnoreCase("mpsc")) {
            return new ActorSystem_A_QueueBased(threadName, new MpscArrayQueue<>(queueSize), errorConsumer);
        }
        else if (type.equalsIgnoreCase("jctools") && args.equalsIgnoreCase("mpmc")) {
            return new ActorSystem_A_QueueBased(threadName, new MpmcArrayQueue<>(queueSize), errorConsumer);
        }
        else if (type.equalsIgnoreCase("disruptor") && args.equalsIgnoreCase("spin")) {
            return new ActorSystem_A_DisruptorBased(threadName, queueSize, new BusySpinWaitStrategy());
        }
        else if (type.equalsIgnoreCase("disruptor") && args.equalsIgnoreCase("lock")) {
            return new ActorSystem_A_DisruptorBased(threadName, queueSize, new BlockingWaitStrategy());
        }
        else {
            throw new IllegalArgumentException(type);
        }
    } 
}
