/*** (org.kabuki.utils.concurrent.WaitType waitType, org.kabuki.queues.mpsc.MPSC_SlotType slotType) { ***/

/*** boolean LOCK = waitType == org.kabuki.utils.concurrent.WaitType.LOCK; ***/
/*** boolean SPIN = waitType == org.kabuki.utils.concurrent.WaitType.SPIN; ***/

package org.kabuki.queues.mpsc;

import org.kabuki.utils.UnsafeUtils;
import sun.misc.Contended;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MPSC_Slot {

    public /*** if (SPIN) { ***/ @Contended("turn") volatile /*** } ***/ int turn;

    /*** if (SPIN) { ***/
    public static long turn_ = UnsafeUtils.getDeclaredFieldOffset(MPSC_Slot.class, "turn");
    /*** } ***/

    /*** if (LOCK) { ***/
    public final ReentrantLock l = new ReentrantLock();
    public final Condition r = l.newCondition();
    public final Condition w = l.newCondition();
    /*** } ***/

    public MPSC_Consumer<MPSC_Slot> consumer;

    /*** /***/
    public Object o1;
    /***/

    // DATA FIELDS
    /***
        for (int i=1; i<=slotType.ps; i++) {
            # public long l$i$; #
        }
        for (int i=1; i<=slotType.os; i++) {
            # public Object o$i$; #
        }
     ***/

    public MPSC_Slot(int turn) {
        this.turn = turn;
    }

    public void commit() {

        /*** if (SPIN) { ***/
        UnsafeUtils.putOrderedInt(this, turn_, turn + 1);
        /*** } ***/

        /*** if (LOCK) { ***/
        turn++;
        r.signal();
        l.unlock();
        /*** } ***/
    }

    public void release(int turn) {

        // RELEASE DATA FIELDS
        /***
         for (int i=1; i<=slotType.os; i++) {
            # this.o$i$ = null; #
         }
         ***/

        this.consumer = null;

        /*** if (SPIN) {***/
        UnsafeUtils.putOrderedInt(this, turn_, turn);
        /*** } ***/

        /*** if (LOCK) { ***/
        this.turn = turn;
        w.signal();
        l.unlock();
        /*** } ***/

    }

}
/*** } ***/
