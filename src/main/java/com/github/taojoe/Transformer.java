package com.github.taojoe;

import com.github.taojoe.core.BeanUtil;
import com.github.taojoe.core.CoreValueCoerce;
import com.github.taojoe.helper.*;
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
import java.time.LocalTime;
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
            try {
                if (value instanceof String) {
                    return fieldDescriptor.getEnumType().findValueByName((String) value);
                } else if (value.getClass().isEnum()) {
                    return fieldDescriptor.getEnumType().findValueByName(((Enum) value).name());
                }
            }catch (Exception e){}
        }else if(type.equals(JavaType.BYTE_STRING)){
            return ByteString.copyFrom(((byte[]) value));
        }
        return null;
    }
    protected Object massageValueToJavaValue(Object value, Class clz){
        if(clz.equals(Integer.class) || clz.equals(Long.class) || clz.equals(Float.class) || clz.equals(Double.class) ||clz.equals(Boolean.class) ||
                clz.equals(int.class) || clz.equals(long.class) || clz.equals(float.class) || clz.equals(double.class) || clz.equals(boolean.class)){
            // 判断clz时,如果是boolean, 估计是直接返回的boolean type,因此同时包含类型的判断
            return value;
        }else if(clz.equals(String.class)){
            if(value instanceof String){
                return value;
            }else if(value instanceof Descriptors.EnumDescriptor){
                return ((Descriptors.EnumDescriptor)value).getName();
            }else if(value instanceof Descriptors.EnumValueDescriptor){
                return ((Descriptors.EnumValueDescriptor) value).getName();
            }
        }else if(clz.isEnum()){
            try{
                if(value instanceof String){
                    return Enum.valueOf(clz, (String) value);
                }else if(value instanceof Descriptors.EnumValueDescriptor){
                    return Enum.valueOf(clz, ((Descriptors.EnumValueDescriptor) value).getName() );
                }
            }catch (Exception e){
                return null;
            }

        }else if(clz.equals(BigDecimal.class)){
            return new BigDecimal((double)value);
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
        }else if(clz.equals(LocalTime.class)){
            if(!"".equals(value)){
                return LocalTime.parse((String) value);
            }
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
                    hasValue=!fieldDescriptor.getJavaType().equals(JavaType.MESSAGE) || message.hasField(fieldDescriptor);
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
        if(bean==null){
            return builder;
        }
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
                            Object newValue=javaValueToMessageValue(oldValue, fieldDescriptor);
                            if(newValue!=null){
                                builder.setField(fieldDescriptor, newValue);
                            }
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
    //-----
    protected Object toMessageValue(Object value, Descriptors.FieldDescriptor fieldDescriptor){
        return CoreValueCoerce.toMessageValue(value, fieldDescriptor);
    }
    protected <T extends Message.Builder> T objectToMessage(ObjectHelper helper, Object object, T builder){
        if(object!=null) {
            List<Descriptors.FieldDescriptor> fields = builder.getDescriptorForType().getFields();
            for (Descriptors.FieldDescriptor fieldDescriptor : fields) {
                PropertyReader propertyReader= helper.getPropertyReader(object, fieldDescriptor.getName());
                Object oldValue=propertyReader!=null? propertyReader.get():null;
                if (oldValue != null) {
                    boolean isValueMessage = fieldDescriptor.getJavaType().equals(Descriptors.FieldDescriptor.JavaType.MESSAGE);
                    if (fieldDescriptor.isMapField()) {
                        if (oldValue instanceof Map) {
                            //fieldDescriptor 在map的时候描述的是entry, entry 本身是message类型, 因此需要进一步获取到entry value的fieldDescriptor
                            if (fieldDescriptor.getMessageType() != null) {
                                List<Descriptors.FieldDescriptor> entryFields = fieldDescriptor.getMessageType().getFields();
                                if (entryFields != null && entryFields.size() == 2) {
                                    Descriptors.FieldDescriptor valueFieldDescriptor = entryFields.get(1);
                                    isValueMessage = valueFieldDescriptor.getJavaType().equals(Descriptors.FieldDescriptor.JavaType.MESSAGE);
                                    for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) oldValue).entrySet()) {
                                        Message.Builder tmpBuilder = builder.newBuilderForField(fieldDescriptor);
                                        MapEntry.Builder entryBuilder = (MapEntry.Builder) tmpBuilder;
                                        entryBuilder.setKey(entry.getKey());
                                        if (isValueMessage) {
                                            entryBuilder.setValue(objectToMessage(helper, entry.getValue(), entryBuilder.newBuilderForField(valueFieldDescriptor)).build());
                                        } else {
                                            entryBuilder.setValue(toMessageValue(entry.getValue(), valueFieldDescriptor));
                                        }
                                    }
                                }
                            }
                        }
                    } else if (fieldDescriptor.isRepeated()) {
                        if (oldValue instanceof List) {
                            if (isValueMessage) {
                                for (Object tmp : (List) oldValue) {
                                    Message.Builder tmpBuilder = builder.newBuilderForField(fieldDescriptor);
                                    builder.addRepeatedField(fieldDescriptor, objectToMessage(helper, tmp, tmpBuilder).build());
                                }
                            } else {
                                for (Object tmp : (List) oldValue) {
                                    builder.addRepeatedField(fieldDescriptor, toMessageValue(tmp, fieldDescriptor));
                                }
                            }
                        }
                    } else {
                        if (isValueMessage) {
                            Message.Builder tmpBuilder = builder.newBuilderForField(fieldDescriptor);
                            builder.setField(fieldDescriptor, objectToMessage(helper, oldValue, tmpBuilder).build());
                        } else {
                            Object newValue=toMessageValue(oldValue, fieldDescriptor);
                            if (newValue != null) {
                                builder.setField(fieldDescriptor, newValue);
                            }
                        }
                    }
                }
            }
        }
        return builder;
    }
    protected <T> T messageToObject(ObjectHelper helper, MessageOrBuilder message, T object){
        if(object!=null) {
            List<Descriptors.FieldDescriptor> fields = message.getDescriptorForType().getFields();
            try {
                for (Descriptors.FieldDescriptor fieldDescriptor : fields) {
                    boolean hasValue = false;
                    if (fieldDescriptor.isRepeated()) {
                        hasValue = message.getRepeatedFieldCount(fieldDescriptor) > 0;
                    } else {
                        hasValue = !fieldDescriptor.getJavaType().equals(Descriptors.FieldDescriptor.JavaType.MESSAGE) || message.hasField(fieldDescriptor);
                    }
                    if (!hasValue) {
                        continue;
                    }
                    Object oldValue = message.getField(fieldDescriptor);
                    PropertyWriter propertyWriter=helper.getPropertyWriter(object, fieldDescriptor.getName());
                    if (propertyWriter!=null) {
                        Object newValue = null;
                        boolean isValueMessage = fieldDescriptor.getJavaType().equals(Descriptors.FieldDescriptor.JavaType.MESSAGE);
                        if (fieldDescriptor.isMapField()) {
                            //fieldDescriptor 在map的时候描述的是entry, entry 本身是message类型, 因此需要进一步获取到entry value的fieldDescriptor
                            if (fieldDescriptor.getMessageType() != null) {
                                List<Descriptors.FieldDescriptor> entryFields = fieldDescriptor.getMessageType().getFields();
                                if (entryFields != null && entryFields.size() == 2) {
                                    Descriptors.FieldDescriptor valueFieldDescriptor = entryFields.get(1);
                                    isValueMessage = valueFieldDescriptor.getJavaType().equals(Descriptors.FieldDescriptor.JavaType.MESSAGE);
                                    Map<Object, Object> destMap = new HashMap<>();
                                    List<MapEntry<Object, MessageOrBuilder>> origList = (List) oldValue;
                                    for (com.google.protobuf.MapEntry<Object, MessageOrBuilder> entry : origList) {
                                        Object mapValue = null;
                                        if (isValueMessage) {
                                            mapValue = messageToObject(helper, entry.getValue(), propertyWriter.itemInstance());
                                        } else {
                                            mapValue = propertyWriter.itemInstance(entry.getValue());
                                        }
                                        destMap.put(entry.getKey(), mapValue);
                                    }
                                    newValue = destMap;
                                }
                            }

                        } else if (fieldDescriptor.isRepeated()) {
                            ArrayList<Object> destList = new ArrayList<>();
                            if (isValueMessage) {
                                List<MessageOrBuilder> origList = (List) oldValue;
                                for (MessageOrBuilder org : origList) {
                                    destList.add(messageToObject(helper, org, propertyWriter.itemInstance()));
                                }
                            } else {
                                List<Object> origList = (List) oldValue;
                                for (Object obj : origList) {
                                    destList.add(propertyWriter.itemInstance(obj));
                                }
                            }
                            newValue = destList;
                        } else {
                            if (isValueMessage) {
                                newValue = messageToObject(helper, (MessageOrBuilder) oldValue, propertyWriter.valueInstance());
                            } else {
                                newValue = propertyWriter.valueInstance(oldValue);
                            }
                        }
                        if (newValue != null) {
                            propertyWriter.write(newValue);
                        }
                    }
                }
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return object;
    }
    public Map messageToMap(MessageOrBuilder message, Map map){
        return messageToObject(new MapObjectHelper(), message, map);
    }
    public Map messageToMap(MessageOrBuilder message){
        return messageToObject(new MapObjectHelper(), message, new HashMap());
    }
    public <T extends Message.Builder> T mapToMessage(Map map, T builder){
        return objectToMessage(new MapObjectHelper(), map, builder);
    }
    public <T> T messageToBean(MessageOrBuilder message, Class<T> clz){
        try {
            return messageToObject(new BeanObjectHelper(), message, clz.newInstance());
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {}
        return null;
    }
    public <T extends Message.Builder> T beanToMessage(Object bean, T builder){
        return objectToMessage(new BeanObjectHelper(), bean, builder);
    }
}
