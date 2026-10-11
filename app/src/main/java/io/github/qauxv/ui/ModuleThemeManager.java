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

package io.github.qauxv.ui;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import io.github.qauxv.R;
import io.github.qauxv.config.ConfigManager;

/**
 * Created by zpp0196 on 2019/5/18.
 */
public class ModuleThemeManager {

    private ModuleThemeManager() {
    }

    private static final ThemeColor THEME_COLOR_DEFAULT = ThemeColor.FTB;
    private static final String CFG_THEME_COLOR = "theme_color";
    private static ThemeColor sCurrentThemeColor = null;

    public static int[] getThemeColors(@NonNull Context context) {
        ThemeColor[] themes = ThemeColor.values();
        int[] colors = new int[themes.length];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = ContextCompat.getColor(context, themes[i].colorId);
        }
        return colors;
    }

    private static ThemeColor color2ThemeColor(@NonNull Context context, int color) {
        ThemeColor[] themes = ThemeColor.values();
        for (ThemeColor theme : themes) {
            if (ContextCompat.getColor(context, theme.colorId) == color) {
                return theme;
            }
        }
        return THEME_COLOR_DEFAULT;
    }

    private static ThemeColor title2ThemeColor(@NonNull String title) {
        ThemeColor[] themes = ThemeColor.values();
        for (ThemeColor theme : themes) {
            if (theme.title.equals(title)) {
                return theme;
            }
        }
        return THEME_COLOR_DEFAULT;
    }

    public static void setCurrentThemeColor(@NonNull Activity activity, int color) {
        sCurrentThemeColor = color2ThemeColor(activity, color);
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        cfg.putString(CFG_THEME_COLOR, sCurrentThemeColor.title);
        cfg.save();
    }

    public static ThemeColor getCurrentThemeColor() {
        if (sCurrentThemeColor == null) {
            String title = ConfigManager.getDefaultConfig().getStringOrDefault(CFG_THEME_COLOR, THEME_COLOR_DEFAULT.title);
            sCurrentThemeColor = title2ThemeColor(title);
        }
        return sCurrentThemeColor;
    }

    public static int getCurrentThemeColorId(@NonNull Context context) {
        return ContextCompat.getColor(context, getCurrentThemeColor().colorId);
    }

    public static int getCurrentThemeColorStyleId() {
        return getCurrentThemeColor().styleId;
    }

    public static String getCurrentThemeColorName() {
        return getCurrentThemeColor().title;
    }

    private static final ThemeMode THEME_MODE_DEFAULT = ThemeMode.FOLLOW_HOST;
    private static final String CFG_THEME_MODE = "theme_mode";
    private static ThemeMode sCurrentThemeMode = null;

    public static String[] getThemeModes() {
        ThemeMode[] themes = ThemeMode.values();
        String[] titles = new String[themes.length];
        for (int i = 0; i < themes.length; i++) {
            titles[i] = themes[i].title;
        }
        return titles;
    }

    private static ThemeMode title2ThemeMode(@NonNull String title) {
        ThemeMode[] themes = ThemeMode.values();
        for (ThemeMode theme : themes) {
            if (theme.title.equals(title)) {
                return theme;
            }
        }
        return THEME_MODE_DEFAULT;
    }

    public static void setCurrentThemeMode(int index) {
        ThemeMode[] themes = ThemeMode.values();
        if (index < 0 || index >= themes.length) {
            return;
        }
        sCurrentThemeMode = themes[index];
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        cfg.putString(CFG_THEME_MODE, sCurrentThemeMode.title);
        cfg.save();
    }

    public static ThemeMode getCurrentThemeMode() {
        if (sCurrentThemeMode == null) {
            String title = ConfigManager.getDefaultConfig().getStringOrDefault(CFG_THEME_MODE, THEME_MODE_DEFAULT.title);
            sCurrentThemeMode = title2ThemeMode(title);
        }
        return sCurrentThemeMode;
    }

    public static int getCurrentThemeModeIndex() {
        ThemeMode current = getCurrentThemeMode();
        ThemeMode[] themes = ThemeMode.values();
        for (int i = 0; i < themes.length; i++) {
            if (themes[i] == current) {
                return i;
            }
        }
        return 0;
    }

    public static String getCurrentThemeModeName() {
        return getCurrentThemeMode().title;
    }

    public static int getCurrentNightMode() {
        ThemeMode mode = getCurrentThemeMode();
        switch (mode) {
            case FOLLOW_SYSTEM:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            case LIGHT:
                return AppCompatDelegate.MODE_NIGHT_NO;
            case DARK:
                return AppCompatDelegate.MODE_NIGHT_YES;
            case FOLLOW_HOST:
            default:
                return ResUtils.isInNightMode() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        }
    }

    public enum ThemeColor {
        BLP(R.color.theme_color_blp, R.style.AppTheme_Blp, "哔哩粉"),
        GOL(R.color.theme_color_gol, R.style.AppTheme_Gol, "亮棕色"),
        CAG(R.color.theme_color_cag, R.style.AppTheme_Cag, "酷安绿"),
        FTB(R.color.theme_color_ftb, R.style.AppTheme_Ftb, "胖次蓝"),
        GHP(R.color.theme_color_ghp, R.style.AppTheme_Ghp, "亮紫色"),
        MAR(R.color.theme_color_mar, R.style.AppTheme_Mar, "姨妈红"),
        TPO(R.color.theme_color_tpo, R.style.AppTheme_TPO, "热带橙"),
        TLG(R.color.theme_color_tlg, R.style.AppTheme_Tlg, "水鸭青"),
        RYB(R.color.theme_color_ryb, R.style.AppTheme_Ryb, "皇室蓝"),
        GAP(R.color.theme_color_gap, R.style.AppTheme_Gap, "基佬紫");

        public final int colorId;
        public final int styleId;
        public final String title;

        ThemeColor(int colorId, int styleId, String title) {
            this.colorId = colorId;
            this.styleId = styleId;
            this.title = title;
        }
    }

    public enum ThemeMode {
        FOLLOW_SYSTEM("跟随系统"),
        FOLLOW_HOST("跟随宿主"),
        LIGHT("浅色"),
        DARK("深色");

        public final String title;

        ThemeMode(String title) {
            this.title = title;
        }
    }
}
