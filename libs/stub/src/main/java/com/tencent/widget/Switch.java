package com.tencent.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import java.util.Random;

public class Switch extends CompoundButton {

    public Switch(Context context) {
        this(context, null);
        throw new RuntimeException("Stub!");
    }

    public Switch(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, new Random().nextInt());
        throw new RuntimeException("Stub!");
    }

    public Switch(Context context, AttributeSet attributeSet, int i2) {
        super(context, attributeSet, i2);
        throw new RuntimeException("Stub!");
    }

    //WARN: Override methods ignored

    public void setSwitchTextAppearance(Context context, int i2) {
        throw new RuntimeException("Stub!");
    }

    public void setSwitchTypeface(Typeface typeface, int i2) {
        setSwitchTypeface(typeface);
        throw new RuntimeException("Stub!");
    }

    public void setSwitchTypeface(Typeface typeface) {
        throw new RuntimeException("Stub!");
    }

    public void setSwitchPadding(int i2) {
        requestLayout();
        throw new RuntimeException("Stub!");
    }

    public void setSwitchMinWidth(int i2) {
        requestLayout();
        throw new RuntimeException("Stub!");
    }

    public void setThumbTextPadding(int i2) {
        requestLayout();
        throw new RuntimeException("Stub!");
    }

    public void setTrackDrawable(Drawable drawable) {
        requestLayout();
        throw new RuntimeException("Stub!");
    }

    public void setTrackResource(int i2) {
        setTrackDrawable(getContext().getResources().getDrawable(i2));
        throw new RuntimeException("Stub!");
    }

    public void setThumbDrawable(Drawable drawable) {
        requestLayout();
        throw new RuntimeException("Stub!");
    }

    public void setThumbResource(int i2) {
        setThumbDrawable(getContext().getResources().getDrawable(i2));
        throw new RuntimeException("Stub!");
    }

    public void setTextOn(CharSequence charSequence) {
        requestLayout();
        throw new RuntimeException("Stub!");
    }

    public void setTextOff(CharSequence charSequence) {
        requestLayout();
        throw new RuntimeException("Stub!");
    }
}
