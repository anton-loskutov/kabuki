package org.kabuki.utils.mutable;

public class MutableBoolean {
    private boolean value;

    public boolean get() {
        return value;
    }

    public void set(boolean flag) {
        this.value = flag;
    }

    public void setTrue() {
        value = true;
    }

    public void setFalse() {
        value = false;
    }

    public boolean getAndSet(boolean value) {
        if (this.value == value) {
            return value;
        }
        this.value = value;
        return !value;
    }

    public boolean getAndSetFalse() {
        if (!value) {
            return false;
        }
        value = false;
        return true;

    }

    public boolean getAndSetTrue() {
        if (value) {
            return true;
        }
        value = true;
        return false;
    }
}
