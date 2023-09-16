package cc.hicore.ReflectUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

public class XField {
    public interface XFieldFilter<TYPE> {
        boolean onField(Field field, TYPE value);
    }
    public static void getStaticFields(Class<?> clz, XFieldFilter filter){
        Field[] fs = clz.getDeclaredFields();
        for (Field f : fs){
            f.setAccessible(true);
            if (Modifier.isStatic(f.getModifiers())){
                try {
                    Object value = f.get(null);
                    filter.onField(f,value);
                } catch (Exception ignored) { }
            }
        }
    }
    public static void getObjFields(Object obj, XFieldFilter filter){
        Class<?> clz = obj.getClass();
        Field[] fs = clz.getDeclaredFields();
        for (Field f : fs){
            f.setAccessible(true);
            try {
                Object value = f.get(obj);
                filter.onField(f,value);
            } catch (Exception ignored) { }
        }
    }
    public static void getObjFields2(Object obj,Class<?>clz, XFieldFilter filter){
        Field[] fs = clz.getDeclaredFields();
        for (Field f : fs){
            f.setAccessible(true);
            try {
                Object value = f.get(obj);
                filter.onField(f,value);
            } catch (Exception ignored) { }
        }
    }
    public static XFieldBuilder clz(String name){
        XFieldBuilder newBuilder = new XFieldBuilder();
        return newBuilder.clazz(XClass.loadEx(name));
    }
    public static XFieldBuilder clz(Class<?> clz){
        XFieldBuilder newBuilder = new XFieldBuilder();
        return newBuilder.clazz(clz);
    }
    public static XFieldBuilder obj(Object obj){
        XFieldBuilder newBuilder = new XFieldBuilder();
        return newBuilder.obj(obj);
    }
    public static class XFieldBuilder{
        private String name;
        private Class<?> type;
        private boolean isStrictMode;
        private Object obj;
        private Class<?> objClass;
        private XFieldFilter filter;
        private boolean onlyStatic;
        private XFieldBuilder(){

        }
        public XFieldBuilder name(String name){
            this.name = name;
            return this;
        }
        public XFieldBuilder type(Class<?> type){
            this.type = type;
            return this;
        }
        public XFieldBuilder clazz(Class<?> clazz){
            this.objClass = clazz;
            return this;
        }
        public <FilterTYPE> XFieldBuilder filter(XFieldFilter<FilterTYPE> filter){
            this.filter = filter;
            return this;
        }
        public XFieldBuilder obj(Object obj){
            this.obj = obj;
            return this;
        }
        public XFieldBuilder onlyStatic(boolean b){
            this.onlyStatic = b;
            return this;
        }
        public XFieldBuilder strict(boolean b){
            this.isStrictMode = b;
            return this;
        }
        public <T> T get(){
            return doFind();
        }
        public <T> void set(T value) throws IllegalAccessException {
            Class<?> findClazz = this.objClass;
            if (findClazz == null){
                findClazz = this.obj == null ? null : this.obj.getClass();
            }
            if (obj == null)onlyStatic = true;
            Objects.requireNonNull(findClazz,"find clz can't be null.(name = " + name + ", type = " + type + ", strict = " + isStrictMode + ")");
            Class<?> saveSourceClass = findClazz;
            while (findClazz != null){
                Field[] fields = findClazz.getDeclaredFields();
                for (Field f : fields){
                    //比对基本信息
                    Class<?> f_type = f.getType();
                    String f_name = f.getName();
                    if (onlyStatic && !Modifier.isStatic(f.getModifiers()))continue;
                    if (name != null && !f_name.equals(name))continue;
                    if (type != null){
                        if (isStrictMode){
                            if (!f_type.equals(type))continue;
                        }else {
                            if (!type.isAssignableFrom(f_type)){
                                if (!f_type.equals(type))continue;
                            }
                        }
                    }
                    f.setAccessible(true);
                    //获取信息
                    Object fieldValue = null;
                    try {
                        if (Modifier.isStatic(f.getModifiers())){
                            fieldValue = f.get(null);
                        }else {
                            if (obj != null){
                                fieldValue = f.get(obj);
                            }
                        }
                    }catch (Exception ignored){ }

                    //调用筛选器
                    if (filter != null){
                        if (!filter.onField(f, fieldValue))continue;
                    }

                    if (Modifier.isStatic(f.getModifiers())){
                        f.set(null,value);
                    }else {
                        f.set(obj,value);
                    }
                    return;
                }
                findClazz = findClazz.getSuperclass();
            }
            throw new RuntimeException("No such field(name="+name+",type="+type+",class="+saveSourceClass+",strict="+isStrictMode+") found.");
        }
        public <T> T doFind(){
            Class<?> findClazz = this.objClass;
            if (findClazz == null){
                findClazz = this.obj == null ? null : this.obj.getClass();
            }
            if (obj == null)onlyStatic = true;
            Objects.requireNonNull(findClazz,"find clz can't be null.(name = " + name + ",type = " + type + ",strict = " + isStrictMode + ")");
            Class<?> saveSourceClass = findClazz;
            while (findClazz != null){
                Field[] fields = findClazz.getDeclaredFields();
                for (Field f : fields){
                    //比对基本信息
                    Class<?> f_type = f.getType();
                    String f_name = f.getName();
                    if (onlyStatic && !Modifier.isStatic(f.getModifiers()))continue;
                    if (name != null && !f_name.equals(name))continue;
                    if (type != null){
                        if (isStrictMode){
                            if (!f_type.equals(type))continue;
                        }else {
                            if (!type.isAssignableFrom(f_type)){
                                if (!f_type.equals(type))continue;
                            }
                        }
                    }
                    f.setAccessible(true);
                    //获取信息
                    Object fieldValue = null;
                    try {
                        if (Modifier.isStatic(f.getModifiers())){
                            fieldValue = f.get(null);
                        }else {
                            if (obj != null){
                                fieldValue = f.get(obj);
                            }
                        }
                    }catch (Exception ignored){ }

                    //调用筛选器
                    if (filter != null){
                        if (!filter.onField(f,fieldValue))continue;
                    }
                    return (T) fieldValue;
                }
                findClazz = findClazz.getSuperclass();
            }
            throw new RuntimeException("No such field(name="+name+",type="+type+",class="+saveSourceClass+",strict="+isStrictMode+") found.");
        }

    }
}
