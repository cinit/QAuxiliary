package io.github.qauxv.base.annotation;

import io.github.qauxv.base.IDynamicHook;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denoting a hook(has nothing to do with the UI).
 * <p>
 * It's should be a Kotlin object(or a Java class with a public static final INSTANCE field).
 * <p>
 * Target should be an instance of {@link IDynamicHook}.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface FunctionHookEntry {

}
