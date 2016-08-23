package com.github.taojoe.core;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Created by joe on 8/22/16.
 */
public class CoreValueCoerce {
    private static final Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS
            = new ImmutableMap.Builder<Class<?>, Class<?>>()
            .put(boolean.class, Boolean.class)
            .put(byte.class, Byte.class)
            .put(char.class, Character.class)
            .put(double.class, Double.class)
            .put(float.class, Float.class)
            .put(int.class, Integer.class)
            .put(long.class, Long.class)
            .put(short.class, Short.class)
            .put(void.class, Void.class)
            .build();

    private static final Map<JavaType, Class<?>> MESSAGE_TYPE_TO_WRAPPERS
            = new ImmutableMap.Builder<JavaType, Class<?>>()
            .put(JavaType.INT, Integer.class)
            .put(JavaType.LONG, Long.class)
            .put(JavaType.FLOAT, Float.class)
            .put(JavaType.DOUBLE, Double.class)
            .build();

    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Object fromMessageValue(Object value){
        if(value instanceof Descriptors.EnumValueDescriptor){
            value=((Descriptors.EnumValueDescriptor) value).getName();
        }else if(value instanceof ByteString) {
            value = ((ByteString) value).toByteArray();
        }
        return value;
    }
    public static Object fromMessageValue(Object value, Class clz){
        if(value!=null){
            if(clz.isPrimitive()){
                clz=PRIMITIVES_TO_WRAPPERS.get(clz);
            }
            if(value instanceof Descriptors.EnumValueDescriptor){
                value=((Descriptors.EnumValueDescriptor) value).getName();
            }else if(value instanceof ByteString){
                value=((ByteString) value).toByteArray();
            }
            try{
                if(clz.isAssignableFrom(value.getClass())){
                    return value;
                }else if(clz.isEnum()){
                    return Enum.valueOf(clz, (String) value);
                }else if(clz.equals(BigDecimal.class)){
                    return new BigDecimal(value.toString());
                }else if(clz.equals(BigInteger.class)){
                    return new BigInteger(value.toString());
                }else if(value instanceof Number && Number.class.isAssignableFrom(clz)){
                    if(Integer.class.equals(clz)){
                        return ((Number) value).intValue();
                    }else if(Long.class.equals(clz)){
                        return ((Number) value).longValue();
                    }else if(Float.class.equals(clz)){
                        return ((Number) value).floatValue();
                    }else if(Double.class.equals(clz)){
                        return ((Number) value).doubleValue();
                    }
                }else if(clz.equals(LocalDateTime.class)){
                    if(!"".equals(value)) {
                        try {
                            return LocalDateTime.parse((String) value, dateTimeFormatter);
                        } catch (Exception ex) {
                            return LocalDateTime.parse((String) value);
                        }
                    }
                }else if(clz.equals(LocalDate.class)){
                    if(!"".equals(value)){
                        return LocalDate.parse((String) value);
                    }
                }
            }catch (Exception e){
            }
        }
        return null;
    }
    public static Object toMessageValue(Object value, Descriptors.FieldDescriptor fieldDescriptor){
        if(value!=null){
            if(value instanceof Enum){
                value=((Enum) value).name();
            }
            JavaType type=fieldDescriptor.getJavaType();
            Class clz=MESSAGE_TYPE_TO_WRAPPERS.get(type);
            if(clz!=null){
                if(value instanceof Number){
                    if(Integer.class.equals(clz)){
                        return ((Number) value).intValue();
                    }else if(Long.class.equals(clz)){
                        return ((Number) value).longValue();
                    }else if(Float.class.equals(clz)){
                        return ((Number) value).floatValue();
                    }else if(Double.class.equals(clz)){
                        return ((Number) value).doubleValue();
                    }
                }
            }else if(type.equals(JavaType.BOOLEAN)){
                return Boolean.TRUE.equals(value);
            }else if(type.equals(JavaType.STRING)){
                if(value instanceof LocalDateTime){
                    return ((LocalDateTime) value).format(dateTimeFormatter);
                } else {
                    return value.toString();
                }

            }else if(type.equals(JavaType.ENUM)){
                try {
                    if (value instanceof String) {
                        return fieldDescriptor.getEnumType().findValueByName((String) value);
                    } else if (value.getClass().isEnum()) {
                        return fieldDescriptor.getEnumType().findValueByName(((Enum) value).name());
                    }
                }catch (Exception e){

                }
            }else if(type.equals(JavaType.BYTE_STRING) && value instanceof byte[]){
                return ByteString.copyFrom(((byte[]) value));
            }
        }
        return null;
    }
}
