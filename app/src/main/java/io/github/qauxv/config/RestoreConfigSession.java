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

package io.github.qauxv.config;

import android.content.Context;
import androidx.annotation.NonNull;
import com.tencent.mmkv.MMKV;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class RestoreConfigSession implements Closeable {

    private static final String RESTORE_WORKING_DIR_NAME = "tmp_qa_restore_workdir";
    private static final String LOCK_FILE_NAME = "lock.pid";
    private static final String METADATA_FILE_NAME = "metadata.json";
    private String mBackupMetadata = null;
    private Context mContext;
    private File workingDir;
    private File lockFile;

    public RestoreConfigSession(@NonNull Context context) throws IllegalStateException {
        mContext = Objects.requireNonNull(context);
        workingDir = new File(mContext.getFilesDir(), RESTORE_WORKING_DIR_NAME);
        if (!workingDir.exists()) {
            if (!workingDir.mkdirs()) {
                throw new IllegalStateException("Failed to create working dir: " + workingDir.getAbsolutePath());
            }
        }
        lockFile = new File(workingDir, LOCK_FILE_NAME);
        createLockFile();
        // rm workingDir/*
        File[] workingDirFiles = workingDir.listFiles();
        if (workingDirFiles != null) {
            for (File workingDirFile : workingDirFiles) {
                if (!LOCK_FILE_NAME.equals(workingDirFile.getName())) {
                    workingDirFile.delete();
                }
            }
        }
    }

    private void createLockFile() throws IllegalStateException {
        try {
            int currentPid = android.os.Process.myPid();
            if (lockFile.exists()) {
                int lastPid = -1;
                // read pid
                byte[] bytes = new byte[64];
                FileInputStream fis = new FileInputStream(lockFile);
                int read = fis.read(bytes);
                fis.close();
                if (read > 0) {
                    String pidStr = new String(bytes, 0, read);
                    try {
                        lastPid = Integer.parseInt(pidStr);
                    } catch (NumberFormatException ignored) {
                        // ignore
                    }
                }
                if (lastPid > 0) {
                    // check if last pid is alive
                    if (new File("/proc/" + lastPid).exists()) {
                        throw new IllegalStateException("RestoreConfigSession is already running at pid: " + lastPid);
                    }
                }
            }
            if (!lockFile.exists()) {
                if (!lockFile.createNewFile()) {
                    throw new IllegalStateException("Failed to create lock file: " + lockFile.getAbsolutePath());
                }
            }
            FileOutputStream fos = new FileOutputStream(lockFile);
            fos.write(String.valueOf(currentPid).getBytes());
            fos.close();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create lock file: " + lockFile.getAbsolutePath(), e);
        }
    }

    @Override
    public void close() {
        if (mContext != null) {
            // rm workingDir/*
            File[] workingDirFiles = workingDir.listFiles();
            if (workingDirFiles != null) {
                for (File workingDirFile : workingDirFiles) {
                    workingDirFile.delete();
                }
            }
            // lockFile should be always deleted
            mBackupMetadata = null;
            mContext = null;
        }
    }

    private void checkState() {
        if (mContext == null) {
            throw new IllegalStateException("RestoreConfigSession has been closed");
        }
    }

    @NonNull
    public String[] listBackupMmkvConfig() {
        requireBackupLoaded();
        File[] backupFiles = workingDir.listFiles();
        if (backupFiles == null) {
            return new String[0];
        } else {
            List<String> mmkvConfigList = new ArrayList<>();
            for (File mmkvConfigListFile : backupFiles) {
                if (!mmkvConfigListFile.isDirectory()) {
                    String name = mmkvConfigListFile.getName();
                    if (!name.endsWith(".crc") && !name.endsWith(".zip") && !LOCK_FILE_NAME.equals(name)
                            && new File(mmkvConfigListFile.getAbsolutePath() + ".crc").exists()) {
                        mmkvConfigList.add(name);
                    }
                }
            }
            return mmkvConfigList.toArray(new String[0]);
        }
    }

    public void restoreBackupMmkvConfig(@NonNull String mmkvConfigName) {
        requireBackupLoaded();
        File mmkvConfigFile = new File(workingDir, mmkvConfigName);
        File mmkvCrcFile = new File(mmkvConfigFile.getAbsolutePath() + ".crc");
        if (!mmkvConfigFile.exists() || !mmkvCrcFile.exists()) {
            throw new IllegalArgumentException("config file not found: " + mmkvConfigName + " or CRC file not found");
        }
        if (!MMKV.restoreOneMMKVFromDirectory(mmkvConfigName, workingDir.getAbsolutePath(), null)) {
            throw new IllegalStateException("failed to restore mmkv config: " + mmkvConfigName + ", see log for more details");
        }
    }

    private void requireBackupLoaded() {
        checkState();
        if (mBackupMetadata == null) {
            throw new IllegalStateException("backup is not loaded");
        }
    }

    @NonNull
    public String[] listOnDeviceMmkvConfig() {
        checkState();
        File mmkvBaseDir = new File(MMKV.getRootDir());
        File[] mmkvConfigListFiles = mmkvBaseDir.listFiles();
        if (mmkvConfigListFiles == null) {
            return new String[0];
        } else {
            List<String> mmkvConfigList = new ArrayList<>();
            for (File mmkvConfigListFile : mmkvConfigListFiles) {
                if (!mmkvConfigListFile.isDirectory()) {
                    String name = mmkvConfigListFile.getName();
                    if (!name.endsWith(".crc") && !name.endsWith(".zip")
                            && new File(mmkvConfigListFile.getAbsolutePath() + ".crc").exists()) {
                        mmkvConfigList.add(name);
                    }
                }
            }
            return mmkvConfigList.toArray(new String[0]);
        }
    }

    public void loadBackupFile(@NonNull File backupFile) throws IOException {
        checkState();
        // rm workingDir/*
        File[] workingDirFiles = workingDir.listFiles();
        if (workingDirFiles != null) {
            for (File workingDirFile : workingDirFiles) {
                if (!LOCK_FILE_NAME.equals(workingDirFile.getName()) && workingDir.isFile()) {
                    workingDirFile.delete();
                }
            }
        }
        byte[] buffer = new byte[4096];
        // unzip backupFile to workingDir
        try (ZipFile zipFile = new ZipFile(backupFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                // entry should be a file, not a directory
                if (!entry.isDirectory()) {
                    // unzip entry to workingDir
                    File file = new File(workingDir, name);
                    file.createNewFile();
                    InputStream inputStream = zipFile.getInputStream(entry);
                    try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                        int s;
                        while ((s = inputStream.read(buffer)) > 0) {
                            fileOutputStream.write(buffer, 0, s);
                        }
                        inputStream.close();
                    }
                }
            }
        }
        // check if workingDir is valid, there should be a metadata.json file
        File metadataFile = new File(workingDir, METADATA_FILE_NAME);
        if (!metadataFile.exists()) {
            throw new IllegalStateException("Invalid backup file: " + backupFile.getAbsolutePath() + "\n"
                    + "The backup file is not a valid backup file.");
        }
        // read metadata.json
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (FileInputStream fileInputStream = new FileInputStream(metadataFile)) {
            int s;
            while ((s = fileInputStream.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer, 0, s);
            }
            // no need to close byteArrayOutputStream
            mBackupMetadata = byteArrayOutputStream.toString();
        }
    }
}
