package com.kabuki;

public interface Actor<M> {

    void onMessage(M message);

    @SuppressWarnings("unchecked")
    static <M> Class<Actor<M>> unchecked(Class<M> c) {
        return ((Class)Actor.class);
    }
}
