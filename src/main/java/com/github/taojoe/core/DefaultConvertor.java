package com.github.taojoe.core;

/**
 * Created by joe on 4/21/16.
 */
public class DefaultConvertor implements Convertor<Object> {
    @Override
    public int asInt(Object o) {
        return (int) o;
    }

    @Override
    public Object fromInt(int val) {
        return val;
    }

    @Override
    public long asLong(Object o) {
        return (long) o;
    }

    @Override
    public Object fromLong(long val) {
        return val;
    }

    @Override
    public float asFloat(Object o) {
        return (float) o;
    }

    @Override
    public Object fromFloat(float val) {
        return val;
    }

    @Override
    public double asDouble(Object o) {
        return (double) o;
    }

    @Override
    public Object fromDouble(double val) {
        return val;
    }

    @Override
    public boolean asBoolean(Object o) {
        return (boolean) o;
    }

    @Override
    public Object fromBoolean(boolean val) {
        return val;
    }

    @Override
    public String asString(Object o) {
        return o.toString();
    }

    @Override
    public Object fromString(String val) {
        return val;
    }

    @Override
    public byte[] asByteString(Object o) {
        return new byte[0];
    }

    @Override
    public Object fromByteString(byte[] val) {
        return val;
    }

    @Override
    public String asEnumName(Object o) {
        return o.toString();
    }

    @Override
    public Object fromEnumName(String val) {
        return val;
    }
}
