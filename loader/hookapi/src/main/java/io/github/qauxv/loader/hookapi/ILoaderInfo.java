package io.github.qauxv.loader.hookapi;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

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

}
