package name.mikanoshi.customiuizer.holidays;

import android.app.Activity;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import androidx.annotation.Nullable;
import com.github.jinatonic.confetti.ConfettiManager;
import com.github.jinatonic.confetti.ConfettoGenerator;
import com.github.matteobattilana.weather.PrecipType;
import com.github.matteobattilana.weather.WeatherView;
import io.github.qauxv.R;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import name.mikanoshi.customiuizer.utils.GravitySensor;
import name.mikanoshi.customiuizer.utils.Helpers;
import name.mikanoshi.customiuizer.utils.Helpers.Holidays;

public class HolidayHelper {

    private static WeakReference<WeatherView> weatherView;
    private static WeakReference<GravitySensor> angleListener;

    public static void setWeatherGenerator(ConfettoGenerator generator) {
        try {
            ConfettiManager manager = weatherView.get().getConfettiManager();
            Field confettoGenerator = ConfettiManager.class.getDeclaredField("confettoGenerator");
            confettoGenerator.setAccessible(true);
            confettoGenerator.set(manager, generator);
        } catch (ReflectiveOperationException | NullPointerException t) {
            t.printStackTrace();
        }
    }

    public static void setup(Activity activity) {
        Helpers.detectHoliday();
        if (Helpers.currentHoliday == null || Helpers.currentHoliday == Holidays.NONE) {
            return;
        }

        if (!injectWeatherView(activity)) {
            return;
        }

        WeatherView view = activity.findViewById(R.id.weather_view);
        ImageView header = activity.findViewById(R.id.holiday_header);

        view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        weatherView = new WeakReference<>(view);
        GravitySensor listener = null;
        if (Helpers.currentHoliday == Helpers.Holidays.NEWYEAR) {
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            view.setPrecipType(PrecipType.SNOW);
            view.setSpeed(50);
            view.setEmissionRate(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270 ? 8 : 4);
            view.setFadeOutPercent(0.75f);
            view.setAngle(0);
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            lp.height = activity.getResources().getDisplayMetrics().heightPixels
                    / (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270 ? 2 : 3);
            view.setLayoutParams(lp);
            setWeatherGenerator(new SnowGenerator(activity));
            view.resetWeather();
            view.setVisibility(View.VISIBLE);
            view.getConfettiManager().setRotationalVelocity(0, 45);

            listener = new GravitySensor(activity, view);
            listener.setOrientation(rotation);
            listener.setSpeed(50);
            listener.start();

            header.setImageResource(R.drawable.newyear_header);
            header.setVisibility(View.VISIBLE);
        } else if (Helpers.currentHoliday == Helpers.Holidays.LUNARNEWYEAR) {
            int rotation = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                rotation = activity.getDisplay().getRotation();
            } else {
                rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            }
            view.setPrecipType(PrecipType.SNOW);
            view.setSpeed(35);
            view.setEmissionRate(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270 ? 4 : 2);
            view.setFadeOutPercent(0.75f);
            view.setAngle(0);
            ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) view.getLayoutParams();
            lp.height = activity.getResources().getDisplayMetrics().heightPixels
                    / (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270 ? 3 : 4);
            view.setLayoutParams(lp);
            setWeatherGenerator(new FlowerGenerator(activity));
            view.resetWeather();
            view.setVisibility(View.VISIBLE);
            view.getConfettiManager().setRotationalVelocity(0, 45);

            listener = new GravitySensor(activity, view);
            listener.setOrientation(rotation);
            listener.setSpeed(35);
            listener.start();

            header.setImageResource(R.drawable.lunar_newyear_header);
            header.setVisibility(View.VISIBLE);
        } else {
            ((ViewGroup) view.getParent()).removeView(view);
            ((ViewGroup) header.getParent()).removeView(header);
        }
        angleListener = new WeakReference<>(listener);
    }

    /**
     * Find a top-level FrameLayout or RelativeLayout in the activity and add the weather view to it.
     *
     * @param activity The activity to add the view to.
     * @return the target container, null if none found.
     */
    @Nullable
    public static ViewGroup findSuitableContainer(Activity activity) {
        ViewGroup container = activity.findViewById(android.R.id.content);
        do {
            if (container instanceof FrameLayout || container instanceof RelativeLayout) {
                return container;
            }
            container = (ViewGroup) container.getParent();
        } while (container != null);
        return null;
    }

    private static boolean injectWeatherView(Activity activity) {
        ViewGroup container = findSuitableContainer(activity);
        if (container == null) {
            return false;
        }
        if (container instanceof RelativeLayout) {
            //	<com.github.matteobattilana.weather.WeatherView
            //		android:id="@+id/weather_view"
            //		android:layout_height="match_parent"
            //		android:layout_width="match_parent"
            //		android:layout_alignParentTop="true"
            //		android:translationZ="101dp"
            //		android:focusable="false"
            //		android:focusableInTouchMode="false"
            //		android:visibility="gone" />
            WeatherView weatherView = new WeatherView(activity, null);
            weatherView.setId(R.id.weather_view);
            weatherView.setTranslationZ(101);
            weatherView.setFocusable(false);
            weatherView.setFocusableInTouchMode(false);
            weatherView.setVisibility(View.GONE);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            lp.setMargins(0, 0, 0, 0);
            container.addView(weatherView, lp);
            //<ImageView
            //		android:id="@+id/holiday_header"
            //		android:layout_width="match_parent"
            //		android:layout_height="wrap_content"
            //		android:scaleType="fitStart"
            //		android:adjustViewBounds="true"
            //		android:layout_alignParentTop="true"
            //		android:translationZ="100dp"
            //		android:focusable="false"
            //		android:focusableInTouchMode="false"
            //		android:visibility="gone"
            //		tools:ignore="ContentDescription" />
            ImageView header = new ImageView(activity);
            header.setId(R.id.holiday_header);
            header.setAdjustViewBounds(true);
            header.setScaleType(ImageView.ScaleType.FIT_START);
            header.setTranslationZ(100);
            header.setFocusable(false);
            header.setFocusableInTouchMode(false);
            header.setVisibility(View.GONE);
            RelativeLayout.LayoutParams headerLp = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            headerLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            headerLp.setMargins(0, 0, 0, 0);
            container.addView(header, headerLp);
            return true;
        } else if (container instanceof FrameLayout) {
            WeatherView weatherView = new WeatherView(activity, null);
            weatherView.setId(R.id.weather_view);
            weatherView.setTranslationZ(101);
            weatherView.setFocusable(false);
            weatherView.setFocusableInTouchMode(false);
            weatherView.setVisibility(View.GONE);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            lp.setMargins(0, 0, 0, 0);
            container.addView(weatherView, lp);
            ImageView header = new ImageView(activity);
            header.setId(R.id.holiday_header);
            header.setAdjustViewBounds(true);
            header.setScaleType(ImageView.ScaleType.FIT_START);
            header.setTranslationZ(100);
            header.setFocusable(false);
            header.setFocusableInTouchMode(false);
            header.setVisibility(View.GONE);
            FrameLayout.LayoutParams headerLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            headerLp.setMargins(0, 0, 0, 0);
            container.addView(header, headerLp);
            return true;
        }
        return false;
    }

    public static void onPause() {
        if (angleListener != null) {
            GravitySensor listener = angleListener.get();
            if (listener != null) {
                listener.onPause();
            }
            WeatherView view = weatherView.get();
            if (view != null) {
                view.getConfettiManager().terminate();
            }
        }
    }

    public static void onResume() {
        if (angleListener != null) {
            GravitySensor listener = angleListener.get();
            if (listener != null) {
                listener.onResume();
            }
            WeatherView view = weatherView.get();
            if (view != null) {
                view.getConfettiManager().animate();
            }
        }
    }

    public static void onDestroy() {
        if (angleListener != null) {
            GravitySensor listener = angleListener.get();
            if (listener != null) {
                listener.stop();
            }
        }
    }
}
