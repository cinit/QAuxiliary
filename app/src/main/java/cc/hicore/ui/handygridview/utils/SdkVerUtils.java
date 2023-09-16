package cc.hicore.ui.handygridview.utils;

import android.os.Build;

/**
 * Created by Administrator on 2017/11/26.
 */

public class SdkVerUtils {
    public static boolean isAboveVersion(int version) {
        int sdkVersion = Build.VERSION.SDK_INT;
        if (sdkVersion >= version) {
            return true;
        }
        return false;
    }

    public static boolean isAbove19() {
        return isAboveVersion(Build.VERSION_CODES.KITKAT);
    }
}
