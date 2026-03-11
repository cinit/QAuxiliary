package io.github.libxposed.api.errors;

/**
 * Thrown to indicate that the Xposed framework function is broken.
 */
public class XposedFrameworkError extends Error {

    public XposedFrameworkError(String message) {
        super(message);
    }

    public XposedFrameworkError(String message, Throwable cause) {
        super(message, cause);
    }

    public XposedFrameworkError(Throwable cause) {
        super(cause);
    }
}
