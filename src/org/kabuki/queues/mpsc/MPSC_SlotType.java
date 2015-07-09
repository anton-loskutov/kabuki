package org.kabuki.queues.mpsc;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Function;

import static java.util.stream.Collectors.reducing;

public class MPSC_SlotType {
    public static final MPSC_SlotType ZERO = new MPSC_SlotType(0,0);

    public final int ps;
    public final int os;

    public MPSC_SlotType(int ps, int os) {
        if (ps < 0 || os < 0) {
            throw new IllegalArgumentException();
        }
        this.ps = ps;
        this.os = os;
    }

    public boolean matches(MPSC_SlotType type) {
        return ps <= type.ps && os <= type.os;
    }

    public static MPSC_SlotType get(Method m) {
        Function<Boolean, Integer> counter = (primitive) -> Arrays.asList(m.getParameterTypes()).stream().collect(reducing(0, (a) -> primitive ^ a.isPrimitive() ? 0 : 1, Integer::sum));
        return new MPSC_SlotType(counter.apply(true),counter.apply(false));
    }

    public static MPSC_SlotType get(Class<?> c) {
        return Arrays.asList(c.getMethods()).stream().collect(reducing(ZERO, MPSC_SlotType::get, MPSC_SlotType::max));
    }

    public static MPSC_SlotType max(MPSC_SlotType t1, MPSC_SlotType t2) {
        return new MPSC_SlotType(Math.max(t1.ps, t2.ps), Math.max(t1.os, t2.os));
    }
}
