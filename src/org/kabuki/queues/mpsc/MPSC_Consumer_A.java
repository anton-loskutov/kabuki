/*** (int id, java.lang.Class iface, java.lang.reflect.Method commit) { ***/

package org.kabuki.queues.mpsc;

import org.kabuki.utils.MutableBoolean;

public class MPSC_Consumer_A/***#_$id$ implements $iface.getCanonicalName()$ #***/ {

    public final MPSC_Queue q;

    /***/
    public MPSC_Consumer<MPSC_Slot> c;
    /***
     REFLECTION.getMethods(iface, STATIC::notModified).forEach((method) -> {
        # public final MPSC_Consumer<MPSC_Slot> c$method.ix()$; #
     });
    ***/

    public final MutableBoolean commitFlag = new MutableBoolean();

    public MPSC_Consumer_A/***#_$id$#***/ (MPSC_Queue q, Object target) {
        this.q = q;
        /***
         REFLECTION.getMethods(iface, STATIC::notModified).forEach((method) -> {
            String clazz = IMPORT("org/kabuki/queues/mpsc/MPSC_Consumer_B.java", "UTF-8", id, method.ix() + 1, method.it(), commit)[0];
            # this.c$method.ix()$ = $ NEW(clazz, "this.commitFlag", "target") $ #
         });
         ***/
    }

    /***/
    public void accept(Object o) {
        /***
         REFLECTION.getMethods(iface, STATIC::notModified).forEach((method) -> {
            # public void $method.it().getName()$( #
            ITERATION.asStream(method.it().getParameterTypes()).forEach((type) -> {
                # $type.it().getCanonicalName()$ arg$type.ix()$ #
                if (!type.last()) {
                    # , #
                }
            });
            # ) { #
         ***/

        MPSC_Slot s = q.obtainPutSlot();

        /***
            int[] x = new int[] {1, 1}; // counter of primitive and reference types
            ITERATION.asStream(method.it().getParameterTypes()).forEach((type) -> {
                if (REFLECTION.isFloating(type.it())) {
                    # s.p$x[0]++$ = java.lang.Double.doubleToRawLongBits(arg$type.ix()$); #
                } else if (REFLECTION.isPrimitive(type.it())) {
                    # s.p$x[0]++$ = arg$type.ix()$; #
                } else {
                    # s.o$x[1]++$ = arg$type.ix()$; #
                }
            });

            #s.consumer = c$method.ix()$;#
            #s.commit();#
        /***/

        s.o1 = o;
        s.consumer = c;
        s.commit();

        /***# } # }); /***/
    }
    /***/
}
/*** } ***/
