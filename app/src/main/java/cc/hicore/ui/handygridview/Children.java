package cc.hicore.ui.handygridview;

import android.view.View;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class Children {
    private LinkedHashMap<View, Child> container = new LinkedHashMap<>();
    private LinkedList<Child> mChildren = new LinkedList<>();
    private HandyGridView parent;

    public Children(HandyGridView parent) {
        this.parent = parent;
    }

    public void add(int index, View view) {
        Child child = container.get(view);
        if (child == null) {
            child = new Child(view);
            child.setParent(parent);
            container.put(view, child);
        }
        mChildren.add(index, child);
    }

    public boolean remove(Child child) {
        return mChildren.remove(child);
    }

    public void remove(int index) {
        mChildren.remove(index);
    }

    public Child get(int index) {
        return mChildren.get(index);
    }

    public int indexOf(View v) {
        Child child = container.get(v);
        if (child == null) {
            return -2;
        }
        return mChildren.indexOf(child);
    }

    public int size() {
        return mChildren.size();
    }

    public void clear() {
        container.clear();
        Iterator<Child> it = mChildren.iterator();
        //子view从gridView移除时取消动画效果
        while (it.hasNext()) {
            Child child = it.next();
            child.cancel();
            it.remove();
        }
    }
}
