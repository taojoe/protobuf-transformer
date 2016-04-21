package com.github.taojoe.core;

import com.google.protobuf.Descriptors;

/**
 * Created by joe on 4/20/16.
 */
public interface Convertor<T> {
    int asInt(T t);
    T fromInt(int val);
    long asLong(T t);
    T fromLong(long val);
    float asFloat(T t);
    T fromFloat(float val);
    double asDouble(T t);
    T fromDouble(double val);
    boolean asBoolean(T t);
    T fromBoolean(boolean val);
    String asString(T t);
    T fromString(String val);
    byte[] asByteString(T t);
    T fromByteString(byte[] val);
    String asEnumName(T t);
    T fromEnumName(String val);
    default Object asMessageType(T t, Descriptors.FieldDescriptor fieldDescriptor){
        Descriptors.FieldDescriptor.JavaType type= fieldDescriptor.getJavaType();
        if(type.equals(Descriptors.FieldDescriptor.JavaType.INT)){
            return asInt(t);
        }else if(type.equals(Descriptors.FieldDescriptor.JavaType.LONG)){
            return asLong(t);
        }else if(type.equals(Descriptors.FieldDescriptor.JavaType.FLOAT)){
            return asFloat(t);
        }else if(type.equals(Descriptors.FieldDescriptor.JavaType.DOUBLE)){
            return asDouble(t);
        }else if(type.equals(Descriptors.FieldDescriptor.JavaType.BOOLEAN)){
            return asBoolean(t);
        }else if(type.equals(Descriptors.FieldDescriptor.JavaType.STRING)){
            return asString(t);
        }else if(type.equals(Descriptors.FieldDescriptor.JavaType.BYTE_STRING)){
            return asByteString(t);
        }else if(type.equals(Descriptors.FieldDescriptor.JavaType.ENUM)){
            return fieldDescriptor.getEnumType().findValueByName(asEnumName(t));
        }
        return null;
    }
    default T fromMessageType(Object value, Descriptors.FieldDescriptor fieldDescriptor){
        Descriptors.FieldDescriptor.JavaType type=fieldDescriptor.getJavaType();
        if(type.equals(Descriptors.FieldDescriptor.JavaType.INT)){
            return fromInt((int)value);
        }else if(type.equals(Descriptors.FieldDescriptor.JavaType.LONG)){
            return fromLong((long)value);
        }else if(type.equals(Descriptors.FieldDescriptor.JavaType.FLOAT)){
            return fromFloat((float)value);
        }else if(type.equals(Descriptors.FieldDescriptor.JavaType.DOUBLE)){
            return fromDouble((double)value);
        }else if(type.equals(Descriptors.FieldDescriptor.JavaType.BOOLEAN)){
            return fromBoolean((boolean)value);
        }else if(type.equals(Descriptors.FieldDescriptor.JavaType.STRING)){
            return fromString((String)value);
        }else if(type.equals(Descriptors.FieldDescriptor.JavaType.BYTE_STRING)){
            return fromByteString((byte[])value);
        }else if(type.equals(Descriptors.FieldDescriptor.JavaType.ENUM)){
            Descriptors.EnumValueDescriptor vd=(Descriptors.EnumValueDescriptor) value;
            return fromEnumName(vd.getName());
        }
        return null;
    }
}
