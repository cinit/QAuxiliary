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

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.Gravity;

public class ProportionDrawable extends Drawable {

    private final int iGravity;
    private final int iDoneColor;
    private final int iUndoneColor;
    private final Paint p;
    private float fProportion;

    public ProportionDrawable(int doneColor, int undoneColor, int gravity, float prop) {
        iGravity = gravity;
        iDoneColor = doneColor;
        iUndoneColor = undoneColor;
        fProportion = prop;
        p = new Paint();
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void draw(Canvas canvas) {
        int h = getBounds().height();
        int w = getBounds().width();
        if (Gravity.LEFT == iGravity) {
            int x = (int) (0.5f + fProportion * w);
            p.setColor(iDoneColor);
            canvas.drawRect(0, 0, x, h, p);
            p.setColor(iUndoneColor);
            canvas.drawRect(x, 0, w, h, p);
        } else {
            throw new UnsupportedOperationException("Only Gravity.LEFT is supported!");
        }
    }

    public float getProportion() {
        return fProportion;
    }

    public void setProportion(float p) {
        if (p < 0f) {
            p = 0f;
        }
        if (p > 1.0f) {
            p = 1.0f;
        }
        fProportion = p;
        invalidateSelf();
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return android.graphics.PixelFormat.TRANSLUCENT;
    }


}
