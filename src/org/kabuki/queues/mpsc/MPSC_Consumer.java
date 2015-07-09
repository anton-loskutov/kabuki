package org.kabuki.queues.mpsc;

import java.util.function.Consumer;

public interface MPSC_Consumer<T> extends Consumer<T> {

    void commit();
}
