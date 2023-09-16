package cc.hicore.ui.handygridview.scrollrunner;

public interface OnItemMovedListener {
    /**
     * Called when user moved the item of gridview.
     * you should swipe data in this method.
     * @param from item's original position
     * @param to   item's destination poisition
     */
    void onItemMoved(int from, int to);

    /**
     * return true if the item of special position can not move.
     * @param position
     * @return
     */
    boolean isFixed(int position);
}
