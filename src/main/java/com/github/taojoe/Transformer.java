package com.github.taojoe;

import com.github.taojoe.core.BeanUtil;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.MapEntry;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.ByteString;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by joe on 4/20/16.
 */
public class Transformer {
    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    protected Object javaValueToMessageValue(Object value, Descriptors.FieldDescriptor fieldDescriptor){
        JavaType type=fieldDescriptor.getJavaType();
        if(type.equals(JavaType.BOOLEAN)){
            return value;
        }else if(type.equals(JavaType.INT) ||type.equals(JavaType.LONG)){
            if(value instanceof BigInteger){
                if(type.equals(JavaType.INT)){
                    return ((BigInteger) value).intValue();
                }else{
                    return ((BigInteger) value).longValue();
                }
            }
            return value;
        }else if(type.equals(JavaType.FLOAT) || type.equals(JavaType.DOUBLE)){
            if(value instanceof BigDecimal){
                if(type.equals(JavaType.FLOAT)){
                    return ((BigDecimal) value).floatValue();
                }else{
                    return ((BigDecimal) value).doubleValue();
                }
            }
            return value;
        }else if(type.equals(JavaType.STRING)){
            if(value instanceof LocalDateTime){
                return ((LocalDateTime) value).format(dateTimeFormatter);
            } else {
                return value.toString();
            }

        }else if(type.equals(JavaType.ENUM)){
            if(value instanceof String) {
                return fieldDescriptor.getEnumType().findValueByName((String) value);
            }else if(value.getClass().isEnum()){
                return fieldDescriptor.getEnumType().findValueByName(((Enum) value).name());
            }
        }else if(type.equals(JavaType.BYTE_STRING)){
            return ByteString.copyFrom(((byte[]) value));
        }
        return null;
    }
    protected Object massageValueToJavaValue(Object value, Class clz){
        String clzName=clz.getName();
        if(clz.equals(Integer.class) || clz.equals(Long.class) || clz.equals(Float.class) || clz.equals(Double.class) ||clz.equals(Boolean.class) ||
                clz.equals(int.class) || clz.equals(long.class) || clz.equals(float.class) || clz.equals(double.class) || clz.equals(boolean.class)){
            // 判断clz时,如果是boolean, 估计是直接返回的boolean type,因此同时包含类型的判断
            return value;
        }else if(clz.equals(String.class)){
            if(value instanceof String){
                return value;
            }else if(value instanceof Descriptors.EnumDescriptor){
                return ((Descriptors.EnumDescriptor)value).getName();
            }
        }else if(clz.isEnum()){
            if(value instanceof String){
                return Enum.valueOf(clz, (String) value);
            }else if(value instanceof Descriptors.EnumValueDescriptor){
                return Enum.valueOf(clz, ((Descriptors.EnumValueDescriptor) value).getName() );
            }
        }else if(clz.equals(BigDecimal.class)){
            return new BigDecimal((double)value);
        }else if(clz.equals(LocalDateTime.class)){
            try{
                return LocalDateTime.parse((String) value, dateTimeFormatter);
            } catch (Exception ex){
                return LocalDateTime.parse((String) value);
            }

        }else if(clz.equals(LocalDate.class)){
            return LocalDate.parse((String) value);
        }else if(clz.equals(byte[].class)){
            return ((ByteString) value).toByteArray();
        }
        return null;
    }
    public <T> T messageToJava(MessageOrBuilder message, Class<T> clz){
        List<Descriptors.FieldDescriptor> fields=message.getDescriptorForType().getFields();
        try {
            T target=clz.newInstance();
            for(Descriptors.FieldDescriptor fieldDescriptor:fields){
                boolean hasValue=false;
                if(fieldDescriptor.isRepeated()){
                    hasValue=message.getRepeatedFieldCount(fieldDescriptor)>0;
                }else{
                    hasValue=message.hasField(fieldDescriptor) || fieldDescriptor.getJavaType().equals(JavaType.ENUM);
                }
                if(!hasValue){
                    continue;
                }
                Object oldValue=message.getField(fieldDescriptor);
                String name = fieldDescriptor.getName();
                Object newValue=null;
                BeanUtil.FieldOrProperty objField=BeanUtil.fieldOrProperty(target, name, false, true);
                if(objField!=null && objField.typeDescriptor.mustValueClz()){
                    boolean isValueMessage=fieldDescriptor.getJavaType().equals(Descriptors.FieldDescriptor.JavaType.MESSAGE);
                    if(fieldDescriptor.isMapField()){
                        //fieldDescriptor 在map的时候描述的是entry, entry 本身是message类型, 因此需要进一步获取到entry value的fieldDescriptor
                        if(fieldDescriptor.getMessageType()!=null) {
                            List<Descriptors.FieldDescriptor> entryFields = fieldDescriptor.getMessageType().getFields();
                            if (entryFields != null && entryFields.size() == 2) {
                                Descriptors.FieldDescriptor valueFieldDescriptor = entryFields.get(1);
                                isValueMessage = valueFieldDescriptor.getJavaType().equals(Descriptors.FieldDescriptor.JavaType.MESSAGE);
                                Map<Object, Object> destMap=new HashMap<>();
                                List<MapEntry<Object, MessageOrBuilder>> origList=(List)oldValue;
                                for(com.google.protobuf.MapEntry<Object, MessageOrBuilder> entry: origList){
                                    Object mapValue=null;
                                    if(isValueMessage){
                                        mapValue=messageToJava(entry.getValue(), objField.typeDescriptor.valueClz);
                                    }else{
                                        mapValue=massageValueToJavaValue(entry.getValue(), objField.typeDescriptor.valueClz);
                                    }
                                    destMap.put(entry.getKey(), mapValue);
                                }
                                newValue=destMap;
                            }
                        }

                    }else if(fieldDescriptor.isRepeated()){
                        ArrayList<Object> destList=new ArrayList<>();
                        if(isValueMessage){
                            List<MessageOrBuilder> origList=(List) oldValue;
                            for (MessageOrBuilder org : origList) {
                                destList.add(messageToJava(org, objField.typeDescriptor.valueClz));
                            }
                        }else{
                            List<Object> origList=(List) oldValue;
                            for(Object obj:origList){
                                destList.add(massageValueToJavaValue(obj, objField.typeDescriptor.valueClz));
                            }
                        }
                        newValue=destList;
                    }else {
                        if(isValueMessage){
                            newValue=messageToJava((MessageOrBuilder) oldValue, objField.typeDescriptor.valueClz);
                        }else{
                            newValue=massageValueToJavaValue(oldValue, objField.typeDescriptor.valueClz);
                        }
                    }
                    if(newValue!=null){
                        objField.setValue(target, newValue);
                    }
                }
            }
            return target;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
    public <T extends Message.Builder> T javaToMessage(Object bean, T builder){
        Class clz=bean.getClass();
        List<Descriptors.FieldDescriptor> fields=builder.getDescriptorForType().getFields();
        for (Descriptors.FieldDescriptor fieldDescriptor: fields) {
            String name=fieldDescriptor.getName();
            BeanUtil.FieldOrProperty objField=BeanUtil.fieldOrProperty(bean, name, true, false);
            if(objField!=null){
                Object oldValue=objField.getValue(bean);
                if(oldValue!=null){
                    boolean isValueMessage=fieldDescriptor.getJavaType().equals(Descriptors.FieldDescriptor.JavaType.MESSAGE);
                    if(fieldDescriptor.isMapField()){
                        if(oldValue instanceof Map){
                            //fieldDescriptor 在map的时候描述的是entry, entry 本身是message类型, 因此需要进一步获取到entry value的fieldDescriptor
                            if(fieldDescriptor.getMessageType()!=null){
                                List<Descriptors.FieldDescriptor> entryFields=fieldDescriptor.getMessageType().getFields();
                                if(entryFields!=null && entryFields.size()==2) {
                                    Descriptors.FieldDescriptor valueFieldDescriptor = entryFields.get(1);
                                    isValueMessage=valueFieldDescriptor.getJavaType().equals(Descriptors.FieldDescriptor.JavaType.MESSAGE);
                                    for(Map.Entry<Object, Object> entry:((Map<Object, Object>) oldValue).entrySet()){
                                        Message.Builder tmpBuilder=builder.newBuilderForField(fieldDescriptor);
                                        MapEntry.Builder entryBuilder=(MapEntry.Builder) tmpBuilder;
                                        entryBuilder.setKey(entry.getKey());
                                        if(isValueMessage){
                                            entryBuilder.setValue(javaToMessage(entry.getValue(), entryBuilder.newBuilderForField(valueFieldDescriptor)).build());
                                        }else{
                                            entryBuilder.setValue(javaValueToMessageValue(entry.getValue(), valueFieldDescriptor));
                                        }
                                    }
                                }
                            }
                        }
                    }else if(fieldDescriptor.isRepeated()){
                        if(oldValue instanceof List){
                            if(isValueMessage){
                                for(Object tmp:(List) oldValue){
                                    Message.Builder tmpBuilder=builder.newBuilderForField(fieldDescriptor);
                                    builder.addRepeatedField(fieldDescriptor, javaToMessage(tmp, tmpBuilder).build());
                                }
                            }else{
                                for(Object tmp:(List) oldValue){
                                    builder.addRepeatedField(fieldDescriptor, javaValueToMessageValue(tmp, fieldDescriptor));
                                }
                            }
                        }
                    }else {
                        if(isValueMessage){
                            Message.Builder tmpBuilder=builder.newBuilderForField(fieldDescriptor);
                            builder.setField(fieldDescriptor, javaToMessage(oldValue, tmpBuilder).build());
                        }else{
                            builder.setField(fieldDescriptor, javaValueToMessageValue(oldValue, fieldDescriptor));
                        }
                    }
                }
            }
        }
        return builder;
    }
    public Object getValueByFieldNameOrNull(MessageOrBuilder message, String fieldName, Object defaultValue){
        Descriptors.FieldDescriptor field=message.getDescriptorForType().findFieldByName(fieldName);
        if(field!=null && message.hasField(field)){
            return message.getField(field);
        }
        return defaultValue;
    }
    public Object getValueByFieldNameOrNull(MessageOrBuilder message, String fieldName){
        return getValueByFieldNameOrNull(message, fieldName, null);
    }
}
