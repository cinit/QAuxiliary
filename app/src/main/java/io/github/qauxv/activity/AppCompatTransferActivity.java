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
package io.github.qauxv.activity;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import io.github.qauxv.R;
import io.github.qauxv.lifecycle.Parasitics;
import io.github.qauxv.util.HostInfo;
import io.github.qauxv.util.SavedInstanceStatePatchedClassReferencer;

public abstract class AppCompatTransferActivity extends AppCompatActivity {

    private ClassLoader mXref = null;

    @Override
    public ClassLoader getClassLoader() {
        if (mXref == null) {
            mXref = new SavedInstanceStatePatchedClassReferencer(
                AppCompatTransferActivity.class.getClassLoader());
        }
        return mXref;
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        Bundle windowState = savedInstanceState.getBundle("android:viewHierarchyState");
        if (windowState != null) {
            windowState.setClassLoader(AppCompatTransferActivity.class.getClassLoader());
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    protected void requestTranslucentStatusBar() {
        Window window = getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.flags &= ~WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        params.flags &= ~WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
        params.flags |= WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        window.setAttributes(params);
        View decorView = window.getDecorView();
        int option = decorView.getSystemUiVisibility()
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(option);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
    }

    protected int getNavigationBarLayoutInsect() {
        View decorView = getWindow().getDecorView();
        if ((decorView.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION) == 0) {
            return 0;
        } else {
            WindowInsets insets = decorView.getRootWindowInsets();
            if (insets != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    return insets.getInsets(WindowInsets.Type.systemBars()).bottom;
                } else {
                    return insets.getStableInsetBottom();
                }
            } else {
                return 0;
            }
        }
    }

    protected int getStatusBarLayoutInsect() {
        View decorView = getWindow().getDecorView();
        if ((decorView.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN) == 0) {
            return 0;
        } else {
            WindowInsets insets = decorView.getRootWindowInsets();
            if (insets != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    return insets.getInsets(WindowInsets.Type.systemBars()).top;
                } else {
                    return insets.getStableInsetTop();
                }
            } else {
                return 0;
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        try {
            getString(R.string.res_inject_success);
        } catch (Resources.NotFoundException e) {
            // inject resources
            // TODO: 2022-03-20 either inject into ActivityThread$H or Instrumentation to do this
            if (HostInfo.isInHostProcess()) {
                Parasitics.injectModuleResources(getResources());
            }
        }
        super.onConfigurationChanged(newConfig);
    }
}
