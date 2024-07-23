package io.github.qauxv.loader.hookapi;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@Keep
public interface ILoaderInfo {

    @NonNull
    String getEntryPointName();

    @NonNull
    String getLoaderVersionName();

    int getLoaderVersionCode();

    /**
     * Get the main module path (loaded target module path).
     *
     * @return The main module path
     */
    @NonNull
    String getMainModulePath();

    void log(@NonNull String msg);

    void log(@NonNull Throwable tr);

    /**
     * Query the extension of the current implementation.
     *
     * @param key  The key of the extension
     * @param args The arguments for the extension, may be empty
     * @return The result of the extension, may be null
     */
    @Nullable
    Object queryExtension(@NonNull String key, @Nullable Object... args);

}
