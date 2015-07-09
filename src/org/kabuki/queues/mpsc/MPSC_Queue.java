/*** (org.kabuki.utils.concurrent.WaitType waitType, org.kabuki.queues.mpsc.MPSC_SlotType slotType) { ***/

/*** boolean LOCK = waitType == org.kabuki.utils.concurrent.WaitType.LOCK; ***/
/*** boolean SPIN = waitType == org.kabuki.utils.concurrent.WaitType.SPIN; ***/

/*** String MPSC_Slot_class = IMPORT("org/kabuki/queues/mpsc/MPSC_Slot.java", "UTF-8", waitType, slotType)[0]; ***/

package org.kabuki.queues.mpsc;

import org.kabuki.queues.Queue;
import org.kabuki.utils.IntegerUtils;
import org.kabuki.utils.UnsafeUtils;
import sun.misc.Contended;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class MPSC_Queue implements Queue {

    public final int mask;

    public final MPSC_Slot[] slots;
    public final MPSC_SlotType slotType = new MPSC_SlotType(/***$slotType.ps$//***/0 ,/***$slotType.os$//***/0 );

    private final Consumer<Throwable> errorConsumer;

    public @Contended("put") volatile int putCursor = 0;
    public static final long putCursor_ = UnsafeUtils.getDeclaredFieldOffset(MPSC_Queue.class, "putCursor");

    public int getCursor = 1;

    public MPSC_Queue(int size, Consumer<Throwable> errorConsumer) {
        this.errorConsumer = errorConsumer;
        size = IntegerUtils.roundPositiveIntToPowerOf2(size);
        slots = new MPSC_Slot[size];
        for (int i = 0; i < size; i++) {
            slots[i] = /*** $ NEW(MPSC_Slot_class, "i") $ /***/  new MPSC_Slot(i); /***/
        }
        mask = (size - 1);
    }

    @Override
    public <I> I asynchronize(Class<I> iface, String commitMethodName, I o) {
        Method commitMethod = null;
        for (Method method : iface.getMethods()) {
            if ((method.getModifiers() & Modifier.STATIC) == 0) {
                if (!method.getReturnType().equals(Void.TYPE)) {
                    throw new IllegalArgumentException("Can not asynchronize interface with method with non-void return types!");
                }
                if (commitMethodName != null && method.getName().equals(commitMethodName) && method.getParameterCount() == 0) {
                    commitMethod = method;
                }
                if (!MPSC_SlotType.get(method).matches(slotType)) {
                    throw new IllegalArgumentException("Method have too many arguments!");
                }
            }
        }
        if (commitMethodName != null && commitMethod == null) {
            throw new IllegalArgumentException("Can not find method '" + commitMethodName + "()' in '" + iface.getCanonicalName() + "'!");
        }

        // Generate new consumer:
        /***# String className = $IMPORT_DYN("org/kabuki/queues/mpsc/MPSC_Consumer_A.java", "UTF-8", true, "iface", "commitMethod")$[0]; #***/
        /***# return $NEW_DYN("className", "I", "this", "o")$ /***/
        return o;
        /***/
    }

    @Override
    public <I> I asynchronize(Class<I> i, I o) {
        return asynchronize(i, null, o);
    }

    public MPSC_Slot obtainPutSlot() {
        int putCursor = UnsafeUtils.getAndIncrementInt(this, MPSC_Queue.putCursor_);
        MPSC_Slot slot = slots[putCursor & mask];

        /*** if (LOCK) ***/ slot.l.lock();

        while (slot.turn != putCursor) {
            /*** if (LOCK) ***/ slot.w.awaitUninterruptibly();
        }

        return slot;
    }

    private final ReentrantLock consume = new ReentrantLock();

    public void consume() {
        if (!consume.tryLock()) {
            throw new IllegalStateException("Queue is for single consumer!");
        }
        try {

            MPSC_Slot[] slots = this.slots;
            int getCursor = this.getCursor;
            MPSC_Slot slot = slots[(getCursor - 1) & mask];
            int MAX_BATCH = slots.length; // TODO: make parameter

            @SuppressWarnings("unchecked")
            MPSC_Consumer<MPSC_Slot>[] consumersToCommit = new MPSC_Consumer[MAX_BATCH];
            int consumersToCommit_length = 0;

            /*** if (LOCK) ***/slot.l.lock();

            consume:
            for (; ; ) {
                while (slot.turn != getCursor) {
                    /*** if (LOCK) ***/slot.r.awaitUninterruptibly();
                }

                do {

                    MPSC_Consumer<MPSC_Slot> consumer = slot.consumer;

                    try {
                        consumer.accept(slot);
                    } catch (Shutdown shutdown) {
                        break consume;
                    } catch (Throwable error) {
                        errorConsumer.accept(error);
                    } finally {
                        slot.release(getCursor + mask);
                    }

                    consumersToCommit[consumersToCommit_length++] = consumer;

                    slot = slots[getCursor & mask];

                    /*** if (LOCK) ***/slot.l.lock();

                    getCursor++;

                } while (slot.turn == getCursor && consumersToCommit_length < MAX_BATCH);

                // commit
                commit(consumersToCommit, consumersToCommit_length);
                consumersToCommit_length = 0;
            }

            commit(consumersToCommit, consumersToCommit_length);
            this.getCursor = getCursor + 1;

        } finally {
            consume.unlock();
        }
    }

    private void commit(MPSC_Consumer<MPSC_Slot>[] consumersToCommit, int consumersToCommit_length) {
        for (int i = 0; i < consumersToCommit_length; i++) {
            try {
                consumersToCommit[i].commit();
            } catch (Throwable error) {
                errorConsumer.accept(error);
            }
            consumersToCommit[i] = null;
        }
    }

    // ======= shutdown =======

    private static class Shutdown extends RuntimeException {
    }

    public final Runnable shutdown = asynchronize(Runnable.class, () -> {
        throw new Shutdown();
    });

    public void consumeShutdown() {
        shutdown.run();
    }
}

/*** } ***/
