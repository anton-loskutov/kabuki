package org.kabuki.actor;

import org.kabuki.utils.concurrent.AgentThread;
import org.kabuki.utils.concurrent.GenericRunnableQueue;
import org.kabuki.utils.concurrent.GenericRunnable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;


public class ActorSystemProxy implements ActorSystem {
    public enum OverflowMode {
        FAIL,
        SKIP,
        WAIT,
    }

    private final AgentThread thread;
    private final GenericRunnableQueue queue;
    private final OverflowMode overflowMode;

    public ActorSystemProxy(int queueSize, String threadName, OverflowMode overflowMode, Consumer<Throwable> errorConsumer) {
        this.queue = new GenericRunnableQueue(queueSize) {
            @Override
            protected void onSleep() {
                commitCalls.forEach(CommitCall::run);
                cycle++;
            }
        };
        this.thread = new AgentThread(threadName, queue, error -> errorConsumer.accept(error instanceof MethodCall.Error ? error.getCause() : error));
        this.overflowMode = overflowMode;
    }

    public void start() {
        thread.start(false);
    }

    public void stop() {
        thread.stop();
    }

    public void startAsDaemon() {
        thread.start(true);
    }

    @Override
    public <I> I asynchronize(Class<I> i, I object) {
        return asynchronize(i, null, object);
    }

    @Override
    public <I> I asynchronize(Class<I> i, String commitMethodName, I object) {
        if (!i.isInterface()) {
            throw new IllegalArgumentException();
        }
        CommitCall commitCall = null;
        for (Method method : i.getMethods()) {
            if (!method.getReturnType().equals(Void.TYPE)) {
                throw new IllegalArgumentException();
            }
            if (commitMethodName != null && commitMethodName.equals(method.getName()) && method.getParameterCount() == 0) {
                commitCall = new CommitCall(object, method);
            }
        }
        if (commitCall != null) {
            commitCalls.add(commitCall);
        }

        CommitCall final_commitCall = commitCall;
        Consumer<GenericRunnable> consumer = toConsumer(queue, overflowMode);

        @SuppressWarnings("unchecked")
        final I actor = (I) Proxy.newProxyInstance(i.getClassLoader(), new Class<?>[]{i},  (proxy, method, args) -> {
            consumer.accept(new AcceptCall(object, method, args, final_commitCall));
            return null;
        });
        return actor;
    }

    // ---- private -----

    private static Consumer<GenericRunnable> toConsumer(GenericRunnableQueue queue, OverflowMode mode) {
        switch (mode) {
            case FAIL:
                return queue::add;
            case SKIP:
                return queue::offer;
            case WAIT:
                return queue::put;
            default:
                throw new AssertionError(mode);
        }
    }

    private static class MethodCall implements GenericRunnable {
        public final Object obj;
        public final Method method;
        public final Object[] args;

        public static class Error extends RuntimeException {
            public Error(Throwable cause) {
                super(cause);
            }
        }

        public MethodCall(Object obj, Method method, Object[] args) {
            this.obj = obj;
            this.method = method;
            this.args = args;
        }

        @Override
        public void run() {
            try {
                method.invoke(obj, args);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new Error(e);
            }
        }
    }

    private long cycle = 1;
    private CopyOnWriteArrayList<CommitCall> commitCalls = new CopyOnWriteArrayList<>();

    private class CommitCall extends MethodCall {
        public CommitCall(Object obj, Method method) {
            super(obj, method, new Object[0]);
        }

        public long cycle = 0;

        @Override
        public void run() {
            if (cycle == ActorSystemProxy.this.cycle) {
                super.run();
            }
        }
    }

    private class AcceptCall extends MethodCall {
        public AcceptCall(Object obj, Method method, Object[] args, CommitCall commit) {
            super(obj, method, args);
            this.commit = commit;
        }

        public final CommitCall commit;

        @Override
        public void run() {
            if (commit != null) {
                commit.cycle = ActorSystemProxy.this.cycle;
            }
            super.run();
        }
    }
}
