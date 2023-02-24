/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

import android.os.Environment;
import io.github.qauxv.startup.HookEntry;
import io.github.qauxv.util.HostInfo;
import io.github.qauxv.util.Log;
import java.io.File;
import java.io.IOException;

public class SafeModeManager {

    private static SafeModeManager INSTANCE;

    public static final String SAFE_MODE_FILE_NAME = "qauxv_safe_mode";

    private File mSafeModeEnableFile;

    public static SafeModeManager getManager() {
        if (INSTANCE == null) {
            INSTANCE = new SafeModeManager();
        }
        INSTANCE.mSafeModeEnableFile = new File(
                Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" +
                        HookEntry.sCurrentPackageName + "/" + SAFE_MODE_FILE_NAME
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

    public boolean isEnabled() {
        return isAvailable() && mSafeModeEnableFile.exists();
    }

    public void setEnabled(boolean isEnable) {
        if (!isAvailable()) {
            return;
        }
        if (isEnable) {
            try {
                boolean isCreated = mSafeModeEnableFile.createNewFile();
                if (!isCreated) {
                    throw new IOException("Failed to create file: " + mSafeModeEnableFile.getAbsolutePath());
                }
            } catch (SecurityException | IOException e) {
                Log.e("Safe mode enable failed", e);
            }
        } else {
            if (isEnabled()) {
                try {
                    boolean isDeleted = mSafeModeEnableFile.delete();
                    if (!isDeleted) {
                        throw new IOException("Failed to delete file: " + mSafeModeEnableFile.getAbsolutePath());
                    }
                } catch (SecurityException | IOException e) {
                    Log.e("Safe mode disable failed", e);
                }
            } else {
                Log.w("Safe mode is not enabled, ignored");
            }
        }
    }
}
