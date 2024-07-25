package io.github.qauxv.loader.hookapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface IClassLoaderHelper {

    /**
     * Create a new instance of {@link ClassLoader} with the specified parent class loader. The class loader is empty on creation.
     * <p>
     * This instance of {@link ClassLoader} can be later used with {@link #injectDexToClassLoader(ClassLoader, byte[], String)}.
     *
     * @param parent The parent class loader
     * @return The new instance of {@link ClassLoader}
     * @throws UnsupportedOperationException If the operation is not supported on current platform
     */
    @NonNull
    ClassLoader createEmptyInMemoryMultiDexClassLoader(@NonNull ClassLoader parent) throws UnsupportedOperationException;

    /**
     * Inject a dex file to the specified class loader. The class loader must be created by {@link #createEmptyInMemoryMultiDexClassLoader(ClassLoader)}.
     *
     * @param classLoader The class loader, returned by {@link #createEmptyInMemoryMultiDexClassLoader(ClassLoader)}
     * @param dexBytes    The dex file bytes
     * @param dexName     The optional name of the dex file, for debugging purpose only, may be null
     * @throws IllegalArgumentException      If the class loader is not created by {@link #createEmptyInMemoryMultiDexClassLoader(ClassLoader)}, or the dex file
     *                                       bytes is invalid.
     * @throws UnsupportedOperationException If the operation is not supported on current platform
     */
    void injectDexToClassLoader(@NonNull ClassLoader classLoader, @NonNull byte[] dexBytes, @Nullable String dexName)
            throws IllegalArgumentException, UnsupportedOperationException;

}
