package org.kabuki.queues;

import org.kabuki.utils.concurrent.Asynchronizer;

public interface Queue extends Asynchronizer {

    void consume();

    void consumeShutdown();

    @Override
    <I> I asynchronize(Class<I> i, I o);

    @Override
    <I> I asynchronize(Class<I> i, String commitMethodName, I o);
}
