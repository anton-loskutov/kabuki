package org.kabuki.queues;

import org.kabuki.queues.mpsc.MPSC_Queue;
import org.kabuki.queues.mpsc.MPSC_SlotType;
import org.kabuki.utils.concurrent.WaitType;
import org.metaja.Metaja;
import org.metaja.utils.ClassUtils;

import java.util.function.Consumer;

public class Queues {

    public static Queue mpsc(WaitType type, MPSC_SlotType slotType, int size, Consumer<Throwable> errorConsumer) {
        return ClassUtils.newInstance(
                Metaja.<Queue>load(MPSC_Queue.class.getName(), new Object[]{ type, slotType }),
                size, errorConsumer
        );
    }
}
