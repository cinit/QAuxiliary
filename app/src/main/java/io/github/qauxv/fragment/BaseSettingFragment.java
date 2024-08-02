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

package io.github.qauxv.fragment;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import io.github.qauxv.activity.SettingsUiFragmentHostActivity;

public abstract class BaseSettingFragment extends Fragment {

    private SettingsUiFragmentHostActivity mSettingsHostActivity = null;
    @Nullable
    private String mTitle = null;

    @Nullable
    private String mSubtitle = null;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mSettingsHostActivity = (SettingsUiFragmentHostActivity) requireActivity();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mSettingsHostActivity = null;
    }

    @Nullable
    protected SettingsUiFragmentHostActivity getSettingsHostActivity() {
        return mSettingsHostActivity;
    }

    @NonNull
    protected SettingsUiFragmentHostActivity requireSettingsHostActivity() {
        if (mSettingsHostActivity == null) {
            throw new IllegalStateException("mSettingsHostActivity is null, is current fragment attached?");
        }
        return mSettingsHostActivity;
    }

    public void finishFragment() {
        if (mSettingsHostActivity == null) {
            throw new IllegalStateException("mSettingsHostActivity is null, is current fragment attached?");
        }
        mSettingsHostActivity.finishFragment(this);
    }

    @Nullable
    public String getTitle() {
        return mTitle;
    }

    protected void setTitle(@Nullable String title) {
        mTitle = title;
        if (mSettingsHostActivity != null) {
            mSettingsHostActivity.requestInvalidateActionBar();
        }
    }

    @Nullable
    public String getSubtitle() {
        return mSubtitle;
    }

    protected void setSubtitle(@Nullable String title) {
        mSubtitle = title;
        if (mSettingsHostActivity != null) {
            mSettingsHostActivity.requestInvalidateActionBar();
        }
    }

    public boolean doOnBackPressed() {
        return false;
    }

    public void notifyLayoutPaddingsChanged() {
        onLayoutPaddingsChanged();
    }

    /**
     * @deprecated use {@link #doOnCreateView(LayoutInflater, ViewGroup, Bundle)} instead
     */
    @Nullable
    @Deprecated
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return doOnCreateView(inflater, container, savedInstanceState);
    }

    @Nullable
    public View doOnCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                               @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    protected void onLayoutPaddingsChanged() {
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        onLayoutPaddingsChanged();
    }

    public int getLayoutPaddingTop() {
        return mSettingsHostActivity.getLayoutPaddingTop();
    }

    public int getLayoutPaddingBottom() {
        return mSettingsHostActivity.getLayoutPaddingBottom();
    }

    public boolean isWrapContent() {
        return true;
    }
}
