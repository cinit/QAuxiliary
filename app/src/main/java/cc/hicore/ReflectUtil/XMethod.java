package cc.hicore.ReflectUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class XMethod {
    public interface XMethodFilter {
        boolean onMethod(Method m);
    }
    public static XMethodBuilder clz(String name){
        XMethodBuilder newBuilder = new XMethodBuilder();
        newBuilder.clazz(XClass.loadEx(name));
        return newBuilder;
    }
    public static XMethodBuilder clz(Class<?> clz){
        XMethodBuilder newBuilder = new XMethodBuilder();
        newBuilder.clazz(clz);
        return newBuilder;
    }
    public static XMethodBuilder obj(Object obj){
        XMethodBuilder newBuilder = new XMethodBuilder();
        newBuilder.obj(obj);
        return newBuilder;
    }
    public static class XMethodBuilder {

        private Class<?> clz;
        private Object obj;
        private String name;
        private boolean noLoop;
        private boolean ignoreParam;
        private boolean noAbstract;
        private int paramCount = -1;
        private Class<?> retType;
        private final ArrayList<Class<?>> paramTypes = new ArrayList<>();
        private XMethodFilter filter;
        private XMethodBuilder(){ }
        public <T> T invoke(Object... params) throws Exception {
            //fillParam
            if (paramTypes.size() == 0){
                for (Object param : params){
                    Objects.requireNonNull(param,"Can't invoke a method with null param when paramTypes is not set.");
                    paramTypes.add(param.getClass());
                }
            }
            //getMethod
            Method m = get();
            //invoke
            if (Modifier.isStatic(m.getModifiers())){
                m.setAccessible(true);
                return (T) m.invoke(null,params);
            }else {
                Objects.requireNonNull(obj,"Can't invoke a virtual method in a null object.");
                m.setAccessible(true);
                return (T) m.invoke(obj,params);
            }
        }
        public Method get() throws NoSuchMethodException {
            Class<?> findClz = clz;
            if (findClz == null){
                if (obj != null) findClz = obj.getClass();
            }
            Objects.requireNonNull(findClz,"find method class can't be null (name = " + name + ", ret = " + retType + ", params = " + paramTypes);

            Class<?> findClzSave = findClz;
            while (findClz != null){
                Method[] methods = findClz.getDeclaredMethods();

                MethodLoop:
                for (Method m : methods){
                    String m_name = m.getName();
                    Class<?> m_retType = m.getReturnType();
                    Class<?>[] m_paramTypes = m.getParameterTypes();
                    if (noAbstract && Modifier.isAbstract(m.getModifiers()))continue;
                    if (name != null && !m_name.equals(name))continue;
                    if (retType != null && !m_retType.equals(retType))continue;
                    if (!ignoreParam){
                        if (paramCount > -1 && m_paramTypes.length!= paramCount)continue;
                        if (m_paramTypes.length != paramTypes.size())continue;
                        for (int i=0;i<paramTypes.size();i++){
                            if (!m_paramTypes[i].equals(paramTypes.get(i)))continue MethodLoop;
                        }
                    }
                    if (filter != null){
                        if (!filter.onMethod(m))continue;
                    }
                    m.setAccessible(true);
                    return m;
                }
                if (noLoop)break;
                findClz = findClz.getSuperclass();
            }
            throw new NoSuchMethodException("No such method(name="+name+",ret="+retType+",params="+paramTypes+") found in class "+findClzSave);
        }
        public XMethodBuilder name(String name){
            this.name = name;
            return this;
        }
        public XMethodBuilder noLoop(){
            this.noLoop = true;
            return this;
        }
        public XMethodBuilder skipAbstract(){
            this.noAbstract = true;
            return this;
        }
        public XMethodBuilder ret(Class<?> retType){
            this.retType = retType;
            return this;
        }
        public XMethodBuilder paramCount(int count){
            this.paramCount = count;
            return this;
        }
        public XMethodBuilder param(Class<?>... params){
            paramTypes.addAll(Arrays.asList(params));
            return this;
        }
        public XMethodBuilder ignoreParam(){
            this.ignoreParam = true;
            return this;
        }
        private void clazz(Class<?> clz){
            this.clz = clz;
        }
        private void obj(Object obj){
            this.obj = obj;
        }
        public XMethodBuilder filter(XMethodFilter filter){
            this.filter = filter;
            return this;
        }
    }
    public static Method[] getAllMethod(String clzName,String methodName){
        Class<?> clz = XClass.loadEx(clzName);
        ArrayList<Method> methods = new ArrayList<>();
        while (clz != null){
            Method[] ms = clz.getDeclaredMethods();
            for (Method m : ms){
                if (m.getName().equals(methodName)){
                    methods.add(m);
                }
            }
            clz = clz.getSuperclass();
        }
        return methods.toArray(new Method[0]);
    }
    public static Filter_Param ParamFilter(int count,int pos,Class<?> clz){
        return new Filter_Param(count,pos,clz);
    }
    private static class Filter_Param implements XMethodFilter{
        private final int count;
        private final int pos;
        private final Class<?> clz;
        public Filter_Param(int count,int pos,Class<?> clz){
            this.count = count;
            this.pos = pos;
            this.clz = clz;
        }
        @Override
        public boolean onMethod(Method m) {
            return m.getParameterCount() == count && m.getParameterCount() > pos && m.getParameterTypes()[pos].equals(clz);
        }
    }
}
