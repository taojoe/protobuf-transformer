package com.github.taojoe.helper;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by joe on 8/23/16.
 */
public class MapObjectHelper implements ObjectHelper {
    protected static class MapPropertyReader implements PropertyReader{
        Map map;
        String name;

        public MapPropertyReader(Map map, String name) {
            this.map = map;
            this.name = name;
        }

        @Override
        public Object get() {
            return map!=null ? map.get(name):null;
        }
    }
    protected static class MapPropertyWriter implements PropertyWriter{
        Map map;
        String name;

        public MapPropertyWriter(Map map, String name) {
            this.map = map;
            this.name = name;
        }

        @Override
        public void write(Object value) throws IllegalAccessException {
            map.put(name, value);
        }

        @Override
        public Object valueInstance() throws InstantiationException {
            return new HashMap<>();
        }

        @Override
        public Object valueInstance(Object object) {
            return object;
        }

        @Override
        public Object itemInstance() throws InstantiationException {
            return new HashMap<>();
        }

        @Override
        public Object itemInstance(Object object) {
            return object;
        }
    }
    @Override
    public PropertyReader getPropertyReader(Object object, String name) {
        return new MapPropertyReader(object instanceof Map ? (Map) object: null, name);
    }

    @Override
    public PropertyWriter getPropertyWriter(Object object, String name) {
        if(object instanceof Map){
            return new MapPropertyWriter((Map)object, name);
        }
        return null;
    }
}
