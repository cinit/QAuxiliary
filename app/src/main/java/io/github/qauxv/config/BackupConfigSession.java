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
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupConfigSession implements Closeable {

    private static final String BACKUP_WORKING_DIR_NAME = "tmp_qa_backup_workdir";
    private Context mContext;
    private File workingDir;
    private File mmkvBaseDir;
    private File lockFile;
    private ArrayList<File> mTmpFiles = new ArrayList<>();

    public BackupConfigSession(@NonNull Context context) throws IllegalStateException {
        mContext = Objects.requireNonNull(context);
        workingDir = new File(mContext.getFilesDir(), BACKUP_WORKING_DIR_NAME);
        mmkvBaseDir = new File(MMKV.getRootDir());
        if (!workingDir.exists()) {
            if (!workingDir.mkdirs()) {
                throw new IllegalStateException("Failed to create working dir: " + workingDir.getAbsolutePath());
            }
        }
        lockFile = new File(workingDir, "lock.pid");
        createLockFile();
        // rm -rf workingDir/*
        File[] workingDirFiles = workingDir.listFiles();
        if (workingDirFiles != null) {
            for (File workingDirFile : workingDirFiles) {
                if (!"lock.pid".equals(workingDirFile.getName())) {
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
                        throw new IllegalStateException("BackupConfigSession is already running at pid: " + lastPid);
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
            // clean working dir
            for (File tmpFile : mTmpFiles) {
                tmpFile.delete();
            }
            lockFile.delete();
            mContext = null;
        }
    }

    private void checkState() {
        if (mContext == null) {
            throw new IllegalStateException("BackupConfigSession has been closed");
        }
    }

    private static void deleteFileRecursive(File file) throws IOException {
        if (file.isFile()) {
            if (!file.delete() && file.exists()) {
                throw new IOException("Failed to delete file: " + file.getAbsolutePath());
            }
        } else if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            if (listFiles != null) {
                for (File deleteFile : listFiles) {
                    deleteFileRecursive(deleteFile);
                }
            }
            if (!file.delete() && file.exists()) {
                throw new IOException("Failed to rmdir: " + file.getAbsolutePath());
            }
        }
    }

    @NonNull
    public String[] listMmkvConfig() {
        checkState();
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

    public void backupMmkvConfigToWorkDir(@NonNull String mmkvConfigName) throws IllegalStateException {
        checkState();
        File mmkvConfigFile = new File(mmkvBaseDir, mmkvConfigName);
        if (!mmkvConfigFile.exists()) {
            throw new IllegalStateException("Mmkv config file not found: " + mmkvConfigFile.getAbsolutePath());
        }
        File mmkvCrcFile = new File(mmkvConfigFile.getAbsolutePath() + ".crc");
        if (!mmkvCrcFile.exists()) {
            throw new IllegalStateException("Mmkv CRC file not found: " + mmkvCrcFile.getAbsolutePath());
        }
        if (!MMKV.backupOneToDirectory(mmkvConfigName, workingDir.getAbsolutePath(), null)) {
            throw new IllegalStateException("MMKV.backupOneToDirectory error: " + mmkvConfigName + ", check log for more detail");
        }
        // check if backup is successful
        File backupMmkvConfigFile = new File(workingDir, mmkvConfigName);
        if (!backupMmkvConfigFile.exists()) {
            throw new IllegalStateException("Failed to backup mmkv config file: " + mmkvConfigFile.getAbsolutePath());
        }
        File backupMmkvCrcFile = new File(backupMmkvConfigFile.getAbsolutePath() + ".crc");
        if (!backupMmkvCrcFile.exists()) {
            throw new IllegalStateException("Failed to backup mmkv CRC file: " + mmkvCrcFile.getAbsolutePath());
        }
        mTmpFiles.add(backupMmkvConfigFile);
        mTmpFiles.add(backupMmkvCrcFile);
    }

    public File createBackupZipFile() throws IOException {
        checkState();
        if (mTmpFiles.isEmpty()) {
            throw new IllegalStateException("empty backup list");
        }
        String zipFileName = "backup_" + System.currentTimeMillis() + ".zip";
        File zipFile = new File(workingDir, zipFileName);
        zipFile.createNewFile();
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile));
        // add mmkv config files
        for (File tmpFile : mTmpFiles) {
            if (tmpFile.isFile()) {
                ZipEntry zipEntry = new ZipEntry(tmpFile.getName());
                zipOutputStream.putNextEntry(zipEntry);
                FileInputStream fileInputStream = new FileInputStream(tmpFile);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fileInputStream.read(buffer)) != -1) {
                    zipOutputStream.write(buffer, 0, len);
                }
                fileInputStream.close();
                zipOutputStream.closeEntry();
            }
        }
        String metadataContent = "{\"timestamp\":\"" + System.currentTimeMillis() + "\"}";
        ZipEntry metadataEntry = new ZipEntry("metadata.json");
        zipOutputStream.putNextEntry(metadataEntry);
        zipOutputStream.write(metadataContent.getBytes());
        zipOutputStream.closeEntry();
        zipOutputStream.close();
        return zipFile;
    }
}
