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

package cc.hicore.Utils;

import io.github.qauxv.config.ConfigManager;
import java.util.Map;

public class FunConf {
    public static void setString(String setName,String key,String value){
        ConfigManager manager = ConfigManager.getDefaultConfig();
        manager.putString(setName + ":" + key,value);
    }
    public static String getString(String setName,String key,String defValue){
        ConfigManager manager = ConfigManager.getDefaultConfig();
        return manager.getString(setName + ":" + key,defValue);
    }
    public static boolean getBoolean(String setName,String key,boolean defValue){
        ConfigManager manager = ConfigManager.getDefaultConfig();
        return manager.getBoolean(setName + ":" + key,defValue);
    }
    public static void setBoolean(String setName,String key,boolean value){
        ConfigManager manager = ConfigManager.getDefaultConfig();
        manager.putBoolean(setName + ":" + key,value);
    }
    public static int getInt(String setName,String key,int defValue){
        ConfigManager manager = ConfigManager.getDefaultConfig();
        return manager.getInt(setName + ":" + key,defValue);
    }
    public static void setInt(String setName,String key,int value){
        ConfigManager manager = ConfigManager.getDefaultConfig();
        manager.putInt(setName + ":" + key,value);
    }
    public static void removeConfig(String setName){
        ConfigManager manager = ConfigManager.getDefaultConfig();
        Map<String, ?> map = manager.getAll();
        map.keySet().removeIf(key -> key.startsWith(setName + ":"));
    }
}
