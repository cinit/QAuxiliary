package cc.hicore.ui.handygridview.listener;

import android.view.View;

public interface OnItemCapturedListener {
    /**
     * Called when user selected a view to drag.
     *
     * @param v
     */
    void onItemCaptured(View v,int position);

    /**
     * Called when user released the drag view.
     *
     * @param v
     */
    void onItemReleased(View v,int position);
}
