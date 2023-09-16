package cc.hicore.ui.handygridview;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import cc.hicore.ui.handygridview.listener.IDrawer;
import cc.hicore.ui.handygridview.listener.OnItemCapturedListener;
import cc.hicore.ui.handygridview.scrollrunner.ICarrier;
import cc.hicore.ui.handygridview.scrollrunner.OnItemMovedListener;
import cc.hicore.ui.handygridview.scrollrunner.ScrollRunner;
import cc.hicore.ui.handygridview.utils.ReflectUtil;
import cc.hicore.ui.handygridview.utils.SdkVerUtils;


public class HandyGridView extends GridView implements AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener, ICarrier {
    private int mScrollY;
    private int mFirstTop, mFirstLeft;
    private int mFirstVisibleFirstItem = -1;
    private float mLastMotionX, mLastMotionY;
    private Children mChildren;
    private int mTouchSlop;
    private OnItemLongClickListener mOnItemLongClickListener;
    private OnScrollListener mOnScrollListener;
    private ScrollRunner mScrollRunner;
    private int mScrollSpeed = 750;
    private boolean mClipToPadding = false;
    private Rect mGridViewVisibleRect;
    private MotionEvent mCurrMotionEvent;

    private ListAdapter mAdapter;
    private OnItemMovedListener mItemMovedListener;

    private View mDraggedView;
    private int mDraggedIndex = -1;
    private Rect mDraggedRect = new Rect();
    private int mColumnWidth, mRowHeight, mColumnsNum;
    private int mHorizontalSpacing;
    private int mVerticalSpacing;
    private int mDraggedPosition = INVALID_POSITION;
    private OnItemClickListener mOnItemClickListener;
    private boolean mShouldMove = false;
    private boolean mUseSelector = false;
    private boolean mAutoOptimize = true;
    private MODE mode = MODE.TOUCH;

    private Drawable mSelector;
    private Drawable mSpaceDrawable;
    private IDrawer mDrawer;
    private boolean mDrawOnTop = false;

    public HandyGridView(Context context) {
        this(context, null);
    }

    public HandyGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mScrollRunner = new ScrollRunner(this, new AccelerateDecelerateInterpolator());
        setChildrenDrawingOrderEnabled(true);
        super.setOnItemLongClickListener(this);
        super.setOnItemClickListener(this);
        setOverScrollMode(OVER_SCROLL_NEVER);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mChildren = new Children(this);
        super.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (mOnScrollListener != null) {
                    mOnScrollListener.onScrollStateChanged(view, scrollState);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (visibleItemCount != 0) {
                    mFirstVisibleFirstItem = firstVisibleItem;
                    getBasicValues(firstVisibleItem);
                    if (mOnScrollListener != null) {
                        mOnScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
                    }
                }
            }
        });
    }

    private void getBasicValues(int firstVisibleItem) {
        View firstChild = getChildAt(0);
        mFirstLeft = getListPaddingLeft();
        mFirstTop = firstChild.getTop();
        mColumnWidth = firstChild.getWidth();
        mRowHeight = firstChild.getHeight();
        int rowLine = firstVisibleItem / mColumnsNum;
        mScrollY = mFirstTop - rowLine * (mVerticalSpacing + mRowHeight);
    }

    @Override
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        mOnItemLongClickListener = listener;
    }

    @Override
    public void setOnScrollListener(OnScrollListener l) {
        mOnScrollListener = l;
    }

    /**
     * If you want to draw something in gridview,just set a drawer.
     *
     * @param drawer    a drawer.
     * @param drawOnTop true if you want draw on the top of Gridview.
     * @see IDrawer#onDraw(Canvas, int, int)
     */
    public void setDrawer(IDrawer drawer, boolean drawOnTop) {
        this.mDrawer = drawer;
        mDrawOnTop = drawOnTop;
    }

    /**
     * Tells the GridView whether to use Selector.
     * Nonuse selector by default.
     *
     * @param enabled true if use selector.
     */
    public void setSelectorEnabled(boolean enabled) {
        if (enabled != mUseSelector) {
            mUseSelector = enabled;
            if (mUseSelector && mSelector != null) {
                setSelector(mSelector);
            }
            if (!mUseSelector) {
                setSelector(getSelector());
            }
        }

    }

    public boolean isSelectorEnabled() {
        return mUseSelector;
    }

    @Override
    public void setSelector(Drawable sel) {
        if (mUseSelector) {
            super.setSelector(sel);
        } else {
            mSelector = sel;
            if (mSpaceDrawable == null) {
                mSpaceDrawable = new ColorDrawable();
            }
            super.setSelector(mSpaceDrawable);
        }
    }

    @Override
    public void setHorizontalSpacing(int horizontalSpacing) {
        super.setHorizontalSpacing(horizontalSpacing);
        mHorizontalSpacing = horizontalSpacing;
    }

    @Override
    public void setNumColumns(int numColumns) {
        super.setNumColumns(numColumns);
        mColumnsNum = numColumns;
    }

    @Override
    public void setClipToPadding(boolean clipToPadding) {
        super.setClipToPadding(clipToPadding);
        mClipToPadding = clipToPadding;
    }

    @Override
    public void setVerticalSpacing(int verticalSpacing) {
        super.setVerticalSpacing(verticalSpacing);
        mVerticalSpacing = verticalSpacing;
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        mAdapter = adapter;
        if (adapter instanceof OnItemMovedListener) {
            mItemMovedListener = (OnItemMovedListener) adapter;
        } else {
            //TODO should throw a exception here.
            log("Your adapter should implements OnItemMovedListener for listening  item's swap action.");
        }
        super.setAdapter(mAdapter);
    }

    /**
     * When set, will change TOUCH mode to LONG_PRESS mode if the content of gridview can not scroll.
     * Set by default.
     *
     * @param autoOptimize
     */
    public void setAutoOptimize(boolean autoOptimize) {
        mAutoOptimize = autoOptimize;
    }

    public void setMode(MODE mode) {
        this.mode = mode;
    }

    /**
     * set the number of pixels gridview scrolled per second.
     *
     * @param scrollSpeed
     */
    public void setScrollSpeed(int scrollSpeed) {
        mScrollSpeed = scrollSpeed;
    }

    public int getDragPosition() {
        return mDraggedPosition;
    }

    private void refreshChildren() {
        int childCount = getChildCount();
        clearAllChildren();
        for (int i = 0; i < childCount; i++) {
            addChild(i, super.getChildAt(i));
        }
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
//        if (getChildCount() > 0) {
//            getBasicValues(getFirstVisiblePosition());
//        }
        if (mDraggedView != null) {
            refreshChildren();
            View draggedView = super.getChildAt(mDraggedPosition - mFirstVisibleFirstItem);
            dispatchItemReleased();
            mDraggedView = draggedView;
            dispatchItemCaptured();
            correctDraggedViewLocation(0, 0);
        } else {
            refreshChildren();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mGridViewVisibleRect = null;
        measureVisibleRect();
    }

    @Override
    protected void detachViewsFromParent(int start, int count) {
        super.detachViewsFromParent(start, count);
        if (start == 0) {
            for (int i = start; i < start + count; i++) {
                removeChild(0);
            }
        } else {
            start = mChildren.size() - 1;
            for (int i = start; i > start - count; i--) {
                removeChild(i);
            }
        }
    }

    @Override
    protected void attachViewToParent(View child, int index, ViewGroup.LayoutParams params) {
        super.attachViewToParent(child, index, params);
        addChild(index, child);
    }

    @Override
    protected void detachAllViewsFromParent() {
        super.detachAllViewsFromParent();
        clearAllChildren();
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);
        int index = indexOfChild(child);
        addChild(index, child);
    }

    @Override
    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        removeChild(child);
    }

    private void clearAllChildren() {
        mChildren.clear();
    }

    private void addChild(int index, View view) {
        if (index < 0) {
            index = mChildren.size();
        }
        mChildren.add(index, view);
    }

    private boolean removeChild(View view) {
        boolean result = false;
        int childSize = mChildren.size();
        for (int i = 0; i < childSize; i++) {
            Child child = mChildren.get(i);
            if (child.view == view) {
                result = mChildren.remove(child);
                break;
            }
        }
        return result;
    }

    private void removeChild(int index) {
        mChildren.remove(index);
    }

    public boolean isTouchMode() {
        if (canScrollDown() || canScrollUp()) {
            // if the content of gridview can not scroll,change mode to LONG_PRESS for better user experience.
            if (mAutoOptimize) {
                mode = MODE.LONG_PRESS;
            }
        }
        return mode == MODE.TOUCH;
    }

    public boolean isNoneMode() {
        return mode == MODE.NONE;
    }

    public boolean isLongPressMode() {
        return mode == MODE.LONG_PRESS;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return handleTouchEvent(ev);
    }

    private boolean handleTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        mCurrMotionEvent = ev;
        boolean handled = false;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionX = ev.getRawX();
                mLastMotionY = ev.getRawY();
                mShouldMove = false;
//                Debug.startMethodTracing("MoveOnGridView");
                if (isTouchMode()) {
                    recordDragViewIfNeeded(null, -1);
                    invalidate();
                    if (mDraggedView != null) {
                        mDraggedView.setPressed(true);
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int deltaX = (int) (ev.getRawX() - mLastMotionX);
                int deltaY = (int) (ev.getRawY() - mLastMotionY);
                if (mDraggedView != null) {
                    handled = true;
                    // intercept the MotionEvent only when user is not scrolling
                    if (!mShouldMove) {
//                        if (Math.abs(deltaX) > mTouchSlop || Math.abs(deltaY) > mTouchSlop) {
//                            if (mDraggedView.isPressed()) {
//                                mDraggedView.setPressed(false);
//                            }
//                            mShouldMove = true;
//                        }
                        if (mDraggedView.isPressed()) {
                            mDraggedView.setPressed(false);
                        }
                        mShouldMove = true;
                    }
                    if (mShouldMove) {
                        correctDraggedViewLocation(deltaX, deltaY);
                        swapItemIfNeed(ev);
                        scrollIfNeeded();
                    }
                    mLastMotionX = ev.getRawX();
                    mLastMotionY = ev.getRawY();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
//                Debug.stopMethodTracing();
                if (mDraggedView != null) {
                    releaseDraggedView();
                    mScrollRunner.cancel();
                    handled = true;
                }
//                int motionPosition = getMotionPosition();
                mDraggedView = null;
                mCurrMotionEvent = null;
//                super.onTouchEvent(ev);
                break;
        }
        if (isTouchMode()) {
            handled = true;
        }
        boolean result = handled ? handled : super.onTouchEvent(ev);
        return result;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        boolean handled = false;
        if (isLongPressMode() && !isFixedPosition(position)) {
            recordDragViewIfNeeded(view, position);
            handled = true;
        }
        if (mOnItemLongClickListener != null) {
            boolean longClickReturn = mOnItemLongClickListener.onItemLongClick(parent, view, position, id);
            if (!handled) {
                handled = longClickReturn;
            }
        }
        return handled;
    }

    private int getMotionPosition() {
        final int realX = (int) (mCurrMotionEvent.getRawX() - mGridViewVisibleRect.left);
        final int realY = (int) (mCurrMotionEvent.getRawY() - mGridViewVisibleRect.top);
        return pointToPosition(realX, realY);
    }

    private void recordDragViewIfNeeded(View view, int position) {
        measureVisibleRect();
        if (view == null && position == -1) {
            int currDraggedPosition = getMotionPosition();
            if (currDraggedPosition != INVALID_POSITION) {
                recordDragViewIfNeeded(getChildAt(currDraggedPosition - mFirstVisibleFirstItem), currDraggedPosition);
            }
        } else {
            mDraggedPosition = position;
            mDraggedView = view;
            measureDraggedRect();
            mDraggedIndex = mDraggedPosition - mFirstVisibleFirstItem;
            dispatchItemCaptured();
            correctDraggedViewLocation(0, 0);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (!mDrawOnTop) {
            drawSomeThing(canvas);
        }
        super.dispatchDraw(canvas);
        if (mDrawOnTop) {
            drawSomeThing(canvas);
        }
    }

    private void drawSomeThing(Canvas canvas) {
        if (mDrawer != null) {
            canvas.save();
            mDrawer.onDraw(canvas, getWidth(), getHeight());
            canvas.restore();
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void scrollIfNeeded() {
        measureDraggedRect();
        measureVisibleRect();
        if (!isDraggedInGridView()) {
            mScrollRunner.cancel();
        } else if (mDraggedRect.top <= mGridViewVisibleRect.top) {
            boolean canScrollDown = canScrollDown();
            if (canScrollDown && !mScrollRunner.isRunning()) {
                int deltaY = mClipToPadding ? mScrollY : mScrollY - getListPaddingTop();
                final int duration = Math.abs(deltaY) * 1000 / mScrollSpeed;
                mScrollRunner.start(0, deltaY, duration);
            }
        } else if (mDraggedRect.bottom >= mGridViewVisibleRect.bottom) {
            boolean canScrollUp = canScrollUp();
            if (canScrollUp && !mScrollRunner.isRunning()) {
                int deltaY = mClipToPadding ? getTotalScrollY() + mScrollY : getTotalScrollY() + mScrollY + getListPaddingBottom();
                final int duration = Math.abs(deltaY) * 1000 / mScrollSpeed;
                mScrollRunner.start(0, deltaY, duration);
            }
        } else {
            mScrollRunner.cancel();
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void scrollListBy(int deltaY) {
        if (SdkVerUtils.isAbove19()) {
            super.scrollListBy(deltaY);
        } else {
            ReflectUtil.invokeMethod(this, "trackMotionScroll", new Object[]{-deltaY, -deltaY}, new Class[]{int.class, int.class});
        }
    }

    @Override
    public void onMove(int lastX, int lastY, int curX, int curY) {
        int deltaY = curY - lastY;
        mDraggedView.offsetTopAndBottom(deltaY);
        scrollListBy(deltaY);
        swapItemIfNeed(mCurrMotionEvent);
    }

    @Override
    public void onDone() {
    }

    /**
     * detact whether the content of gridview can scroll down.
     *
     * @return
     */
    public boolean canScrollDown() {
        final int threshold = mClipToPadding ? 0 : getListPaddingTop();
        if (mScrollY < threshold) {
            return true;
        }
        return false;
    }

    /**
     * detact whether the content of gridview can scroll up.
     *
     * @return
     */
    public boolean canScrollUp() {
        final int threshold = mClipToPadding ? -mScrollY : getListPaddingBottom();
        if (getTotalScrollY() > -threshold) {
            return true;
        }
        return false;
    }

    public int getTotalScrollY() {
        if (mAdapter == null) return 0;
        int row = (mAdapter.getCount() - 1) / mColumnsNum + 1;
        int total = row * mRowHeight + (row - 1) * mVerticalSpacing;
        return total - getHeight();
    }

    private boolean isDraggedInGridView() {
        return mGridViewVisibleRect.intersects(mDraggedRect.left, mDraggedRect.top, mDraggedRect.right, mDraggedRect.bottom);
    }

    private void measureDraggedRect() {
        mDraggedView.getGlobalVisibleRect(mDraggedRect);
        int location[] = new int[2];
        mDraggedView.getLocationOnScreen(location);
        mDraggedRect.set(location[0], location[1], location[0] + mDraggedRect.width(), location[1] + mDraggedRect.height());
    }

    private void measureVisibleRect() {
        if (mGridViewVisibleRect == null) {
            mGridViewVisibleRect = new Rect();
            getGlobalVisibleRect(mGridViewVisibleRect);
            int location[] = new int[2];
            getLocationOnScreen(location);
            mGridViewVisibleRect.set(location[0], location[1], location[0] + mGridViewVisibleRect.width(),
                    location[1] + mGridViewVisibleRect.height());
        }
    }

    private boolean isFixedPosition(int position) {
        if (position != INVALID_POSITION && mAdapter instanceof OnItemMovedListener) {
            mItemMovedListener = (OnItemMovedListener) mAdapter;
            if (mItemMovedListener.isFixed(position)) {
                return true;
            }
            return false;
        }
        return false;
    }

    private void swapItemIfNeed(MotionEvent ev) {
        if (ev == null || mDraggedView == null || isFixedPosition(mDraggedPosition)) return;
        measureVisibleRect();
        measureDraggedRect();
        final int realX = (int) (ev.getRawX() - mGridViewVisibleRect.left);
        final int realY = (int) (ev.getRawY() - mGridViewVisibleRect.top);
        int currDraggedPosition = pointToPosition(realX, realY);
        boolean intersect = isDraggedInGridView();
        int draggedViewPosition = INVALID_POSITION;
        if (currDraggedPosition != INVALID_POSITION) {
            if (intersect) {
                draggedViewPosition = currDraggedPosition;
            } else {
                draggedViewPosition = INVALID_POSITION;
            }
        }
        if (isFixedPosition(draggedViewPosition)) {
            draggedViewPosition = INVALID_POSITION;
        }
        if (draggedViewPosition != INVALID_POSITION) {
            int dragPosition = getDragPosition();
            if (draggedViewPosition != dragPosition) {
                measureDraggedRect();
                if (draggedViewPosition < dragPosition) {
                    for (int i = dragPosition - 1; i >= draggedViewPosition; i--) {
                        swapItem(i, i + 1);
                    }
                } else {
                    for (int i = dragPosition + 1; i <= draggedViewPosition; i++) {
                        swapItem(i, i - 1);
                    }
                }
                moveViewToPosition(draggedViewPosition, mDraggedView);
                mDraggedPosition = draggedViewPosition;
            }
        }
    }

    private void swapItem(int from, int to) {
        int fromIndex = from - getFirstVisiblePosition();
        Child fromChild = mChildren.get(fromIndex);
        final View fromView = fromChild.view;
        if (fromChild == null || fromView == null) return;
        fromChild.moveTo(from, to);
        moveViewToPosition(to, fromView);
        dispatchItemMoved(from, to);
        detachViewFromParent(fromView);
        super.attachViewToParent(fromView, to - getFirstVisiblePosition(), fromView.getLayoutParams());
    }

    private void dispatchItemMoved(int from, int to) {
        if (mItemMovedListener != null) {
            mItemMovedListener.onItemMoved(from, to);
        }
    }

    private void moveViewToPosition(int position, View view) {
        boolean removeResult = removeChild(view);
        addChild(getIndex(position), view);
    }

    private int getIndex(int position) {
        return position - mFirstVisibleFirstItem;
    }

    public int[] getLeftAndTopForPosition(int position) {
        int[] lt = new int[2];
        int m = position % mColumnsNum;
        int n = position / mColumnsNum;
        int left = mFirstLeft + m * (mColumnWidth + mHorizontalSpacing);
        int top = mScrollY + n * (mRowHeight + mVerticalSpacing);
        lt[0] = left;
        lt[1] = top;
        return lt;
    }

    @Override
    public View getChildAt(int index) {
        int position = index;
        final int childCount = getChildCount();
        if (mDraggedView != null) {
            int dragIndex = mDraggedPosition - mFirstVisibleFirstItem;
            if (dragIndex == 0) {
                if (index == 0) {
                    position = 1;
                } else if (position == 1) {
                    position = 0;
                } else {
//                position = position;
                }

            } else if (dragIndex == childCount - 1) {
                if (childCount % mColumnsNum != 1) {
                    if (index == childCount - 1) {
                        position = index - 1;
                    } else if (index == childCount - 2) {
                        position = childCount - 1;
                    }
                }
            }
        }
        if (position >= getChildCount()) {
            position = getChildCount() - 1;
        }
        return super.getChildAt(position);
    }

    @Override
    public int getChildCount() {
        return super.getChildCount();
    }

    @Override
    public int pointToPosition(int x, int y) {
        if (mColumnWidth + mHorizontalSpacing <= 0 || mRowHeight + mVerticalSpacing == 0) {
            return INVALID_POSITION;
        }
        int m = (x - mFirstLeft) / (mColumnWidth + mHorizontalSpacing);
        int n = (y - mFirstTop) / (mRowHeight + mVerticalSpacing);
        int right = mFirstLeft + (m + 1) * (mColumnWidth + mHorizontalSpacing);
        int bottom = mFirstTop + (n + 1) * (mRowHeight + mVerticalSpacing) + mRowHeight;
        if (x > right || y > bottom || m >= mColumnsNum) {
            return INVALID_POSITION;
        } else {
            int result = n * mColumnsNum + m + mFirstVisibleFirstItem;
            result = result <= getLastVisiblePosition() ? result : INVALID_POSITION;
            return result;
        }
    }

    private void log(String msg) {
        Log.e("moveOnGridView", msg);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    /**
     * Let our finger touch the center area of  draggedView.
     */
    private void correctDraggedViewLocation(int deltaX, int deltaY) {
        if (mCurrMotionEvent == null) return;
        float motionX = mCurrMotionEvent.getRawX();
        float motionY = mCurrMotionEvent.getRawY();
        measureVisibleRect();
        deltaX = (int) (motionX - mGridViewVisibleRect.left - (mDraggedView.getLeft() + mColumnWidth / 2)) + deltaX;
        deltaY = (int) (motionY - mGridViewVisibleRect.top - (mDraggedView.getTop() + mRowHeight / 2)) + deltaY;
        if (!isFixedPosition(mDraggedPosition)) {
            mDraggedView.offsetLeftAndRight(deltaX);
            mDraggedView.offsetTopAndBottom(deltaY);
        }

    }

    private void dispatchItemCaptured() {
        if (mItemCapturedListener != null && !isFixedPosition(mDraggedPosition)) {
            mItemCapturedListener.onItemCaptured(mDraggedView, mDraggedPosition);
        }
    }

    private void releaseDraggedView() {
        int[] destination = getLeftAndTopForPosition(mDraggedPosition);
        int offsetX = destination[0] - mDraggedView.getLeft();
        int offsetY = destination[1] - mDraggedView.getTop();
        mDraggedView.offsetLeftAndRight(offsetX);
        mDraggedView.offsetTopAndBottom(offsetY);
        dispatchItemReleased();
        if (mDraggedView.isPressed()) {
            mDraggedView.setPressed(false);
        }
    }

    private void dispatchItemReleased() {
        if (mItemCapturedListener != null && !isFixedPosition(mDraggedPosition)) {
            mItemCapturedListener.onItemReleased(mDraggedView, mDraggedPosition);
        }
    }

    private OnItemCapturedListener mItemCapturedListener;

    public void setOnItemCapturedListener(OnItemCapturedListener listener) {
        mItemCapturedListener = listener;
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        int order = i;
        if (mDraggedView != null) {
            mDraggedIndex = mDraggedPosition - mFirstVisibleFirstItem;
            if (i == mDraggedIndex) {
                order = childCount - 1;
            } else if (i == childCount - 1) {
                order = mDraggedIndex;
            }
        }
        return order;
    }

    public MODE getMode() {
        return mode;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemClick(parent, view, position, id);
        }
    }

    public enum MODE {
        TOUCH, LONG_PRESS, NONE;

        public static int indexOf(MODE mode) {
            int index = -1;
            for (MODE mode2 : MODE.values()) {
                index++;
                if (mode == mode2) {
                    break;
                }
            }
            return index;
        }

        public static MODE get(int index) {
            int i = 0;
            for (MODE mode : MODE.values()) {
                if (i == index) {
                    return mode;
                }
                i++;
            }
            return null;
        }

        @Override
        public String toString() {
            String name = name();
            switch (name) {
                case "TOUCH":
                    return "编辑模式";
                case "LONG_PRESS":
                    return "长按拖拽模式";
                case "NONE":
                    return "普通模式";
            }
            return super.toString();
        }
    }
}
