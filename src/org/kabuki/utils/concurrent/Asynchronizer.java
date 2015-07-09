package org.kabuki.utils.concurrent;

public interface Asynchronizer {

    <I> I asynchronize(Class<I> i, I o);

    <I> I asynchronize(Class<I> i, String commitMethodName, I o);
}
