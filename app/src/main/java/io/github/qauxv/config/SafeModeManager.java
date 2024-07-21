/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package io.github.qauxv.config;

import android.os.Environment;
import io.github.qauxv.util.PackageConstants;
import io.github.qauxv.util.HostInfo;
import io.github.qauxv.util.Log;
import java.io.File;
import java.io.IOException;

public class SafeModeManager {

    private static SafeModeManager INSTANCE;

    public static final String SAFE_MODE_FILE_NAME = "qauxv_safe_mode";

    private File mSafeModeEnableFile;

    private boolean sIsSafeModeForThisTime = false;

    public static SafeModeManager getManager() {
        if (INSTANCE == null) {
            INSTANCE = new SafeModeManager();
        }
        INSTANCE.mSafeModeEnableFile = new File(
                Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" +
                        HostInfo.getHostInfo().getPackageName() + "/" + SAFE_MODE_FILE_NAME
        );
        return INSTANCE;
    }

    private boolean isAvailable() {
        if (HostInfo.isInModuleProcess()) {
            Log.w("SafeModeManager only available in host process, ignored");
            return false;
        }
        return true;
    }

    public boolean isEnabledForThisTime() {
        return sIsSafeModeForThisTime;
    }

    public void setSafeModeForThisTime(boolean isSafeMode) {
        sIsSafeModeForThisTime = isSafeMode;
    }

    public boolean isEnabledForNextTime() {
        return isAvailable() && mSafeModeEnableFile.exists();
    }

    public boolean setEnabledForNextTime(boolean isEnable) {
        if (!isAvailable()) {
            return false;
        }
        if (isEnable) {
            try {
                boolean isCreated = mSafeModeEnableFile.createNewFile();
                if (!isCreated) {
                    throw new IOException("Failed to create file: " + mSafeModeEnableFile.getAbsolutePath());
                }
                return true;
            } catch (SecurityException | IOException e) {
                Log.e("Safe mode enable failed", e);
            }
        } else {
            if (isEnabledForNextTime()) {
                try {
                    boolean isDeleted = mSafeModeEnableFile.delete();
                    if (!isDeleted) {
                        throw new IOException("Failed to delete file: " + mSafeModeEnableFile.getAbsolutePath());
                    }
                    return true;
                } catch (SecurityException | IOException e) {
                    Log.e("Safe mode disable failed", e);
                }
            } else {
                Log.w("Safe mode is not enabled, ignored");
            }
        }
        return false;
    }
}
