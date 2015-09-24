package org.kabuki.utils.concurrent;

import org.kabuki.utils.ThreadUtils;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.newSetFromMap;

public class AgentThreadRegistry {

    private static final Set<AgentThread> THREADS = newSetFromMap(new WeakHashMap<>());

    /*package*/ static void register(AgentThread thread) {
        THREADS.add(thread);
    }

    public static synchronized AgentThread[] getAgentThreads(Predicate<? super AgentThread> filter) {
        return THREADS.stream().filter(filter).toArray(AgentThread[]::new);
    }

    public static synchronized AgentThread getAgentThread(Predicate<? super AgentThread> filter) {
        return THREADS.stream().filter(filter).findAny().orElse(null);
    }

    // ====== jmx ======

    public interface AgentThreadRegistryPointMBean {
        String getState(String threadName, boolean stackTrace);
        String getNativeId(String threadName);
    }

    public static class AgentThreadRegistryPoint implements AgentThreadRegistryPointMBean {
        public static final String LINE_SEPARATOR = System.getProperty("line.separator");

        @Override
        public String getState(String threadName, boolean stackTrace) {
            return Stream.of(getAgentThreads(thread -> thread.getName().equals(threadName)))
                    .map(AgentThread::getState)
                    .flatMap(state -> stackTrace && state instanceof AgentThreadState.Started
                            ? Stream.of(state.toString(), ThreadUtils.printStackTrace(((AgentThreadState.Started) state).info.getStackTrace()))
                            : Stream.of(state.toString()))
                    .collect(Collectors.joining(LINE_SEPARATOR));
        }

        @Override
        public String getNativeId(String threadName) {
            return Stream.of(getAgentThreads(thread -> thread.getName().equals(threadName)))
                    .map(AgentThread::getState)
                    .filter(state -> state instanceof AgentThreadState.Started)
                    .map(state -> String.valueOf(((AgentThreadState.Started) state).nativeId))
                    .collect(Collectors.joining(LINE_SEPARATOR));
        }
    }

    static {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            mbs.registerMBean(new AgentThreadRegistryPoint(), new ObjectName(AgentThreadRegistry.class.getPackage().getName() + ":type=" + AgentThreadRegistry.class.getSimpleName()));
        } catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
            throw new Error(e);
        }
    }
}
