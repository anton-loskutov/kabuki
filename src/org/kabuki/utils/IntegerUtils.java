package org.kabuki.utils;

public class IntegerUtils {

    public static int roundPositiveIntToPowerOf2(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException();
        }
        return Integer.bitCount(i) == 1 ? i : Integer.highestOneBit(i) << 1;
    }
}
