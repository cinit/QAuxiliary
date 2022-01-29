package com.tencent.mobileqq.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import com.tencent.widget.ScrollView;

public class BounceScrollView extends ScrollView {

    public BounceScrollView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setOverScrollMode(0);
        setFadingEdgeLength(0);
        throw new RuntimeException("Stub!");
    }

    protected void onScrollChanged(int i, int i2, int i3, int i4) {
        throw new RuntimeException("Stub!");
    }

    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        throw new RuntimeException("Stub!");
    }
}
