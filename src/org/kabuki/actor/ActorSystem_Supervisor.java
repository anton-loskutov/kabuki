package org.kabuki.actor;

import org.kabuki.utils.concurrent.WaitType;

import java.util.ArrayList;
import java.util.function.Consumer;

public class ActorSystem_Supervisor {

    private final ArrayList<ActorSystem> systems = new ArrayList<>();
    private final ArrayList<Consumer<Throwable>> errorConsumers = new ArrayList<>();
    private final Consumer<Throwable> errorConsumersHub = throwable -> errorConsumers.forEach(errorConsumer -> errorConsumer.accept(throwable));

    public synchronized ActorSystem createDynamic(WaitType waitType, int queueSize, String threadName) {
        ActorSystemMPSC_Dynamic system = new ActorSystemMPSC_Dynamic(waitType, queueSize, threadName, errorConsumersHub);
        systems.add(system);
        return system;
    }

    public synchronized ActorSystem createStatic(WaitType waitType, int queueSize, String threadName, Class ... classes) {
        ActorSystemMPSC_Static system = new ActorSystemMPSC_Static(waitType, queueSize, threadName, errorConsumersHub, classes);
        systems.add(system);
        return system;
    }

    public synchronized void start() {
        systems.forEach(ActorSystem::start);
    }

    public synchronized void stop() {
        systems.forEach(ActorSystem::stop);
    }

    public synchronized void addErrorConsumer(Consumer<Throwable> errorConsumer) {
        errorConsumers.add(errorConsumer);
    }
}
