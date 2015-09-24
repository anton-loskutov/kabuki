/*** (int a_id, int id, java.lang.reflect.Method accept, java.lang.reflect.Method commit) { ***/

package org.kabuki.queues.mpsc;

import org.kabuki.utils.mutable.MutableBoolean;

public class MPSC_Consumer_B/***#_$a_id$_$id$#***/ implements MPSC_Consumer<MPSC_Slot> {

    public final /*** $accept.getDeclaringClass().getCanonicalName()$ //***/ Object o;

    /*** if (commit != null) { ***/
    public final MutableBoolean commitFlag;
    /*** } ***/

    public MPSC_Consumer_B/***#_$a_id$_$id$#***/ (MutableBoolean commitFlag, /*** $accept.getDeclaringClass().getCanonicalName()$ //***/Object o) {
        this.o = o;

        /*** if (commit != null) { ***/
        this.commitFlag = commitFlag;
        /*** } ***/
    }

    public void accept(MPSC_Slot slot) {
        /*** if (commit != null) { ***/
        commitFlag.setTrue();
        /*** } ***/

        /***
         # o.$accept.getName()$( #
         int[] x = new int[] {1, 1}; // counter of primitive and reference types
         ITERATION.stream(accept.getParameterTypes()).forEach((type) -> {
            # ($type.it().getCanonicalName()$) #
            if (REFLECTION.isFloating(type.it())) {
                # java.lang.Double.longBitsToDouble(slot.l$x[0]++$) #
            } else if (REFLECTION.isPrimitive(type.it())) {
                # slot.l$x[0]++$ #
            } else {
                # slot.o$x[1]++$ #
            }
            if (!type.last()) {
                # , #
            }
         });
         #); #
         ***/
    }

    public void commit() {
        /*** if (commit != null) { ***/
        if (commitFlag.getAndSetFalse()) {
            /***# o.$commit.getName()$(); #***/
        }
        /*** } ***/
    }
}
/*** } ***/