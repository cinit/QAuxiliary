/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */
package io.github.qauxv.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dalvik.annotation.optimization.FastNative;
import io.github.qauxv.util.dexkit.DexMethodDescriptor;
import io.github.qauxv.util.xpcompat.ArrayUtils;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

public class Natives {

    public static final int RTLD_LAZY = 0x00001;    /* Lazy function call binding.  */
    public static final int RTLD_NOW = 0x00002;    /* Immediate function call binding.  */
    public static final int RTLD_BINDING_MASK = 0x3;    /* Mask of binding time value.  */
    public static final int RTLD_NOLOAD = 0x00004;    /* Do not load the object.  */
    public static final int RTLD_DEEPBIND = 0x00008;    /* Use deep binding.  */
    /* If the following bit is set in the MODE argument to `dlopen',
       the symbols of the loaded object and its dependencies are made
       visible as if the object were linked directly into the program.  */
    public static final int RTLD_GLOBAL = 0x00100;
    /* Unix98 demands the following flag which is the inverse to RTLD_GLOBAL.
       The implementation does this by default and so we can define the
       value to zero.  */
    public static final int RTLD_LOCAL = 0;

    /* Do not delete object when closed.  */
    public static final int RTLD_NODELETE = 0x01000;

    public static final int PROT_READ = 0x1;        /* Page can be read.  */
    public static final int PROT_WRITE = 0x2;        /* Page can be written.  */
    public static final int PROT_EXEC = 0x4;        /* Page can be executed.  */
    public static final int PROT_NONE = 0x0;        /* Page can not be accessed.  */
    public static final int PROT_GROWSDOWN = 0x01000000;	/* Extend change to start of
					   growsdown vma (mprotect only).  */
    public static final int PROT_GROWSUP = 0x02000000;	/* Extend change to start of
					   growsup vma (mprotect only).  */

    public static final int SEEK_SET = 0;        /* Set file offset to OFFSET.  */
    public static final int SEEK_CUR = 1;        /* Set file offset to current plus OFFSET.  */
    public static final int SEEK_END = 2;        /* Set file offset to end plus OFFSET.  */

    public static final int O_ACCMODE = 0x3;
    public static final int O_RDONLY = 0x0;
    public static final int O_WRONLY = 0x1;
    public static final int O_RDWR = 0x2;
    public static final int O_CREAT = 0x40;
    public static final int O_EXCL = 0x80;
    public static final int O_NOCTTY = 0x100;
    public static final int O_TRUNC = 0x200;
    public static final int O_APPEND = 0x400;
    public static final int O_NONBLOCK = 0x800;
    public static final int O_DSYNC = 0x1000;
    public static final int O_DIRECT = 0x4000;
    public static final int O_LARGEFILE = 0x8000;
    public static final int O_DIRECTORY = 0x10000;
    public static final int O_NOFOLLOW = 0x20000;
    public static final int O_NOATIME = 0x40000;
    public static final int O_CLOEXEC = 0x80000;
    public static final int O_SYNC = (0x100000 | O_DSYNC);
    public static final int O_PATH = 0x200000;

    private Natives() {
        throw new AssertionError("No instance for you!");
    }

    public static native void mwrite(long ptr, int len, byte[] buf, int offset);

    public static void mwrite(long ptr, int len, byte[] buf) {
        mwrite(ptr, len, buf, 0);
    }

    public static native void mread(long ptr, int len, byte[] buf, int offset);

    public static void mread(long ptr, int len, byte[] buf) {
        mread(ptr, len, buf, 0);
    }

    public static native int write(int fd, byte[] buf, int offset, int len) throws IOException;

    public static int write(int fd, byte[] buf, int len) throws IOException {
        return write(fd, buf, 0, len);
    }

    public static native int read(int fd, byte[] buf, int offset, int len) throws IOException;

    public static int read(int fd, byte[] buf, int len) throws IOException {
        return read(fd, buf, 0, len);
    }

    public static native int dup(int fd) throws IOException;

    public static native int dup2(int oldfd, int newfd) throws IOException;

    public static native int dup3(int oldfd, int newfd, int flags) throws IOException;

    public static native int open(String path, int flags, int mode) throws IOException;

    public static native long lseek(int fd, long offset, int whence) throws IOException;

    public static native void close(int fd) throws IOException;

    public static native long malloc(int size);

    public static native void free(long ptr);

    public static native void memcpy(long dest, long src, int num);

    public static native void memset(long addr, int c, int num);

    public static native int mprotect(long addr, int len, int prot);

    public static native long dlsym(long ptr, String symbol);

    public static native long dlopen(String filename, int flag);

    public static native int dlclose(long ptr);

    public static native String dlerror();

    public static native int sizeofptr();

    public static native int getpagesize();

    public static native long call(long addr);

    public static native long call(long addr, long argv);

    public static native int getProcessDumpableState() throws IOException;

    public static native void setProcessDumpableState(int dumpable) throws IOException;

    /**
     * Allocate a object instance of the specified class without calling the constructor.
     * <p>
     * Do not use this directly, use {@link cc.ioctl.util.Reflex#allocateInstance(Class)} instead.
     *
     * @param clazz the class to allocate
     * @return the allocated object
     */
    public static native Object allocateInstanceImpl(Class<?> clazz);

    /**
     * Invoke an instance method non-virtually (i.e. without calling the overridden method).
     * <p>
     * Do not use this directly, use {@link cc.ioctl.util.Reflex#invokeNonVirtual(Object, Method, Object[])} instead.
     *
     * @param declaringClass the class of the method, e.g. "Ljava/lang/String;"
     * @param methodName     the method name
     * @param methodSig      the method signature, e.g. "(Ljava/lang/String;)Ljava/lang/String;"
     * @param obj            the object to invoke the method on, must not be null
     * @param args           the arguments to pass to the method, may be null if no arguments are passed
     * @return the return value of the method
     * @throws InvocationTargetException if the method threw an exception
     */
    public static native Object invokeNonVirtualImpl(Class<?> declaringClass, String methodName,
            String methodSig, Object obj, Object[] args)
            throws InvocationTargetException;

    // If the method signature does not match the actual method signature, the behavior is undefined, eg, ART runtime aborts.
    private static native Object invokeNonVirtualArtMethodImpl(@NonNull Member member, @NonNull String signature, @NonNull Class<?> klass, boolean isStatic,
            @Nullable Object obj, @NonNull Object[] args) throws InvocationTargetException;

    @FastNative
    public static native Member getReflectedMethod(@NonNull Class<?> cls, @NonNull String name, @NonNull String sig, boolean isStatic) throws NoSuchMethodError;

    /**
     * Invoke an instance method non-virtually, no CHA lookup is performed. No declaring class check is performed.
     * <p>
     * Caller is responsible for checking that the method declaration class matches the receiver object (aka "this").
     *
     * @param member         the method or constructor to invoke, method may be static or non-static method
     * @param declaringClass the "effective" declaring class of the method
     * @param obj            the object to invoke the method on, may be null if the method is static
     * @param args           the arguments to pass to the method. may be null if no arguments are passed
     * @return the return value of the method
     * @throws InvocationTargetException if the method threw an exception
     */
    public static Object invokeNonVirtualArtMethodNoDeclaringClassCheck(@NonNull Member member, @NonNull Class<?> declaringClass,
            @Nullable Object obj, @Nullable Object[] args) throws InvocationTargetException {
        Objects.requireNonNull(member, "member must not be null");
        Objects.requireNonNull(declaringClass, "declaringClass must not be null");
        if (args == null) {
            args = ArrayUtils.EMPTY_OBJECT_ARRAY;
        }
        // perform some basic checks
        if (obj != null) {
            if (!declaringClass.isInstance(obj)) {
                throw new IllegalArgumentException("object class mismatch, expected " + declaringClass + ", got " + obj.getClass());
            }
        }
        if (member instanceof Method) {
            Method method = (Method) member;
            if (method.getParameterTypes().length != args.length) {
                throw new IllegalArgumentException("args length mismatch, expected " + method.getParameterTypes().length + ", got " + args.length);
            }
            // abstract method is not allowed
            if ((method.getModifiers() & (Modifier.ABSTRACT)) != 0) {
                throw new IllegalArgumentException("abstract method is not allowed");
            }
            boolean isStatic = Modifier.isStatic(method.getModifiers());
            String signature = DexMethodDescriptor.getMethodTypeSig(method);
            return invokeNonVirtualArtMethodImpl(member, signature, declaringClass, isStatic, obj, args);
        } else if (member instanceof Constructor) {
            Constructor<?> constructor = (Constructor<?>) member;
            if (constructor.getParameterTypes().length != args.length) {
                throw new IllegalArgumentException("args length mismatch, expected " + constructor.getParameterTypes().length + ", got " + args.length);
            }
            String signature = DexMethodDescriptor.getConstructorTypeSig(constructor);
            return invokeNonVirtualArtMethodImpl(member, signature, declaringClass, false, obj, args);
        } else {
            throw new IllegalArgumentException("member must be a method or constructor");
        }
    }

}
