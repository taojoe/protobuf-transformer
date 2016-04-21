package com.github.taojoe.core;

import org.apache.commons.beanutils.PropertyUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.util.List;
import java.util.Map;

/**
 * Created by joe on 4/20/16.
 */
public class BeanUtil {
    protected static String secondaryName(String name){
        return name.replaceAll("([A-Z][a-z]+)", "_$1").toLowerCase();
    }
    public static class TypeDescriptor{
        public Class valueClz;
        public boolean isList;
        public boolean isMap;
        public Class keyClz;

        public TypeDescriptor(Type type) {
            if(type instanceof Class){
                this.valueClz=(Class) type;
            }else if(type instanceof ParameterizedType){
                ParameterizedType type1=(ParameterizedType) type;
                Type tmp=null;
                if(type1.getRawType()== Map.class && type1.getActualTypeArguments().length==2){
                    isMap=true;
                    tmp=type1.getActualTypeArguments()[0];
                    if(tmp instanceof Class){
                        keyClz=(Class) tmp;
                    }
                    tmp=type1.getActualTypeArguments()[1];
                    if(tmp instanceof Class){
                        valueClz=(Class) tmp;
                    }
                }else if(type1.getRawType()== List.class && type1.getActualTypeArguments().length==1){
                    isList=true;
                    tmp=type1.getActualTypeArguments()[0];
                    if(tmp instanceof Class){
                        valueClz=(Class) tmp;
                    }
                }
            }
            if(this.valueClz==null){
                throw new RuntimeException("不能识别的type");
            }
        }
    }
    public static class FieldOrProperty {
        public Field field;
        public PropertyDescriptor property;
        public TypeDescriptor typeDescriptor;

        public FieldOrProperty(Field field, PropertyDescriptor property) {
            this.field = field;
            this.property = property;
            if(field!=null){
                if(!field.isAccessible()) {
                    field.setAccessible(true);
                }
                typeDescriptor=new TypeDescriptor(field.getGenericType());
            }else if(property!=null){
                typeDescriptor=new TypeDescriptor(property.getReadMethod().getGenericReturnType());
            }
        }

        public void setValue(Object bean, Object value){
            if(field!=null){
                try {
                    field.set(bean, value);
                } catch (IllegalAccessException e) {
//                    e.printStackTrace();
                }
            }else if(property!=null){
                try {
                    PropertyUtils.setSimpleProperty(bean, property.getName(), value);
                } catch (IllegalAccessException e) {
//                    e.printStackTrace();
                } catch (InvocationTargetException e) {
//                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
//                    e.printStackTrace();
                }
            }
        }
        public Object getValue(Object bean){
            if(field!=null){
                try {
                    return field.get(bean);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }else if(property!=null){
                try {
                    return PropertyUtils.getSimpleProperty(bean, property.getName());
                } catch (IllegalAccessException e) {
//                    e.printStackTrace();
                } catch (InvocationTargetException e) {
//                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
//                    e.printStackTrace();
                }
            }
            return null;
        }
    }
    protected static FieldOrProperty simpleFieldOrProperty(Object obj, String name){
        try {
            Field field = obj.getClass().getDeclaredField(name);
            if(Modifier.isPublic(field.getModifiers())){
                return new FieldOrProperty(field, null);
            }
        } catch (NoSuchFieldException e) {
//            e.printStackTrace();
        }
        try {
            PropertyDescriptor descriptor = PropertyUtils.getPropertyDescriptor(obj, name);
            if(descriptor!=null && descriptor.getWriteMethod()!=null && descriptor.getReadMethod()!=null){
                return new FieldOrProperty(null, descriptor);
            }
        } catch (IllegalAccessException e) {
//            e.printStackTrace();
        } catch (InvocationTargetException e) {
//            e.printStackTrace();
        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
        }
        return null;
    }
    public static FieldOrProperty fieldOrProperty(Object obj, String name){
        FieldOrProperty ret=simpleFieldOrProperty(obj, name);
        if(ret==null){
            String name2=secondaryName(name);
            if(!name2.equals(name)){
                ret=simpleFieldOrProperty(obj, name2);
            }
        }
        return ret;
    }
}
