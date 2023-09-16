package cc.hicore.ui.handygridview.listener;

import android.graphics.Canvas;


public interface IDrawer {
    /**
     * You can draw something in gridview by this method.
     *
     * @param canvas
     * @param width  the gridview's width
     * @param height the gridview's height
     */
    void onDraw(Canvas canvas, int width, int height);
}
