package org.kabuki.actor;

import org.kabuki.utils.concurrent.Asynchronizer;
import org.kabuki.utils.concurrent.Agent;

public interface ActorSystem extends Agent, Asynchronizer {

    @Override
    <I> I asynchronize(Class<I> i, I object);

    @Override
    <I> I asynchronize(Class<I> i, String commitMethodName, I object);

    @Override
    void start();

    @Override
    void stop();
}
