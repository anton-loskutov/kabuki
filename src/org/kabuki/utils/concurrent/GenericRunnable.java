package org.kabuki.utils.concurrent;

public interface GenericRunnable<T extends Throwable> {

    void run() throws T, InterruptedException;
}
