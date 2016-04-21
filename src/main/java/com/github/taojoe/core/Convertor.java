package com.github.taojoe.core;

/**
 * Created by joe on 4/20/16.
 */
public class Convertor {
    interface IntConvertor<T>{
        int toInt(T t);
        T fromInt(int val);
    }
    interface LongConvertor<T>{
        int toLong(T t);
        T fromLong(long val);
    }
    interface FloatConvertor<T>{
        float toFloat(T t);
        T fromFloat(float val);
    }
    interface DoubleConvertor<T>{
        double toDouble(T t);
        T fromDouble(double val);
    }
    interface BooleanConvertor<T>{
        boolean toBoolean(T t);
        T fromBoolean(boolean val);
    }
    interface StringConvertor<T>{
        String toString(T t);
        T fromString(String val);
    }
    interface ByteStringConvertor<T>{
        byte[] toByteString(T t);
        T fromByteString(byte[] val);
    }
    interface EnumConvertor<T>{
        String toEnumName(T t);
        T fromEnumName(String val);
    }
}
