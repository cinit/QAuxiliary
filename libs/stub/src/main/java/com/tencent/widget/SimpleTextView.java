package com.tencent.widget;

import android.content.Context;
import android.view.View;

public class SimpleTextView extends View {
    public SimpleTextView(Context context) {
        super(context);
    }

    public CharSequence getText() {
        // DON'T USE IN OLD VERSION QQ
        throw new RuntimeException("Stub!");
    }
}
