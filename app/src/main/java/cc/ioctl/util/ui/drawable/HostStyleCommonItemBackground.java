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

package cc.ioctl.util.ui.drawable;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HostStyleCommonItemBackground extends Drawable {

    private boolean mLastIsHighlight = false;
    private Paint mPaint = new Paint();

    @Override
    public void draw(@NonNull Canvas canvas) {
        // get target rect
        int left = getBounds().left;
        int top = getBounds().top;
        int right = getBounds().right;
        int bottom = getBounds().bottom;
        // draw background
        if (mLastIsHighlight) {
            // draw highlight background, use alpha to make it more obvious
            mPaint.setColor(0x40808080);
            canvas.drawRect(left, top, right, bottom, mPaint);
        }
        // draw divider
        mPaint.setColor(0xFF808080);
        canvas.drawLine(left, bottom - 1, right, bottom - 1, mPaint);
    }

    private boolean isHighlightState(int[] state) {
        for (int i : state) {
            if (i == android.R.attr.state_selected || i == android.R.attr.state_pressed) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean onStateChange(int[] state) {
        boolean isHighlight = isHighlightState(state);
        if (mLastIsHighlight != isHighlight) {
            mLastIsHighlight = isHighlight;
            return true;
        }
        return false;
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    public void setAlpha(int alpha) {
        // unsupported
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        // unsupported
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
