package io.github.libxposed.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import kotlin.annotation.MustBeDocumented;

/**
 * Indicates this API is only available on Xposed framework API level at least the specified value.
 * <p>
 * This annotation itself is not part of the Xposed frame API.
 */
@MustBeDocumented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
public @interface XposedApiMin {

    int value();
}
