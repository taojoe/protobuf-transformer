package com.github.taojoe.helper;

/**
 * Created by joe on 8/22/16.
 */
public interface PropertyWriter {
    /* 写入object*/
    void write(Object value) throws IllegalAccessException;
    /* 当遇到 value 对应的是 message类型的时候, 需要创建 一个实例 与message 对应*/
    Object valueInstance() throws InstantiationException;
    /* 当遇到 value 是个普通的类型, 调用此方法进行类型的转换*/
    Object valueInstance(Object object);
    /* 当遇到property 对应的是一个Map或者List的时候,就需要获取子项的instance 与map或list中子项(类型为message) 对应 */
    Object itemInstance() throws InstantiationException;
    /* 当遇到property 对应的是一个Map或者List的时候, 对应的子项value 是个普通类型,调用此方法进行类型转换*/
    Object itemInstance(Object object);
}
