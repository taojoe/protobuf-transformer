package com.github.taojoe.helper;

/**
 * Created by joe on 8/22/16.
 */
public interface PropertyWriter {
    void write(Object value);
    Class valueClass();
}
