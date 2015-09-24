package org.kabuki.utils.concurrent;

import java.lang.management.ThreadInfo;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Function;

public abstract class AgentThreadState {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat();

    public final long timestamp;
    public final String threadName;

    public AgentThreadState(long timestamp, String threadName) {
        this.timestamp = timestamp;
        this.threadName = threadName;
    }


    /** STARTED **/
    public static class Started extends AgentThreadState {
        public final ThreadInfo info;
        public final long nativeId;

        public Started(long timestamp, String threadName, ThreadInfo info, long nativeId) {
            super(timestamp, threadName);
            this.info = info;
            this.nativeId = nativeId;
        }

        @Override
        public <R> R visit(Visitor<R> visitor) {
            return visitor.onStarted(this);
        }

        @Override
        public String toString() {
            return "Thread [" + threadName + "] STARTED at '" + DATE_FORMAT.format(new Date(timestamp)) + "' (id='" + info.getThreadId() + "' nid='" + nativeId + "' state='" + info.getThreadState() + "')";
        }
    }


    /** STOPPED **/
    public static class Stopped extends AgentThreadState {

        public Stopped(long timestamp, String threadName) {
            super(timestamp, threadName);
        }

        @Override
        public <R> R visit(Visitor<R> visitor) {
            return visitor.onStopped(this);
        }

        @Override
        public String toString() {
            return "Thread [" + threadName + "] STOPPED at '" + DATE_FORMAT.format(new Date(timestamp)) + "'";

        }
    }


    /** STOPPED WITH ERROR **/
    public static class StoppedWithError extends AgentThreadState {
        public final Throwable error;

        public StoppedWithError(long timestamp, String threadName, Throwable error) {
            super(timestamp, threadName);
            this.error = error;
        }

        @Override
        public <R> R visit(Visitor<R> visitor) {
            return visitor.onStoppedWithError(this);
        }

        @Override
        public String toString() {
            return "Thread [" + threadName + "] STOPPED WITH ERROR at '" + DATE_FORMAT.format(new Date(timestamp)) + "' (" + error.toString() + ")";
        }
    }

    // ===== visitor =====

    public interface Visitor<R> {
        R onStarted(Started state);
        R onStopped(Stopped state);
        R onStoppedWithError(StoppedWithError state);
    }

    public abstract <R> R visit(Visitor<R> visitor);

    public <R> R visit(
            Function<Started,R> onStarted,
            Function<Stopped,R> onStopped,
            Function<StoppedWithError,R> onStoppedWithError
    ) {
        return visit(new Visitor<R>() {
            @Override
            public R onStarted(Started state) {
                return onStarted.apply(state);
            }

            @Override
            public R onStopped(Stopped state) {
                return onStopped.apply(state);
            }

            @Override
            public R onStoppedWithError(StoppedWithError state) {
                return onStoppedWithError.apply(state);
            }
        });
    }
}

