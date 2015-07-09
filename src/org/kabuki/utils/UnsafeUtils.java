package org.kabuki.utils;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.TreeMap;

public class UnsafeUtils {
    public static final Unsafe unsafe;

    static {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe = (Unsafe) unsafeField.get(null);

        } catch (Throwable exception) {
            throw new Error("Can't get unsafe", exception);
        }
    }

    public static long getDeclaredFieldOffset(Class<?> type, String name) {
        try {
            return unsafe.objectFieldOffset(type.getDeclaredField(name));
        } catch (NoSuchFieldException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    // TODO: do special version for java 8 ?
    public static int getAndIncrementInt(Object object, long offset) {
        for (;;) {
            int current = unsafe.getIntVolatile(object, offset);
            int next = current + 1;
            if (compareAndSet(object, offset, current, next)) {
                return current;
            }
        }
    }

    public static boolean compareAndSet(Object object, long offset, int expect, int update) {
        return unsafe.compareAndSwapInt(object, offset, expect, update);
    }

    public static boolean compareAndSet(Object object, long offset, long expect, long update) {
        return unsafe.compareAndSwapLong(object, offset, expect, update);
    }

    public static void putOrderedInt(Object object, long offset, int update) {
        unsafe.putOrderedInt(object, offset, update);
    }

    public static String getLayout(Class c) {
        TreeMap<Long, Field> fields = new TreeMap<>();
        for (Field field : c.getDeclaredFields()) {
            if ((field.getModifiers() & Modifier.STATIC) == 0) {
                fields.put(unsafe.objectFieldOffset(field), field);
            }
        }
        StringBuilder s = new StringBuilder(c.getCanonicalName()).append("[");
        for (Map.Entry<Long, Field> field : fields.entrySet()) {
            s.append(field.getKey()).append(":").append(field.getValue().getName()).append(", ");
        }
        if (!fields.isEmpty()) {
            s.setLength(s.length() - 2);
        }
        s.append("]");
        return s.toString();
    }
}