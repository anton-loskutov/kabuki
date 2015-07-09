/*** (int a_id, int id, java.lang.reflect.Method accept, java.lang.reflect.Method commit) { ***/

package org.kabuki.queues.mpsc;

import org.kabuki.utils.MutableBoolean;

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
         COLLECTIONS.iterate(ARRAYS.asList(accept.getParameterTypes()), new int[] {1, 1}, (type, c) -> {
            if (type.equals(Double.TYPE) || type.equals(Float.TYPE)) {
                # ($type.getCanonicalName()$) java.lang.Double.longBitsToDouble(slot.l$c[0]++$) #
            } else if (type.isPrimitive()) {
                # ($type.getCanonicalName()$) slot.l$c[0]++$ #
            } else {
                # ($type.getCanonicalName()$) slot.o$c[1]++$ #
            }
         }, ()->{#,#});
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