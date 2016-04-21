package com.github.taojoe.core;

import com.google.protobuf.Descriptors;

/**
 * Created by joe on 4/20/16.
 */
public interface Convertor<T> {
    T fromMessageValue(Object value);
    Object toMessageValue(Object value, Descriptors.FieldDescriptor fieldDescriptor);
}
