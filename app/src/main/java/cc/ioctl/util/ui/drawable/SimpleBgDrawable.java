/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
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
import android.graphics.drawable.Drawable;

public class SimpleBgDrawable extends Drawable {

    private final int iColor;
    private final int iEdgeColor;
    private final int iEdgeWidth;
    private final Paint mPaint;

    public SimpleBgDrawable(int color, int edgeColor, int edgeWidth) {
        iColor = color;
        iEdgeColor = edgeColor;
        iEdgeWidth = edgeWidth;
        mPaint = new Paint();
    }

    public Paint getPaint() {
        return mPaint;
    }

    @Override
    public void draw(Canvas canvas) {
        int i = iEdgeWidth;
        int w = getBounds().width();
        int h = getBounds().height();
        if (iEdgeWidth > 0) {
            mPaint.setColor(iEdgeColor);
            canvas.drawRect(0, 0, w, i, mPaint);
            canvas.drawRect(0, h - i, w, h, mPaint);
            canvas.drawRect(0, i, i, h - i, mPaint);
            canvas.drawRect(w - i, i, w, h - i, mPaint);
        }
        mPaint.setColor(iColor);
        canvas.drawRect(i, i, w - i, h - i, mPaint);
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getOpacity() {
        return android.graphics.PixelFormat.TRANSLUCENT;
    }

}
