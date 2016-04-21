package com.github.taojoe;

import com.github.taojoe.core.BeanUtil;
import com.google.protobuf.Descriptors;
import com.google.protobuf.MapEntry;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by joe on 4/20/16.
 */
public class Transformer {
    public <T> T messageToJava(MessageOrBuilder message, Class<T> clz){
        Map<Descriptors.FieldDescriptor, Object> md=message.getAllFields();
        try {
            T target=clz.newInstance();
            for(Map.Entry<Descriptors.FieldDescriptor, Object> kv : md.entrySet()) {
                Descriptors.FieldDescriptor fieldDescriptor = kv.getKey();
                String name = fieldDescriptor.getName();
                Object oldValue = kv.getValue();
                Object newValue=null;
                BeanUtil.FieldOrProperty objField=BeanUtil.fieldOrProperty(target, name);
                if(objField!=null){
                    boolean isValueMessage=fieldDescriptor.getJavaType().equals(Descriptors.FieldDescriptor.JavaType.MESSAGE);
                    if(fieldDescriptor.isMapField()){
                        Map<Object, Object> destMap=new HashMap<>();
                        List<MapEntry<Object, MessageOrBuilder>> origList=(List)oldValue;
                        for(com.google.protobuf.MapEntry<Object, MessageOrBuilder> entry: origList){
                            Object mapValue=null;
                            if(isValueMessage){
                                mapValue=messageToJava(entry.getValue(), objField.typeDescriptor.valueClz);
                            }else{
                                mapValue=entry.getValue();
                            }
                            destMap.put(entry.getKey(), mapValue);
                        }
                        newValue=destMap;
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
                                destList.add(obj);
                            }
                        }
                        newValue=destList;
                    }else {
                        if(isValueMessage){
                            newValue=messageToJava((MessageOrBuilder) oldValue, objField.typeDescriptor.valueClz);
                        }else{
                            newValue=oldValue;
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
            BeanUtil.FieldOrProperty objField=BeanUtil.fieldOrProperty(bean, name);
            if(objField!=null){
                Object oldValue=objField.getValue(bean);
                if(oldValue!=null){
                    boolean isValueMessage=fieldDescriptor.getJavaType().equals(Descriptors.FieldDescriptor.JavaType.MESSAGE);
                    if(fieldDescriptor.isMapField()){
                        if(oldValue instanceof Map){
                            //fieldDescriptor 在map的时候描述的是entry, 因此需要进一步获取到entry value的fieldDescriptor
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
                                            entryBuilder.setValue(entry.getValue());
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
                                    builder.addRepeatedField(fieldDescriptor, tmp);
                                }
                            }
                        }
                    }else {
                        if(isValueMessage){
                            Message.Builder tmpBuilder=builder.newBuilderForField(fieldDescriptor);
                            builder.setField(fieldDescriptor, javaToMessage(oldValue, tmpBuilder).build());
                        }else{
                            builder.setField(fieldDescriptor, oldValue);
                        }
                    }
                }
            }
        }
        return builder;
    }
}
