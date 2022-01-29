package com.tencent.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ListAdapter;

public class XListView extends ViewGroup {

    public XListView(Context context) {
        this(context, (AttributeSet) null);
    }

    public XListView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16842868);
    }

    public XListView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        throw new RuntimeException("Stub!");
    }

    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        throw new RuntimeException("Stub!");
    }

    public void setOverScrollDistance(int i) {
        throw new RuntimeException("Stub!");
    }

    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        throw new RuntimeException("Stub!");
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        throw new RuntimeException("Stub!");
    }

    public void setEnsureOverScrollStatusToIdleWhenRelease(boolean z) {
        throw new RuntimeException("Stub!");
    }

    public void setOverScrollHeight(int i2) {
        throw new RuntimeException("Stub!");
    }

    public void setAdapter(ListAdapter listAdapter) {
        throw new RuntimeException("Stub!");
    }

    public void setDivider(Drawable drawable) {
        throw new RuntimeException("Stub!");
    }
}
