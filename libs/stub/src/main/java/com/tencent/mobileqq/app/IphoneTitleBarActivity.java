package com.tencent.mobileqq.app;

import static android.view.Window.FEATURE_CUSTOM_TITLE;

import android.content.Intent;
import android.graphics.Paint;
import android.view.View;

@Deprecated
public class IphoneTitleBarActivity extends BaseActivity {

    public static final int LAYER_TYPE_SOFTWARE = 1;

    public static void setLayerType(View view) {
        if (view != null) {
            try {
                view.getClass().getMethod("setLayerType", Integer.TYPE, Paint.class)
                    .invoke(view, 0, null);
            } catch (Exception ignored) {
            }
        }
    }

    protected void requestWindowFeature(Intent intent) {
        requestWindowFeature(FEATURE_CUSTOM_TITLE);
    }

    public void setContentView(int i) {
        throw new RuntimeException("Stub!");
    }

    public void setContentView(View view) {
        throw new RuntimeException("Stub!");
    }

    public void setTitle(CharSequence charSequence) {
        throw new RuntimeException("Stub!");
    }

    public void setTitle(CharSequence charSequence, String str) {
        throw new RuntimeException("Stub!");
    }

    public String getTextTitle() {
        throw new RuntimeException("Stub!");
    }

    public void setLeftViewName(Intent intent) {
        throw new RuntimeException("Stub!");
    }

    public void setLeftViewName(int i) {
        throw new RuntimeException("Stub!");
    }

    public void setLeftButton(int i, View.OnClickListener onClickListener) {
        throw new RuntimeException("Stub!");
    }

    public void setRightButton(int i, View.OnClickListener onClickListener) {
        throw new RuntimeException("Stub!");
    }

    @Deprecated
    public View getRightTextView() {
        throw new RuntimeException("Stub!");
    }

    public void removeWebViewLayerType() {
        throw new RuntimeException("Stub!");
    }
}
