package name.mikanoshi.customiuizer.utils;

import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.WindowManager;
import java.util.Calendar;

@SuppressWarnings("WeakerAccess")
public class Helpers {

    public static Holidays currentHoliday = Holidays.NONE;

    public enum Holidays {
        NONE, NEWYEAR, LUNARNEWYEAR
    }

    @SuppressWarnings("ConstantConditions")
    public static void detectHoliday() {
        currentHoliday = Holidays.NONE;
        int holiday = 0;
        if (holiday > 0) {
            currentHoliday = Holidays.values()[holiday];
        }
        if (holiday == 0) {
            Calendar cal = Calendar.getInstance();
            int month = cal.get(Calendar.MONTH);
            int monthDay = cal.get(Calendar.DAY_OF_MONTH);
            int year = cal.get(Calendar.YEAR);

            // Lunar NY
            if ((month == 0 && monthDay > 15) || month == 1) {
                currentHoliday = Holidays.LUNARNEWYEAR;
            }
            // NY
            else if (month == 0 || month == 11) {
                currentHoliday = Holidays.NEWYEAR;
            }
        }
    }

    public static int getRotation(Context context) {
        if (VERSION.SDK_INT >= VERSION_CODES.R) {
            return context.getDisplay().getRotation();
        } else {
            return ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        }
    }
}
