package org.kabuki.utils;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import sun.tools.attach.HotSpotVirtualMachine;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;

public class ThreadUtils {

    private static ThreadMXBean threadMX = ManagementFactory.getThreadMXBean();

    public static ThreadInfo getInfo(long threadId) {
        return threadMX.getThreadInfo(threadId, 256);
    }

    public static Long getNativeId(String threadName) {
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            String pid = name.substring(0, name.indexOf('@'));

            HotSpotVirtualMachine vm = (HotSpotVirtualMachine) VirtualMachine.attach(pid);

            InputStream in = vm.remoteDataDump("-l");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    if (line.startsWith("\"") && threadName.equals(line.substring(1, line.indexOf('"', 1)))) {
                        int nidIx = line.indexOf("nid=", line.indexOf('"', 1));
                        return Long.parseLong(line.substring(nidIx + 6, line.indexOf(' ', nidIx)), 16);
                    }
                }
            }
            return null;

        } catch (IOException | AttachNotSupportedException e) {
            throw new Error(e);
        }
    }

    public static String printStackTrace(StackTraceElement[] stackTrace) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        for (StackTraceElement stackTraceElement : stackTrace) {
            pw.print('\t');
            pw.println(stackTraceElement);
        }
        return sw.toString();
    }
}
