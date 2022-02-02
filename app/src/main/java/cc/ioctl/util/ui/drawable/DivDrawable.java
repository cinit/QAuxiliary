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
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

public class DivDrawable extends Drawable {

    public static final int TYPE_HORIZONTAL = 1;
    public static final int TYPE_VERTICAL = 2;
    private final int iThickness;
    private final int iType;
    private final Paint p = new Paint();

    public DivDrawable(int type, int thickness) {
        iType = type;
        iThickness = thickness;
    }

    @Override
    public void draw(Canvas canvas) {
        int h = getBounds().height();
        int w = getBounds().width();
        if (iType == TYPE_HORIZONTAL) {
            float off = (h - iThickness) / 2f;
            Shader s = new LinearGradient(0, off, 0, h / 2f, new int[]{0x00363636, 0x36363636},
                new float[]{0f, 1f}, Shader.TileMode.CLAMP);
            p.setShader(s);
            //p.setColor(0x36000000);
            canvas.drawRect(0, off, w, h / 2f, p);
            s = new LinearGradient(0, h / 2f, 0, h / 2f + iThickness / 2f,
                new int[]{0x36C8C8C8, 0x00C8C8C8}, new float[]{0f, 1f}, Shader.TileMode.CLAMP);
            p.setShader(s);
            //p.setColor(0x36FFFFFF);
            canvas.drawRect(0, h / 2f, w, h / 2f + iThickness / 2f, p);
        } else {
            throw new UnsupportedOperationException("iType == " + iType);
        }
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
