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

package io.github.qauxv.util;

import androidx.annotation.NonNull;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class IoUtils {

    private IoUtils() {
        throw new AssertionError("No instances");
    }

    /**
     * Reads all bytes from an input stream and returns them as a byte array.
     * <p>
     * If an exception occurs, the exception is rethrown and the input stream is closed.
     *
     * @param is the input stream to read from
     * @return a byte array containing all the bytes from the input stream
     * @throws IOException if an I/O error occurs
     */
    @NonNull
    public static byte[] readFully(@NonNull InputStream is) throws IOException {
        Objects.requireNonNull(is, "is == null");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        try (is) {
            int len;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
        }
        return baos.toByteArray();
    }

    /**
     * Read all bytes from a file and returns them as a byte array.
     * <p>
     * WARNING: This method will NOT work for a file which is larger than 2GB.
     *
     * @param file the file to read from
     * @return a byte array containing all the bytes from the file
     * @throws IOException if an I/O error occurs
     */
    @NonNull
    public static byte[] readFile(@NonNull File file) throws IOException {
        Objects.requireNonNull(file, "file == null");
        if (!file.exists()) {
            throw new IOException("File not found: " + file.getAbsolutePath());
        }
        long lsize = file.length();
        if (lsize > Integer.MAX_VALUE) {
            throw new IOException("File too large: " + file.getAbsolutePath() + ", size: " + lsize);
        }
        int size = (int) lsize;
        byte[] buf = new byte[size];
        try (InputStream is = new FileInputStream(file)) {
            int len;
            int offset = 0;
            while (offset < size && (len = is.read(buf, offset, size - offset)) != -1) {
                offset += len;
            }
            if (offset != size) {
                throw new IOException("Could not completely read file: " + file.getAbsolutePath() + ", expected: " + size + ", got: " + offset);
            }
        }
        return buf;
    }

    /**
     * Used for create a parent directory if it does not exist.
     * <p>
     * This will throw an unsafe {@link IOException} if the parent directory cannot be created.
     * <p>
     * Only use this when you think mkdirs() must be successful.
     *
     * @param file the file whose parent directory will be created if it does not exist
     * @return the same file as the parameter
     */
    @NonNull
    public static File makeParentDirsOrThrow(@NonNull File file) {
        Objects.requireNonNull(file, "file == null");
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            mkdirsOrThrow(parent);
        }
        return file;
    }

    /**
     * Used to create a directory if it does not exist.
     * <p>
     * This will throw an unsafe {@link IOException} if the directory cannot be created.
     * <p>
     * Only use this when you think mkdirs() must be successful.
     *
     * @param dir the directory to create if it does not exist
     * @return the same directory as the parameter
     */
    @NonNull
    public static File mkdirsOrThrow(@NonNull File dir) {
        Objects.requireNonNull(dir, "dir == null");
        if (!dir.exists() && !dir.mkdirs()) {
            unsafeThrow(new IOException("Failed to create directory: " + dir.getAbsolutePath()));
        }
        return dir;
    }

    public static void unsafeThrow(@NonNull Throwable t) {
        Objects.requireNonNull(t, "t == null");
        unsafeThrowImpl(t);
    }

    public static void unsafeThrowForIteCause(@NonNull Throwable t) {
        Objects.requireNonNull(t, "t == null");
        unsafeThrowImpl(getIteCauseOrSelf(t));
    }

    @NonNull
    public static Throwable getIteCauseOrSelf(@NonNull Throwable t) {
        Objects.requireNonNull(t, "t == null");
        Throwable cause;
        if (t instanceof InvocationTargetException && (cause = t.getCause()) != null) {
            return cause;
        } else {
            return t;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void unsafeThrowImpl(@NonNull Throwable t) throws T {
        throw (T) t;
    }

    public static void writeFile(@NonNull File file, @NonNull byte[] data) throws IOException {
        Objects.requireNonNull(file, "file == null");
        Objects.requireNonNull(data, "data == null");
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                mkdirsOrThrow(parent);
            }
            if (!file.createNewFile()) {
                throw new IOException("Failed to create file: " + file.getAbsolutePath());
            }
        }
        try (OutputStream os = new FileOutputStream(file)) {
            os.write(data);
            os.flush();
        }
    }

    @NonNull
    public static byte[] calculateFileMd5(@NonNull InputStream is) throws IOException {
        Objects.requireNonNull(is, "is == null");
        try (is) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) != -1) {
                md.update(buf, 0, len);
            }
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("No MD5 algorithm found", e);
        }
    }

    @NonNull
    public static byte[] calculateFileMd5(@NonNull File file) throws IOException {
        Objects.requireNonNull(file, "file == null");
        try (InputStream is = new FileInputStream(file)) {
            return calculateFileMd5(is);
        }
    }

    private static final char[] HEX_LOWER_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final char[] HEX_UPPER_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    @NonNull
    public static String calculateFileMd5HexString(@NonNull File file, boolean upperCase) throws IOException {
        Objects.requireNonNull(file, "file == null");
        byte[] md5 = calculateFileMd5(file);
        char[] hexDigits = upperCase ? HEX_UPPER_DIGITS : HEX_LOWER_DIGITS;
        char[] hex = new char[md5.length * 2];
        for (int i = 0; i < md5.length; i++) {
            int b = md5[i] & 0xFF;
            hex[i * 2] = hexDigits[b >>> 4];
            hex[i * 2 + 1] = hexDigits[b & 0xF];
        }
        return new String(hex);
    }
}
