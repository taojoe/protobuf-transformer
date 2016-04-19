package com.github.taojoe;

import com.google.protobuf.*;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import org.apache.commons.beanutils.PropertyUtils;
import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by joe on 4/19/16.
 */
public class Transformer {
    protected static class MatchResult{
        public Class destListItemClz;
        public Class destMapValueClz;
        public boolean matchOk=false;
        public boolean copyOk=false;
    }
    protected static MatchResult typeMatch(Descriptors.FieldDescriptor messageFieldDescriptor, Type javaType){
        MatchResult result=new MatchResult();
        JavaType messageJavaType=messageFieldDescriptor.getJavaType();
        ParameterizedType javaPType=null;
        if(javaType instanceof ParameterizedType){
            javaPType=(ParameterizedType) javaType;
        }
        if(messageFieldDescriptor.isRepeated()){
            if(messageFieldDescriptor.isMapField()){
                if(javaPType.getRawType()== Map.class && javaPType.getActualTypeArguments().length==2){
                    JavaType keyJavaType=null;
                    if(messageFieldDescriptor.getMessageType()!=null){
                        List<Descriptors.FieldDescriptor> tmp=messageFieldDescriptor.getMessageType().getFields();
                        if(tmp!=null && tmp.size()==2)
                            keyJavaType=tmp.get(0).getJavaType();
                    }
                    boolean isKeyMatch=false;
                    if(keyJavaType!=null) {
                        Type keyType = javaPType.getActualTypeArguments()[0];
                        isKeyMatch = (keyJavaType.equals(JavaType.STRING) && keyType instanceof Class && keyType == String.class) ||
                                (keyJavaType.equals(JavaType.INT) && (keyType.getTypeName().equals("int") || (keyType instanceof Class && keyType == Integer.class))) ||
                                (keyJavaType.equals(JavaType.LONG) && (keyType.getTypeName().equals("long") || (keyType instanceof Class && keyType == Long.class)));
                    }
                    if(!isKeyMatch)
                        return result;
                    javaType=javaPType.getActualTypeArguments()[1];
                    if(messageJavaType.equals(JavaType.MESSAGE))
                        result.destMapValueClz =(Class)javaType;
                }else {
                    return result;
                }
            }else if(javaPType.getRawType()== List.class && javaPType.getActualTypeArguments().length==1){
                javaType=javaPType.getActualTypeArguments()[0];
                result.destListItemClz =(Class)javaType;
            }else {
                return result;
            }
        }
        if(!messageFieldDescriptor.isRepeated() && javaPType!=null){
            return result;
        }
        result.matchOk=
                (messageJavaType.equals(JavaType.MESSAGE) && javaType instanceof Class && (!((Class) javaType).isPrimitive()))||
                        (messageJavaType.equals(JavaType.STRING) && javaType instanceof Class && javaType==String.class) ||
                        (messageJavaType.equals(JavaType.BOOLEAN) && (javaType.getTypeName().equals("boolean") || (javaType instanceof Class && javaType==Boolean.class)))||
                        (messageJavaType.equals(JavaType.INT) && (javaType.getTypeName().equals("int") || (javaType instanceof Class && javaType==Integer.class)))||
                        (messageJavaType.equals(JavaType.LONG) && (javaType.getTypeName().equals("long") || (javaType instanceof Class && javaType==Long.class)))||
                        (messageJavaType.equals(JavaType.FLOAT) && (javaType.getTypeName().equals("float") || (javaType instanceof Class && javaType==Float.class)))||
                        (messageJavaType.equals(JavaType.DOUBLE) && (javaType.getTypeName().equals("double") || (javaType instanceof Class && javaType==Double.class)))
        ;
        if(!result.matchOk){
            return result;
        }
        if(messageJavaType.equals(JavaType.MESSAGE)){
            result.destListItemClz=(Class) javaType;
        }
        result.copyOk= messageJavaType.equals(JavaType.STRING)||
                messageJavaType.equals(JavaType.BOOLEAN)||
                messageJavaType.equals(JavaType.INT)||
                messageJavaType.equals(JavaType.LONG)||
                messageJavaType.equals(JavaType.FLOAT)||
                messageJavaType.equals(JavaType.DOUBLE);
        result.copyOk =result.matchOk && result.copyOk && !messageFieldDescriptor.isMapField();
        return result;
    }
    public static void copyFromMessage(Object obj, MessageOrBuilder message){
        Class clz=obj.getClass();
        Map<Descriptors.FieldDescriptor, Object> md=message.getAllFields();
        for (Map.Entry<Descriptors.FieldDescriptor, Object> kv : md.entrySet()) {
            Descriptors.FieldDescriptor messageFieldDescriptor=kv.getKey();
            String name=messageFieldDescriptor.getName();
            Object value=kv.getValue();
            if(value!=null){
                MatchResult matchResult=null;
                Field field=null;
                PropertyDescriptor descriptor= null;
                //获取field 和matchResult
                try {
                    field=clz.getDeclaredField(name);
                    if(Modifier.isPublic(field.getModifiers())){
                        matchResult=typeMatch(messageFieldDescriptor, field.getGenericType());
                    }else
                        field=null;
                } catch (NoSuchFieldException e) {
                }
                //获取property和matchResult
                if(matchResult==null && PropertyUtils.isWriteable(obj, name)){
                    try {
                        descriptor = PropertyUtils.getPropertyDescriptor(obj,name);
                        if(descriptor.getReadMethod()!=null && descriptor.getWriteMethod()!=null){
                            matchResult=typeMatch(messageFieldDescriptor, descriptor.getReadMethod().getGenericReturnType());
                        }
                    } catch (IllegalAccessException e) {
                    } catch (InvocationTargetException e) {
                    } catch (NoSuchMethodException e) {
                    }
                }
                if(matchResult!=null && matchResult.matchOk){
                    if(!matchResult.copyOk){
                        if(messageFieldDescriptor.isRepeated()){
                            if(messageFieldDescriptor.isMapField()){
                                Map<Object, Object> destMap=new HashMap<>();
                                try {
                                    List<com.google.protobuf.MapEntry<Object, MessageOrBuilder>> origList=(List)value;
                                    for(com.google.protobuf.MapEntry<Object, MessageOrBuilder> entry: origList){
                                        if(matchResult.destMapValueClz!=null){
                                            Object destValue=matchResult.destMapValueClz.newInstance();
                                            copyFromMessage(destValue, entry.getValue());
                                            destMap.put(entry.getKey(), destValue);
                                        }else{
                                            destMap.put(entry.getKey(), entry.getValue());
                                        }
                                    }
                                } catch (InstantiationException e) {
                                    e.printStackTrace();
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                                if(destMap.size()>0){
                                    value=destMap;
                                }else{
                                    value=null;
                                }
                            }else{
                                ArrayList<Object> destList=new ArrayList<>();
                                try {
                                    List<MessageOrBuilder> origList=(List) value;
                                    for (MessageOrBuilder org : origList) {
                                        Object destItem=matchResult.destListItemClz.newInstance();
                                        copyFromMessage(destItem, org);
                                        destList.add(destItem);
                                    }
                                } catch (InstantiationException e) {
                                    e.printStackTrace();
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                                if(destList.size()>0){
                                    value=destList;
                                }else {
                                    value=null;
                                }
                            }
                        }else{
                            Object dest=null;
                            try {
                                if(value instanceof MessageOrBuilder){
                                    dest=matchResult.destListItemClz.newInstance();
                                    copyFromMessage(dest, (MessageOrBuilder)value);
                                }
                            } catch (InstantiationException e) {
                            } catch (IllegalAccessException e) {
                            }
                            value=dest;
                        }
                    }
                    if(value!=null){
                        if(field!=null){
                            try {
                                field.set(obj, value);
                            } catch (IllegalAccessException e) {
                            }
                        }else if(descriptor!=null){
                            try {
                                PropertyUtils.setSimpleProperty(obj, name, value);
                            } catch (IllegalAccessException e) {
                            } catch (InvocationTargetException e) {
                            } catch (NoSuchMethodException e) {
                            }
                        }
                    }
                }
            }
        }
    }
    public static <T extends Message.Builder> T writeToMessage(Object obj, T builder){
        Class clz=obj.getClass();
        List<Descriptors.FieldDescriptor> fields=builder.getDescriptorForType().getFields();
        for (Descriptors.FieldDescriptor messageFieldDescriptor: fields) {
            String name=messageFieldDescriptor.getName();
            MatchResult matchResult=null;
            PropertyDescriptor descriptor= null;
            Object value=null;
            //获取value 和matchResult
            try {
                Field field=clz.getDeclaredField(name);
                if(Modifier.isPublic(field.getModifiers())){
                    matchResult=typeMatch(messageFieldDescriptor, field.getGenericType());
                    if(matchResult.matchOk){
                        value=field.get(obj);
                    }
                }
            } catch (NoSuchFieldException e) {
            } catch (IllegalAccessException e) {
            }
            //获取value和matchResult
            if(matchResult==null && PropertyUtils.isReadable(obj, name)){
                try {
                    descriptor = PropertyUtils.getPropertyDescriptor(obj,name);
                    matchResult=typeMatch(messageFieldDescriptor, descriptor.getReadMethod().getGenericReturnType());
                    if(matchResult.matchOk){
                        value=PropertyUtils.getSimpleProperty(obj, name);
                    }
                } catch (IllegalAccessException e) {
                } catch (InvocationTargetException e) {
                } catch (NoSuchMethodException e) {
                }
            }
            if(matchResult!=null && matchResult.matchOk && value!=null){
                if(matchResult.copyOk){
                    builder.setField(messageFieldDescriptor, value);
                }else{
                    if(messageFieldDescriptor.isRepeated()){
                        if(messageFieldDescriptor.isMapField()){
                            if(value instanceof Map){
                                Descriptors.FieldDescriptor entryValueFieldDescriptor=null;
                                if(messageFieldDescriptor.getMessageType()!=null){
                                    List<Descriptors.FieldDescriptor> entryFields=messageFieldDescriptor.getMessageType().getFields();
                                    if(entryFields!=null && entryFields.size()==2)
                                        entryValueFieldDescriptor=entryFields.get(1);
                                }
                                for(Map.Entry<Object, Object> entry:((Map<Object, Object>) value).entrySet()){
                                    Message.Builder tmpBuilder=builder.newBuilderForField(messageFieldDescriptor);
                                    if(tmpBuilder instanceof MapEntry.Builder){
                                        MapEntry.Builder entryBuilder=(MapEntry.Builder) tmpBuilder;
                                        entryBuilder.setKey(entry.getKey());
                                        if(entryValueFieldDescriptor.getJavaType().equals(JavaType.MESSAGE)){
                                            entryBuilder.setValue(writeToMessage(entry.getValue(), entryBuilder.newBuilderForField(entryValueFieldDescriptor)).build());
                                        }else{
                                            entryBuilder.setValue(entry.getValue());
                                        }
                                        builder.addRepeatedField(messageFieldDescriptor, entryBuilder.build());
                                    }
                                    System.out.println(tmpBuilder);
                                }
                            }
                        }else{
                            if(value instanceof List){
                                for (Object tmp : (List) value) {
                                    Message.Builder tmpBuilder=builder.newBuilderForField(messageFieldDescriptor);
                                    writeToMessage(tmp, tmpBuilder);
                                    builder.addRepeatedField(messageFieldDescriptor, tmpBuilder.build());
                                }
                            }
                        }
                    }else{
                        Message.Builder tmpBuilder=builder.newBuilderForField(messageFieldDescriptor);
                        builder.setField(messageFieldDescriptor,
                                writeToMessage(value, tmpBuilder).build());
                    }
                }
            }
        }
        return builder;
    }
}
