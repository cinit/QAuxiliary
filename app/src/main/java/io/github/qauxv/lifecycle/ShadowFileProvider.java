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

package io.github.qauxv.lifecycle;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedBridge;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.SyncUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class ShadowFileProvider {

    private static final String[] COLUMNS = {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
    private static final String DISPLAYNAME_FIELD = "displayName";

    // format content://authority/qauxv/tmp/$uuid/$display_name

    public static Cursor doOnQuery(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                                   @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        // ContentProvider has already checked granted permissions
        // Log.d("doQuery: " + uri);
        String displayName = uri.getQueryParameter(DISPLAYNAME_FIELD);
        if (projection == null) {
            projection = COLUMNS;
        }
        String[] path = getUriPathHierarchy(uri);

        String name = path[path.length - 1];
        long size = -1;

        if (path.length == 4 && path[0].equals("qauxv") && path[1].equals("tmp")) {
            String uuid = path[2];
            IShadowTmpFileProvider provider = getShadowTmpFileProvider();
            if (provider != null) {
                try {
                    name = provider.getTmpFileName(uuid);
                    size = provider.getTmpFileSize(uuid);
                } catch (RemoteException e) {
                    Log.e(e);
                }
            }
        } else if (path.length == 5 && path[0].equals("qauxv") && path[1].equals("pidfd")) {
            int pid = -1;
            int fd = -1;
            try {
                pid = Integer.parseInt(path[2]);
                fd = Integer.parseInt(path[3]);
            } catch (NumberFormatException ignored) {
            }
            File file = new File("/proc/" + pid + "/fd/" + fd);
            if (file.exists()) {
                size = file.length();
            }
        }

        String[] cols = new String[projection.length];
        Object[] values = new Object[projection.length];
        int i = 0;
        for (String col : projection) {
            if (OpenableColumns.DISPLAY_NAME.equals(col)) {
                cols[i] = OpenableColumns.DISPLAY_NAME;
                values[i++] = (displayName == null) ? name : displayName;
            } else if (OpenableColumns.SIZE.equals(col)) {
                cols[i] = OpenableColumns.SIZE;
                values[i++] = size;
            }
        }

        cols = copyOf(cols, i);
        values = copyOf(values, i);

        final MatrixCursor cursor = new MatrixCursor(cols, 1);
        cursor.addRow(values);
        return cursor;
    }

    private static String[] copyOf(String[] original, int newLength) {
        final String[] result = new String[newLength];
        System.arraycopy(original, 0, result, 0, newLength);
        return result;
    }

    private static Object[] copyOf(Object[] original, int newLength) {
        final Object[] result = new Object[newLength];
        System.arraycopy(original, 0, result, 0, newLength);
        return result;
    }

    private static String[] getUriPathHierarchy(@NonNull Uri uri) {
        String[] pathList = uri.getEncodedPath().split("/");
        if (pathList.length > 0 && pathList[0].length() == 0) {
            pathList = Arrays.copyOfRange(pathList, 1, pathList.length);
        }
        return pathList;
    }

    public static ParcelFileDescriptor doOnOpenFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        // ContentProvider has already checked granted permissions
        // Log.d("doOpenFile: " + uri);
        String[] path = getUriPathHierarchy(uri);
        if (path.length == 4 && "qauxv".equals(path[0]) && "tmp".equals(path[1])) {
            String uuid = path[2];
            IShadowTmpFileProvider provider = getShadowTmpFileProvider();
            if (provider != null) {
                try {
                    ParcelFileDescriptor pfd = provider.getTmpFileDescriptor(uuid);
                    if (pfd != null) {
                        return pfd;
                    } else {
                        throw new FileNotFoundException("No such file: " + uri);
                    }
                } catch (IOException | RemoteException e) {
                    throw new FileNotFoundException(e.getMessage());
                }
            } else {
                throw new FileNotFoundException("remote dead");
            }
        }
        throw new FileNotFoundException("internal error");
    }

    private static int modeToMode(String mode) {
        int modeBits;
        if ("r".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_ONLY;
        } else if ("w".equals(mode) || "wt".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else if ("wa".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_APPEND;
        } else if ("rw".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                    | ParcelFileDescriptor.MODE_CREATE;
        } else if ("rwt".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else {
            throw new IllegalArgumentException("Invalid mode: " + mode);
        }
        return modeBits;
    }

    private static boolean sIsHooked = false;

    public static void initHookForFileProvider() throws ReflectiveOperationException {
        if (sIsHooked) {
            return;
        }
        ClassLoader classLoader = Initiator.getHostClassLoader();
        Class<?> supportFileProvider = null;
        Class<?> androidxFileProvider = null;
        try {
            supportFileProvider = classLoader.loadClass("android.support.v4.content.FileProvider");
        } catch (ClassNotFoundException e) {
        }
        try {
            androidxFileProvider = classLoader.loadClass("androidx.core.content.FileProvider");
        } catch (ClassNotFoundException e) {
        }
        Method supportOpenFile = null;
        Method supportQuery = null;
        Method androidxOpenFile = null;
        Method androidxQuery = null;
        if (supportFileProvider != null) {
            supportOpenFile = supportFileProvider.getDeclaredMethod("openFile", Uri.class, String.class);
            supportQuery = supportFileProvider.getDeclaredMethod("query", Uri.class, String[].class, String.class, String[].class, String.class);
        }
        if (androidxFileProvider != null) {
            androidxOpenFile = androidxFileProvider.getDeclaredMethod("openFile", Uri.class, String.class);
            androidxQuery = androidxFileProvider.getDeclaredMethod("query", Uri.class, String[].class, String.class, String[].class, String.class);
        }
        String targetAuthority = HostInfo.getPackageName() + ".fileprovider";
        XC_MethodHook hookOpenFile = new XC_MethodHook(51) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Uri uri = (Uri) param.args[0];
                String mode = (String) param.args[1];
                // Log.d("beforeHookedMethod: " + uri + " " + mode);
                if (targetAuthority.equals(uri.getAuthority())) {
                    String[] path = getUriPathHierarchy(uri);
                    if (path.length != 0 && "qauxv".equals(path[0])) {
                        try {
                            param.setResult(doOnOpenFile(uri, mode));
                        } catch (FileNotFoundException e) {
                            param.setThrowable(e);
                        }
                    }
                }
            }
        };
        XC_MethodHook hookQuery = new XC_MethodHook(51) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Uri uri = (Uri) param.args[0];
                String[] projection = (String[]) param.args[1];
                String selection = (String) param.args[2];
                String[] selectionArgs = (String[]) param.args[3];
                String sortOrder = (String) param.args[4];
                // Log.d("beforeHookedMethod: " + uri + " " + Arrays.toString(projection) + " " + selection + " " + Arrays.toString(selectionArgs));
                if (targetAuthority.equals(uri.getAuthority())) {
                    String[] path = getUriPathHierarchy(uri);
                    // Log.d("path: " + Arrays.toString(path));
                    if (path.length != 0 && "qauxv".equals(path[0])) {
                        param.setResult(doOnQuery(uri, projection, selection, selectionArgs, sortOrder));
                    }
                }
            }
        };
        if (supportOpenFile != null) {
            XposedBridge.hookMethod(supportOpenFile, hookOpenFile);
            // Log.d("hooked supportOpenFile");
        }
        if (androidxOpenFile != null) {
            XposedBridge.hookMethod(androidxOpenFile, hookOpenFile);
            // Log.d("hooked androidxOpenFile");
        }
        if (supportQuery != null) {
            XposedBridge.hookMethod(supportQuery, hookQuery);
            // Log.d("hooked supportQuery");
        }
        if (androidxQuery != null) {
            XposedBridge.hookMethod(androidxQuery, hookQuery);
            // Log.d("hooked androidxQuery");
        }
        // Log.d("proc: " + SyncUtils.getProcessName());
        sIsHooked = true;
    }

    public static class ItemEntry {

        String id;
        String displayName;
        String mimeType;
        ParcelFileDescriptor pfd;
    }

    public static String getAuthority() {
        return HostInfo.getPackageName() + ".fileprovider";
    }

    public static String addItem(@NonNull String displayName, @Nullable String mimeType, @NonNull ParcelFileDescriptor pfd) {
        if (!SyncUtils.isMainProcess()) {
            throw new IllegalStateException("addItem must be called in main process");
        }
        ShadowTmpFileProviderImpl impl = (ShadowTmpFileProviderImpl) getShadowTmpFileProvider();
        String id = UUID.randomUUID().toString();
        checkName(displayName);
        Objects.requireNonNull(pfd, "pfd is null");
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        ItemEntry item = new ItemEntry();
        item.id = id;
        item.displayName = displayName;
        item.mimeType = mimeType;
        item.pfd = pfd;
        impl.addItem(item);
        String authority = HostInfo.getPackageName() + ".fileprovider";
        String path = "/qauxv/tmp/" + id + "/" + displayName;
        return new Uri.Builder().scheme("content").authority(authority).path(path).build().toString();
    }

    private static void checkName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (name.length() == 0) {
            throw new IllegalArgumentException("name must not be empty");
        }
        if (name.contains("/")) {
            throw new IllegalArgumentException("name must not contain '/'");
        }
    }

    private static IShadowTmpFileProvider sShadowTmpFileProvider = null;

    private static class ShadowTmpFileProviderImpl extends IShadowTmpFileProvider.Stub {

        private final HashMap<String, ItemEntry> mItems = new HashMap<>(1);

        @Override
        public boolean isTmpFileExists(String id) {
            return mItems.containsKey(id);
        }

        @Override
        public long getTmpFileSize(String id) {
            ItemEntry item = mItems.get(id);
            if (item == null) {
                return -1;
            }
            return item.pfd.getStatSize();
        }

        @Override
        public String getTmpFileMimeType(String id) {
            ItemEntry item = mItems.get(id);
            if (item == null) {
                return null;
            }
            return item.mimeType;
        }

        @Override
        public String getTmpFileName(String id) {
            ItemEntry item = mItems.get(id);
            if (item == null) {
                return null;
            }
            return item.displayName;
        }

        @Override
        public ParcelFileDescriptor getTmpFileDescriptor(String id) {
            ItemEntry item = mItems.get(id);
            if (item == null) {
                return null;
            }
            try {
                return item.pfd.dup();
            } catch (IOException e) {
                Log.e(e);
                return null;
            }
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        void addItem(ItemEntry item) {
            mItems.put(item.id, item);
        }
    }

    public static void attachShadowTmpFileProviderBinder(IBinder binder) {
        if (SyncUtils.isMainProcess()) {
            throw new IllegalStateException("attachShadowTmpFileProviderBinder must NOT be called in main process");
        } else {
            sShadowTmpFileProvider = IShadowTmpFileProvider.Stub.asInterface(binder);
        }
    }

    public static IShadowTmpFileProvider getShadowTmpFileProvider() {
        synchronized (ShadowFileProvider.class) {
            if (sShadowTmpFileProvider == null) {
                if (SyncUtils.isMainProcess()) {
                    sShadowTmpFileProvider = new ShadowTmpFileProviderImpl();
                } else {
                    // we are not sure whether the main process is alive
                    return null;
                }
            }
        }
        return sShadowTmpFileProvider;
    }
}
