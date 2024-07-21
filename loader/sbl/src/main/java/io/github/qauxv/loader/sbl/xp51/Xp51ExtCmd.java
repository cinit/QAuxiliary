package io.github.qauxv.loader.sbl.xp51;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.robv.android.xposed.XposedBridge;
import io.github.qauxv.loader.sbl.common.CheckUtils;
import io.github.qauxv.loader.sbl.common.ModuleLoader;

public class Xp51ExtCmd {

    private Xp51ExtCmd() {
    }

    public static Object handleQueryExtension(@NonNull String cmd, @Nullable Object[] arg) {
        CheckUtils.checkNonNull(cmd, "cmd");
        switch (cmd) {
            case "GetXposedBridgeClass":
                return XposedBridge.class;
            case "GetLoadPackageParam":
                return Xp51HookEntry.getLoadPackageParam();
            case "GetInitZygoteStartupParam":
                return Xp51HookEntry.getInitZygoteStartupParam();
            case "GetInitErrors":
                return ModuleLoader.getInitErrors();
            default:
                return null;
        }
    }

}
