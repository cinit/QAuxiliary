/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
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
package io.github.qauxv.ui.widget;

import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class Switch extends com.tencent.widget.Switch {

    private boolean mGreyState = false;

    public Switch(Context context) {
        super(context);
        if (!isEnabled()) {
            setGreyEffectEnabled(true);
        }
    }

    public Switch(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        if (!isEnabled()) {
            setGreyEffectEnabled(true);
        }
    }

    public Switch(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        if (!isEnabled()) {
            setGreyEffectEnabled(true);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setGreyEffectEnabled(!enabled);
    }

    private void setGreyEffectEnabled(boolean grey) {
        if (grey != mGreyState) {
            mGreyState = grey;
            if (grey) {
                Paint paint = new Paint();
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(0);
                paint.setColorFilter(new ColorMatrixColorFilter(cm));
                setLayerType(View.LAYER_TYPE_HARDWARE, paint);
            } else {
                setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    }
}
