package org.kabuki.perf;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ActorSystem_A_DisruptorBased extends ActorSystem_A {
    private Disruptor<Event> disruptor;
    private RingBuffer<Event> ringBuffer;

    private final int queueSize;
    private final WaitStrategy ws;
    private ExecutorService executor;

    public ActorSystem_A_DisruptorBased(String threadName, int queueSize, WaitStrategy ws) {
        this.executor = Executors.newSingleThreadExecutor(r -> new Thread(null, r, threadName));
        this.queueSize = queueSize;
        this.ws = ws;
    }

    public class Event {
        public String msg;
    }

    @Override
    public void start() {
        disruptor = new Disruptor<>(Event::new, queueSize, executor, ProducerType.MULTI, ws);
        disruptor.handleEventsWith((event, sequence, endOfBatch) -> a.onMessage(event.msg));
        ringBuffer = disruptor.getRingBuffer();
        disruptor.start();
    }

    @Override
    public void stop() {
        disruptor.shutdown();
    }

    @Override
    public void onMessage(String msg) {
        long seq = ringBuffer.next();
        ringBuffer.get(seq).msg = msg;
        ringBuffer.publish(seq);
    }
}
