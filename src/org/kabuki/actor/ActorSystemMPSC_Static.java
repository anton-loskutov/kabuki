package org.kabuki.actor;

import org.kabuki.queues.Queues;
import org.kabuki.queues.mpsc.MPSC_SlotType;
import org.kabuki.utils.concurrent.WaitType;
import org.metaja.utils.ClassUtils;
import org.metaja.utils.ReflectionUtils;

import java.util.HashSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.metaja.utils.ReflectionUtils.Modifier.PUBLIC;

public class ActorSystemMPSC_Static extends ActorSystemMPSC {

    private final HashSet<Class> classes;

    public ActorSystemMPSC_Static(WaitType waitType, int queueSize, String threadName, Consumer<Throwable> errorConsumer, Class ... classes) {
        super(threadName, errorConsumer);
        this.classes = new HashSet<>();
        for (Class i : classes) {
            if (!PUBLIC.isModified(i)) {
                throw new IllegalArgumentException(i.getCanonicalName() + " is not public!");
            }
            this.classes.add(i);
        }
        MPSC_SlotType slotType = this.classes.stream().collect(Collectors.reducing(MPSC_SlotType.ZERO, MPSC_SlotType::get, MPSC_SlotType::max));
        this.queue = Queues.mpsc(waitType, slotType, queueSize, errorConsumer);
    }

    @Override
    public synchronized <I> I asynchronize(Class<I> i, String commitMethodName, I object) {
        if (!i.isInterface() || !PUBLIC.isModified(i)) {
            throw new IllegalArgumentException(i.getCanonicalName() + " is not a public interface!");
        }

        if (!classes.contains(i)) {
            throw new IllegalArgumentException("Class '" + i.getCanonicalName() + "' is configured within actor system!");
        }
        return commitMethodName == null
            ? queue.asynchronize(i, object)
            : queue.asynchronize(i, commitMethodName, object)
        ;
    }

    // ----- some sugar ----

    public ActorSystemMPSC_Static(WaitType waitType, Class ... classes) {
        this(waitType, DEFAULT_QUEUE_SIZE, ActorSystemMPSC_Static.class.getName(), System.err::println, classes);
    }

    public ActorSystemMPSC_Static(Class ... classes) {
        this(WaitType.LOCK, classes);
    }

    public static <I> I startAsDaemon(WaitType waitType, int queueSize, String threadName, Consumer<Throwable> errorConsumer, Class<I> i, String commitMethodName, I object) {
        ActorSystemMPSC_Static as = new ActorSystemMPSC_Static(waitType, queueSize, threadName, errorConsumer, i);
        as.startAsDaemon();
        return as.asynchronize(i, commitMethodName, object);
    }

    public static <I> I startAsDaemon(WaitType waitType, Class<I> i, String commitMethodName, I object) {
        return startAsDaemon(waitType, DEFAULT_QUEUE_SIZE, ActorSystemMPSC_Static.class.getName(), System.err::println, i, commitMethodName, object);
    }

    public static <I> I startAsDaemon(WaitType waitType, Class<I> i, I object) {
        return startAsDaemon(waitType, i, null, object);
    }

    public static <I> I startAsDaemon(Class<I> i, I object) {
        return startAsDaemon(WaitType.LOCK, i, object);
    }
}
