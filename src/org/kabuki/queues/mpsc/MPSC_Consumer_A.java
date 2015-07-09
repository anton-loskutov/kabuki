/*** (int id, java.lang.Class iface, java.lang.reflect.Method commit) { ***/

package org.kabuki.queues.mpsc;

import org.kabuki.utils.MutableBoolean;

public class MPSC_Consumer_A/***#_$id$ implements $iface.getCanonicalName()$ #***/ {

    public final MPSC_Queue q;

    /***/
    public MPSC_Consumer<MPSC_Slot> c;
    /***
     for (int m = 0; m < iface.getMethods().length; m++) {
        java.lang.reflect.Method method = iface.getMethods()[m];
        if ((method.getModifiers() & java.lang.reflect.Modifier.STATIC) == 0) {

            # public final MPSC_Consumer<MPSC_Slot> c$m$; #
        }
     }
    ***/

    public final MutableBoolean commitFlag = new MutableBoolean();

    public MPSC_Consumer_A/***#_$id$#***/ (MPSC_Queue q, Object target) {
        this.q = q;
        /***
         for (int m = 0; m < iface.getMethods().length; m++) {
            java.lang.reflect.Method method = iface.getMethods()[m];
            if ((method.getModifiers() & java.lang.reflect.Modifier.STATIC) == 0) {

                String clazz = IMPORT("org/kabuki/queues/mpsc/MPSC_Consumer_B.java", "UTF-8", id, m + 1, method, commit)[0];
                # this.c$m$ = $ NEW(clazz, "this.commitFlag", "target") $ #
            }
         }
         ***/
    }

    /***/
    public void accept(Object o) {
        /***
         for (int m = 0; m < iface.getMethods().length; m++) {
            java.lang.reflect.Method method = iface.getMethods()[m];
            if ((method.getModifiers() & java.lang.reflect.Modifier.STATIC) == 0) {

            # public void $method.getName()$( #
            COLLECTIONS.iterate(ARRAYS.asList(method.getParameterTypes()), new int[] {1, 1}, (type, c) -> {
                if (type.isPrimitive()) {
                    # $type.getCanonicalName()$ p$c[0]++$ #
                } else {
                    # $type.getCanonicalName()$ o$c[1]++$ #
                }
            }, ()->{#,#});
            # ) { #
         ***/

        MPSC_Slot s = q.obtainPutSlot();

        /***
            COLLECTIONS.iterate(ARRAYS.asList(method.getParameterTypes()), new int[] {1, 1}, (type, c) -> {
                if (type.equals(Double.TYPE) || type.equals(Float.TYPE)) {
                    # s.p$c[0]$ = java.lang.Double.doubleToRawLongBits(p$c[0]++$); #
                } else if (type.isPrimitive()) {
                    # s.p$c[0]$ = p$c[0]++$; #
                } else {
                    # s.o$c[1]$ = o$c[1]++$; #
                }
            });
            #s.consumer = c$m$;#
            #s.commit();#
        /***/

        s.o1 = o;
        s.consumer = c;
        s.commit();

        /***# } # }} /***/
    }
    /***/
}
/*** } ***/
