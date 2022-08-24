package io.github.qauxv.util;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.github.qauxv.activity.ShadowSafTransientActivity;
import java.io.FileDescriptor;
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
            ShadowSafTransientActivity.startActivityForRequest(context,
                    ShadowSafTransientActivity.TARGET_ACTION_CREATE_AND_WRITE,
                    mineType, defaultFileName, uri -> {
                        if (uri != null) {
                            resultCallback.onResult(uri);
                        } else {
                            if (cancelCallback != null) {
                                cancelCallback.run();
                            }
                        }
                    });
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
            ShadowSafTransientActivity.startActivityForRequest(context,
                    ShadowSafTransientActivity.TARGET_ACTION_READ,
                    mimeType, null, uri -> {
                        if (uri != null) {
                            resultCallback.onResult(uri);
                        } else {
                            if (cancelCallback != null) {
                                cancelCallback.run();
                            }
                        }
                    });
        }

    }

    private static final HashMap<String, ParcelFileDescriptor> sCachedFileDescriptors = new HashMap<>();

    /**
     * Open an input stream for the given uri. The result is cached.
     * TODO: add a lifecycle callback to clear the cached fd when the activity is destroyed.
     *
     * @param context the activity or application context
     * @param uri     the uri of the file to be opened
     * @return the input stream of the file
     * @throws IOException       if any error occurs
     * @throws SecurityException if access denied by the content provider
     */
    public static InputStream openInputStream(@NonNull Context context, @NonNull Uri uri) throws IOException, SecurityException {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(uri, "uri");
        try {
            ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor(uri, "r");
            if (fd == null) {
                throw new IOException("Failed to open " + uri + ", is the provider still running?");
            }
            ParcelFileDescriptor dup = fd.dup();
            synchronized (sCachedFileDescriptors) {
                ParcelFileDescriptor old = sCachedFileDescriptors.put(uri.toString(), dup);
                if (old != null) {
                    old.close();
                }
                sCachedFileDescriptors.put(uri.toString(), dup);
            }
            // do not close the fd, it will be closed when the input stream is closed
            return new FileInputStream(fd.getFileDescriptor());
        } catch (SecurityException se) {
            // the access is denied, maybe there is an activity recreation?
            // try to open the input stream by a cached file descriptor
            ParcelFileDescriptor dup = null;
            synchronized (sCachedFileDescriptors) {
                ParcelFileDescriptor cached = sCachedFileDescriptors.get(uri.toString());
                if (cached != null) {
                    dup = cached.dup();
                }
            }
            if (dup != null) {
                FileDescriptor fd = dup.getFileDescriptor();
                try {
                    // set the offset to 0
                    Os.lseek(fd, 0, OsConstants.SEEK_SET);
                    // do not close the fd, it will be closed when the input stream is closed
                    return new FileInputStream(fd);
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

    private static void checkProcess() {
        if (HostInfo.isInHostProcess() && !SyncUtils.isMainProcess()) {
            throw new IllegalStateException("This method can only be called in the main process");
        }
    }
}
