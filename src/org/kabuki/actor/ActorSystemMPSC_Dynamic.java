package org.kabuki.actor;

import org.kabuki.queues.Queue;
import org.kabuki.queues.Queues;
import org.kabuki.queues.mpsc.MPSC_SlotType;
import org.kabuki.utils.concurrent.WaitType;
import org.metaja.common.Composite;
import org.metaja.common.Stub;
import org.metaja.utils.ClassUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.util.stream.Collectors.reducing;

public class ActorSystemMPSC_Dynamic extends ActorSystemMPSC {

    private final WaitType waitType;
    private final int queueSize;
    private List<ActorReference<?>> actorReferences = new ArrayList<>();

    public ActorSystemMPSC_Dynamic(WaitType waitType, int queueSize, String threadName, Consumer<Throwable> errorConsumer) {
        super(threadName, errorConsumer);
        this.waitType = waitType;
        this.queueSize = queueSize;
    }

    @Override
    public synchronized <I> I asynchronize(Class<I> i, String commitMethodName, I object) {
        if (!i.isInterface() || !ClassUtils.isPublic(i)) {
            throw new IllegalArgumentException(i.getCanonicalName() + " is not a public interface!");
        }

        if (actorReferences == null) {
            throw new IllegalStateException("Actor system can not make actor after first start!");
        }

        ActorReference<I> actorReference = new ActorReference<>(i, commitMethodName, object);
        actorReferences.add(actorReference);
        return actorReference.actor;
    }

    @Override
    protected void initBeforeStart() {
        if (actorReferences != null) {
            MPSC_SlotType queueSlotType = actorReferences.stream().map(actorReference -> actorReference.type).collect(reducing(MPSC_SlotType.ZERO, MPSC_SlotType::get, MPSC_SlotType::max));
            queue = Queues.mpsc(waitType, queueSlotType, queueSize, errorConsumer);
            actorReferences.forEach(actorReference -> actorReference.attach(queue));
            actorReferences = null;
        }
    }

    // ===== private =====

    private static class ActorReference<I> {

        public final Class<I> type;
        public final String commitMethodName;
        public final I object;
        public final I actor;

        public ActorReference(Class<I> type, String commitMethodName, I object) {
            this.type = type;
            this.commitMethodName = commitMethodName;
            this.object = object;
            this.actor = Composite.create(type, Stub.create(type, () -> { throw new IllegalStateException("Actor system is not started!"); }));
        }

        @SuppressWarnings("unchecked")
        public void attach(Queue queue) {
            ((Consumer<I>)actor).accept(
                    commitMethodName == null
                    ? queue.asynchronize(type, object)
                    : queue.asynchronize(type, commitMethodName, object)
            );
        }
    }

    // ----- some sugar ----

    public ActorSystemMPSC_Dynamic(WaitType waitType) {
        this(waitType, 1024, ActorSystemMPSC_Dynamic.class.getName(), Throwable::printStackTrace);
    }

    public ActorSystemMPSC_Dynamic() {
        this(WaitType.LOCK);
    }
}
