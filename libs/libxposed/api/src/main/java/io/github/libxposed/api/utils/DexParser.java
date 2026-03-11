package io.github.libxposed.api.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Closeable;

/**
 * Xposed interface for parsing dex files.
 */
@SuppressWarnings("unused")
public interface DexParser extends Closeable {
    /**
     * The constant NO_INDEX.
     */
    int NO_INDEX = 0xffffffff;

    /**
     * The interface Array.
     */
    interface Array {
        /**
         * Get values value [ ].
         *
         * @return the value [ ]
         */
        @NonNull
        Value[] getValues();
    }

    /**
     * The interface Annotation.
     */
    interface Annotation {
        /**
         * Gets visibility.
         *
         * @return the visibility
         */
        int getVisibility();

        /**
         * Gets type.
         *
         * @return the type
         */
        @NonNull
        TypeId getType();

        /**
         * Get elements element [ ].
         *
         * @return the element [ ]
         */
        @NonNull
        Element[] getElements();
    }

    /**
     * The interface Value.
     */
    interface Value {

        /**
         * Get value byte [ ].
         *
         * @return the byte [ ]
         */
        @Nullable
        byte[] getValue();

        /**
         * Gets value type.
         *
         * @return the value type
         */
        int getValueType();
    }

    /**
     * The interface Element.
     */
    interface Element extends Value {
        /**
         * Gets name.
         *
         * @return the name
         */
        @NonNull
        StringId getName();
    }
    /**
     * The interface Id.
     */
    interface Id<Self> extends Comparable<Self> {
        /**
         * Gets id.
         *
         * @return the id
         */
        int getId();
    }

    /**
     * The interface Type id.
     */
    interface TypeId extends Id<TypeId> {
        /**
         * Gets descriptor.
         *
         * @return the descriptor
         */
        @NonNull
        StringId getDescriptor();
    }


    /**
     * The interface String id.
     */
    interface StringId extends Id<StringId> {
        /**
         * Gets string.
         *
         * @return the string
         */
        @NonNull
        String getString();
    }

    /**
     * The interface Field id.
     */
    interface FieldId extends Id<FieldId> {
        /**
         * Gets type.
         *
         * @return the type
         */
        @NonNull
        TypeId getType();

        /**
         * Gets declaring class.
         *
         * @return the declaring class
         */
        @NonNull
        TypeId getDeclaringClass();

        /**
         * Gets name.
         *
         * @return the name
         */
        @NonNull
        StringId getName();
    }

    /**
     * The interface Method id.
     */
    interface MethodId extends Id<MethodId> {
        /**
         * Gets declaring class.
         *
         * @return the declaring class
         */
        @NonNull
        TypeId getDeclaringClass();

        /**
         * Gets prototype.
         *
         * @return the prototype
         */
        @NonNull
        ProtoId getPrototype();

        /**
         * Gets name.
         *
         * @return the name
         */
        @NonNull
        StringId getName();
    }

    /**
     * The interface Proto id.
     */
    interface ProtoId extends Id<ProtoId> {
        /**
         * Gets shorty.
         *
         * @return the shorty
         */
        @NonNull
        StringId getShorty();

        /**
         * Gets return type.
         *
         * @return the return type
         */
        @NonNull
        TypeId getReturnType();

        /**
         * Get parameters type id [ ].
         *
         * @return the type id [ ]
         */
        @Nullable
        TypeId[] getParameters();
    }

    /**
     * Get string id string id [ ].
     *
     * @return the string id [ ]
     */
    @NonNull
    StringId[] getStringId();

    /**
     * Get type id type id [ ].
     *
     * @return the type id [ ]
     */
    @NonNull
    TypeId[] getTypeId();

    /**
     * Get field id field id [ ].
     *
     * @return the field id [ ]
     */
    @NonNull
    FieldId[] getFieldId();

    /**
     * Get method id method id [ ].
     *
     * @return the method id [ ]
     */
    @NonNull
    MethodId[] getMethodId();

    /**
     * Get proto id proto id [ ].
     *
     * @return the proto id [ ]
     */
    @NonNull
    ProtoId[] getProtoId();

    /**
     * Get annotations annotation [ ].
     *
     * @return the annotation [ ]
     */
    @NonNull
    Annotation[] getAnnotations();

    /**
     * Get arrays array [ ].
     *
     * @return the array [ ]
     */
    @NonNull
    Array[] getArrays();

    /**
     * The interface Early stop visitor.
     */
    interface EarlyStopVisitor {
        /**
         * Stop boolean.
         *
         * @return the boolean
         */
        boolean stop();
    }

    /**
     * The interface Member visitor.
     */
    interface MemberVisitor extends EarlyStopVisitor {
    }

    /**
     * The interface Class visitor.
     */
    interface ClassVisitor extends EarlyStopVisitor {
        /**
         * Visit member visitor.
         *
         * @param clazz                     the clazz
         * @param accessFlags               the access flags
         * @param superClass                the super class
         * @param interfaces                the interfaces
         * @param sourceFile                the source file
         * @param staticFields              the static fields
         * @param staticFieldsAccessFlags   the static fields access flags
         * @param instanceFields            the instance fields
         * @param instanceFieldsAccessFlags the instance fields access flags
         * @param directMethods             the direct methods
         * @param directMethodsAccessFlags  the direct methods access flags
         * @param virtualMethods            the virtual methods
         * @param virtualMethodsAccessFlags the virtual methods access flags
         * @param annotations               the annotations
         * @return the member visitor
         */
        @Nullable
        MemberVisitor visit(int clazz, int accessFlags, int superClass, @NonNull int[] interfaces, int sourceFile, @NonNull int[] staticFields, @NonNull int[] staticFieldsAccessFlags, @NonNull int[] instanceFields, @NonNull int[] instanceFieldsAccessFlags, @NonNull int[] directMethods, @NonNull int[] directMethodsAccessFlags, @NonNull int[] virtualMethods, @NonNull int[] virtualMethodsAccessFlags, @NonNull int[] annotations);
    }

    /**
     * The interface Field visitor.
     */
    interface FieldVisitor extends MemberVisitor {
        /**
         * Visit.
         *
         * @param field       the field
         * @param accessFlags the access flags
         * @param annotations the annotations
         */
        void visit(int field, int accessFlags, @NonNull int[] annotations);
    }

    /**
     * The interface Method visitor.
     */
    interface MethodVisitor extends MemberVisitor {
        /**
         * Visit method body visitor.
         *
         * @param method               the method
         * @param accessFlags          the access flags
         * @param hasBody              the has body
         * @param annotations          the annotations
         * @param parameterAnnotations the parameter annotations
         * @return the method body visitor
         */
        @Nullable
        MethodBodyVisitor visit(int method, int accessFlags, boolean hasBody, @NonNull int[] annotations, @NonNull int[] parameterAnnotations);
    }

    /**
     * The interface Method body visitor.
     */
    interface MethodBodyVisitor {
        /**
         * Visit.
         *
         * @param method          the method
         * @param accessFlags     the access flags
         * @param referredStrings the referred strings
         * @param invokedMethods  the invoked methods
         * @param accessedFields  the accessed fields
         * @param assignedFields  the assigned fields
         * @param opcodes         the opcodes
         */
        void visit(int method, int accessFlags, @NonNull int[] referredStrings, @NonNull int[] invokedMethods, @NonNull int[] accessedFields, @NonNull int[] assignedFields, @NonNull byte[] opcodes);
    }

    /**
     * Visit defined classes.
     *
     * @param visitor the visitor
     * @throws IllegalStateException the illegal state exception
     */
    void visitDefinedClasses(@NonNull ClassVisitor visitor) throws IllegalStateException;
}
