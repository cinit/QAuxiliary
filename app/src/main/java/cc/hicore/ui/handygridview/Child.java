package cc.hicore.ui.handygridview;

import android.content.Context;
import android.view.View;
import cc.hicore.ui.handygridview.scrollrunner.ICarrier;
import cc.hicore.ui.handygridview.scrollrunner.ScrollRunner;


public class Child implements ICarrier {
    public int position;
    public View view;
    private ScrollRunner mRunner;
    private int from, to;
    private boolean hasNext = false;
    private HandyGridView parent;

    public Child(View view) {
        this.view = view;
        mRunner = new ScrollRunner(this);
    }

    public void cancel() {
        mRunner.cancel();
        hasNext = false;
    }

    public void setParent(HandyGridView parent) {
        this.parent = parent;
    }

    private void move(final int offsetX, final int offsetY) {
        mRunner.start(offsetX, offsetY);
    }

    public void moveTo(int from, int to) {
        this.from = from;
        this.to = to;
        int[] start = parent.getLeftAndTopForPosition(from);
        int[] end = parent.getLeftAndTopForPosition(to);
        if (!mRunner.isRunning()) {
            int offsetX = end[0] - start[0];
            int offsetY = end[1] - start[1];
            move(offsetX, offsetY);
        } else {
            hasNext = true;
        }
    }

    @Override
    public void onDone() {
        int[] start = new int[]{view.getLeft(), view.getTop()};
        from = parent.pointToPosition(start[0], start[1]);

        int[] end = parent.getLeftAndTopForPosition(to);
        if (hasNext) {
            if (from != to) {
                int offsetX = end[0] - start[0];
                int offsetY = end[1] - start[1];
                move(offsetX, offsetY);
            }
            hasNext = false;
        }
    }

    @Override
    public void onMove(int lastX, int lastY, int curX, int curY) {
        int deltaX = curX - lastX;
        int deltaY = curY - lastY;
        view.offsetLeftAndRight(deltaX);
        view.offsetTopAndBottom(deltaY);
    }

    @Override
    public boolean post(Runnable runnable) {
        return view.post(runnable);
    }

    @Override
    public boolean removeCallbacks(Runnable action) {
        return view.removeCallbacks(action);
    }

    @Override
    public Context getContext() {
        return view.getContext();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof Child) {
            Child child = (Child) obj;
            if (this.view == child.view) {
                return true;
            }
        }
        return super.equals(obj);
    }
}
