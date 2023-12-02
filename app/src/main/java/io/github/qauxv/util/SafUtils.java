package io.github.qauxv.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import cc.ioctl.util.ui.FaultyDialog;
import io.github.qauxv.activity.ShadowSafTransientActivity;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;

public class SafUtils {

    private SafUtils() {
        throw new AssertionError("No instance for you!");
    }

    public interface SafSelectFileResultCallback {

        void onResult(@NonNull Uri uri);
    }

    @MainProcess
    public static SaveFileTransaction requestSaveFile(@NonNull Context context) {
        checkProcess();
        return new SaveFileTransaction(context);
    }

    public static class SaveFileTransaction {

        private final Context context;
        private String defaultFileName;
        private String mineType;
        private SafSelectFileResultCallback resultCallback;
        private Runnable cancelCallback;

        private SaveFileTransaction(@NonNull Context context) {
            Objects.requireNonNull(context, "activity");
            this.context = context;
        }

        @NonNull
        public SaveFileTransaction setDefaultFileName(@NonNull String fileName) {
            this.defaultFileName = fileName;
            return this;
        }

        @NonNull
        public SaveFileTransaction setMimeType(@Nullable String mimeType) {
            this.mineType = mimeType;
            return this;
        }

        @NonNull
        public SaveFileTransaction onResult(@NonNull SafSelectFileResultCallback callback) {
            Objects.requireNonNull(callback, "callback");
            this.resultCallback = callback;
            return this;
        }

        @NonNull
        public SaveFileTransaction onCancel(@Nullable Runnable callback) {
            this.cancelCallback = callback;
            return this;
        }

        public void commit() {
            Objects.requireNonNull(context);
            Objects.requireNonNull(resultCallback);
            if (mineType == null) {
                mineType = "application/octet-stream";
            }
            ShadowSafTransientActivity.RequestResultCallback callback = new ShadowSafTransientActivity.RequestResultCallback() {
                @Override
                public void onResult(@Nullable Uri uri) {
                    if (uri != null) {
                        resultCallback.onResult(uri);
                    } else {
                        if (cancelCallback != null) {
                            cancelCallback.run();
                        }
                    }
                }

                @Override
                public void onException(@NonNull Throwable e) {
                    if (e instanceof ActivityNotFoundException) {
                        complainAboutNoSafActivity(context, e);
                    } else {
                        FaultyDialog.show(context, "错误", e);
                    }
                    if (cancelCallback != null) {
                        cancelCallback.run();
                    }
                }
            };
            ShadowSafTransientActivity.startActivityForRequest(context,
                    ShadowSafTransientActivity.TARGET_ACTION_CREATE_AND_WRITE,
                    mineType, defaultFileName, callback);
        }
    }

    @MainProcess
    public static OpenFileTransaction requestOpenFile(@NonNull Context context) {
        checkProcess();
        return new OpenFileTransaction(context);
    }

    public static class OpenFileTransaction {

        private final Context context;
        private String mimeType;
        private SafSelectFileResultCallback resultCallback;
        private Runnable cancelCallback;

        private OpenFileTransaction(@NonNull Context context) {
            Objects.requireNonNull(context, "context");
            this.context = context;
        }

        @NonNull
        public OpenFileTransaction setMimeType(@Nullable String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        @NonNull
        public OpenFileTransaction onResult(@NonNull SafSelectFileResultCallback callback) {
            Objects.requireNonNull(callback, "callback");
            this.resultCallback = callback;
            return this;
        }

        @NonNull
        public OpenFileTransaction onCancel(@Nullable Runnable callback) {
            this.cancelCallback = callback;
            return this;
        }

        public void commit() {
            Objects.requireNonNull(context);
            Objects.requireNonNull(resultCallback);
            ShadowSafTransientActivity.RequestResultCallback callback = new ShadowSafTransientActivity.RequestResultCallback() {
                @Override
                public void onResult(@Nullable Uri uri) {
                    if (uri != null) {
                        resultCallback.onResult(uri);
                    } else {
                        if (cancelCallback != null) {
                            cancelCallback.run();
                        }
                    }
                }

                @Override
                public void onException(@NonNull Throwable e) {
                    if (e instanceof ActivityNotFoundException) {
                        complainAboutNoSafActivity(context, e);
                    } else {
                        FaultyDialog.show(context, "错误", e);
                    }
                    if (cancelCallback != null) {
                        cancelCallback.run();
                    }
                }
            };
            ShadowSafTransientActivity.startActivityForRequest(context,
                    ShadowSafTransientActivity.TARGET_ACTION_READ,
                    mimeType, null, callback);
        }

    }

    @UiThread
    private static void complainAboutNoSafActivity(@NonNull Context context, @NonNull Throwable e) {
        FaultyDialog.show(context, "ActivityNotFoundException",
                "找不到 Intent.ACTION_OPEN_DOCUMENT 或 Intent.ACTION_CREATE_DOCUMENT 的 Activity，可能是系统问题。\n" +
                        "Android 规范要求必须有应用能够处理这两个 Intent，但是有些系统没有实现这个规范。\n" + e);
    }

    private static final HashMap<String, ParcelFileDescriptor> sCachedFileDescriptors = new HashMap<>();

    private static void checkMode(@Nullable String mode) {
        // Can be "r", "w", "wt", "wa", "rw" or "rwt".
        if (!"r".equals(mode) && !"w".equals(mode) && !"wt".equals(mode) && !"wa".equals(mode)
                && !"rw".equals(mode) && !"rwt".equals(mode)) {
            throw new IllegalArgumentException("invalid mode: " + mode);
        }
    }

    /**
     * Open a fd for the given uri. The result is cached. It's the caller's responsibility to close the fd.
     * TODO: add a lifecycle callback to clear the cached fd when the activity is destroyed.
     *
     * @param context the activity or application context
     * @param uri     the uri of the file to be opened
     * @param mode    the mode to open the file
     *                Can be "r", "w", "wt", "wa", "rw" or "rwt".
     *                See {@link ParcelFileDescriptor#parseMode} for more details.
     * @return a fd for the given uri
     * @throws IOException       if any error occurs
     * @throws SecurityException if access denied by the content provider
     */
    public static ParcelFileDescriptor openFileDescriptor(@NonNull Context context, @NonNull Uri uri, @NonNull String mode) throws IOException, SecurityException {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(uri, "uri");
        checkMode(mode);
        try {
            ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor(uri, mode);
            if (fd == null) {
                throw new IOException("Failed to open " + uri + ", is the provider still running?");
            }
            ParcelFileDescriptor dup = fd.dup();
            synchronized (sCachedFileDescriptors) {
                ParcelFileDescriptor old = sCachedFileDescriptors.put(mode + "|" + uri, dup);
                if (old != null) {
                    old.close();
                }
                sCachedFileDescriptors.put(mode + "|" + uri, dup);
            }
            return fd;
        } catch (SecurityException se) {
            // the access is denied, maybe there is an activity recreation?
            // try to open the input stream by a cached file descriptor
            ParcelFileDescriptor dup = null;
            synchronized (sCachedFileDescriptors) {
                ParcelFileDescriptor cached = sCachedFileDescriptors.get(mode + "|" + uri);
                if (cached != null) {
                    dup = cached.dup();
                }
            }
            if (dup != null) {
                try {
                    // set the offset to 0
                    Os.lseek(dup.getFileDescriptor(), 0, OsConstants.SEEK_SET);
                    return dup;
                } catch (ErrnoException e) {
                    dup.close();
                    throw new IOException("Failed to seek to the beginning of the file", e);
                }
            } else {
                // we have no cached file descriptor, re-throw the exception
                throw se;
            }
        }
    }

    public static InputStream openInputStream(@NonNull Context context, @NonNull Uri uri) throws IOException, SecurityException {
        // do not close the fd, it will be closed when the input stream is closed
        return new FileInputStream(openFileDescriptor(context, uri, "r").getFileDescriptor());
    }

    private static void checkProcess() {
        if (HostInfo.isInHostProcess() && !SyncUtils.isMainProcess()) {
            throw new IllegalStateException("This method can only be called in the main process");
        }
    }
}
