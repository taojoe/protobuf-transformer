package com.github.taojoe.helper;

import com.github.taojoe.core.CoreValueCoerce;
import org.apache.commons.beanutils.PropertyUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.util.List;
import java.util.Map;

/**
 * Created by joe on 8/23/16.
 */
public class BeanObjectHelper implements ObjectHelper {
    protected static String secondaryName(String name){
        return name.replaceAll("([A-Z][a-z]+)", "_$1").toLowerCase();
    }
    Field getFieldOrNull(Class clz, String name){
        try {
            Field field = clz.getDeclaredField(name);
            if(Modifier.isPublic(field.getModifiers())){
                if(!field.isAccessible()) {
                    field.setAccessible(true);
                }
                return field;
            }
        } catch (NoSuchFieldException e) {
        }
        return null;
    }
    PropertyDescriptor getReadPropertyOrNull(Object bean, String name){
        try {
            PropertyDescriptor descriptor = PropertyUtils.getPropertyDescriptor(bean, name);
            if(descriptor!=null && descriptor.getReadMethod()!=null){
                return  descriptor;
            }
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        } catch (NoSuchMethodException e) {
        }
        return null;
    }
    PropertyDescriptor getWritePropertyOrNull(Object bean, String name){
        try {
            PropertyDescriptor descriptor = PropertyUtils.getPropertyDescriptor(bean, name);
            if(descriptor!=null && descriptor.getWriteMethod()!=null){
                return  descriptor;
            }
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        } catch (NoSuchMethodException e) {
        }
        return null;
    }
    protected static class BeanPropertyReader implements PropertyReader{
        Object bean;
        Field field;
        PropertyDescriptor propertyDescriptor;

        public BeanPropertyReader(Object bean, Field field, PropertyDescriptor propertyDescriptor) {
            this.bean = bean;
            this.field = field;
            this.propertyDescriptor = propertyDescriptor;
        }

        @Override
        public Object get() {
            try {
                return field!=null ? field.get(bean):
                        propertyDescriptor!=null ? propertyDescriptor.getReadMethod().invoke(bean):null;
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
            return null;
        }
    }
    protected static class BeanPropertyWriter implements PropertyWriter{
        Object bean;
        Field field;
        PropertyDescriptor propertyDescriptor;

        public BeanPropertyWriter(Object bean, Field field, PropertyDescriptor propertyDescriptor) {
            this.bean = bean;
            this.field = field;
            this.propertyDescriptor = propertyDescriptor;
        }

        @Override
        public void write(Object value) throws IllegalAccessException {
            try {
                if (field != null) {
                    field.set(bean, value);
                } else if (propertyDescriptor != null) {
                    propertyDescriptor.getWriteMethod().invoke(bean, value);
                }
            }catch (IllegalAccessException e){
            } catch (InvocationTargetException e) {
            }
        }
        Type valueType(){
            return field!=null? field.getGenericType():
                    propertyDescriptor!=null? propertyDescriptor.getWriteMethod().getGenericParameterTypes()[0]:null;
        }

        @Override
        public Object valueInstance() throws InstantiationException {
            Type type=valueType();
            if(type instanceof Class){
                try {
                    return ((Class) type).newInstance();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        public Object valueInstance(Object object) {
            Type type=valueType();
            if(type instanceof Class){
                return CoreValueCoerce.fromMessageValue(object, (Class)valueType());
            }
            return null;
        }

        Class itemClass(){
            Type type=valueType();
            if(type instanceof ParameterizedType){
                ParameterizedType type1=(ParameterizedType) type;
                if(type1.getRawType()== Map.class && type1.getActualTypeArguments().length==2 && type1.getActualTypeArguments()[1] instanceof Class){
                    return (Class)type1.getActualTypeArguments()[1];
                }else if(type1.getRawType()== List.class && type1.getActualTypeArguments().length==1 && type1.getActualTypeArguments()[0] instanceof Class){
                    return (Class)type1.getActualTypeArguments()[0];
                }
            }
            return null;
        }

        @Override
        public Object itemInstance() throws InstantiationException {
            Class clz=itemClass();
            if(clz!=null){
                try {
                    return clz.newInstance();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        public Object itemInstance(Object object) {
            Class clz=itemClass();
            if(clz!=null){
                return CoreValueCoerce.fromMessageValue(object, clz);
            }
            return null;
        }
    }
    @Override
    public PropertyReader getPropertyReader(Object object, String name) {
        Field field=getFieldOrNull(object.getClass(), name);
        field=field!=null ?field:getFieldOrNull(object.getClass(), secondaryName(name));
        PropertyDescriptor propertyDescriptor=getReadPropertyOrNull(object, name);
        propertyDescriptor=propertyDescriptor!=null? propertyDescriptor: getReadPropertyOrNull(object, secondaryName(name));
        return new BeanPropertyReader(object, field, propertyDescriptor);
    }

    @Override
    public PropertyWriter getPropertyWriter(Object object, String name) {
        Field field=getFieldOrNull(object.getClass(), name);
        field=field!=null ?field:getFieldOrNull(object.getClass(), secondaryName(name));
        PropertyDescriptor propertyDescriptor=getWritePropertyOrNull(object, name);
        propertyDescriptor=propertyDescriptor!=null? propertyDescriptor: getWritePropertyOrNull(object, secondaryName(name));
        if(field!=null || propertyDescriptor!=null){
            return new BeanPropertyWriter(object, field, propertyDescriptor);
        }
        return null;
    }
}
