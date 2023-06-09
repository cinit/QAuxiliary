package cc.hicore.hook.stickerPanel;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import java.util.ArrayList;

public class MainPanelAdapter extends BaseAdapter {
    ArrayList<IMainPanelItem> viewData = new ArrayList<>();
    volatile long timeStart;

    @Override
    public int getCount() {
        return viewData.size();
    }

    public int addItemData(IMainPanelItem item) {
        viewData.add(item);
        return viewData.size() - 1;
    }

    @Override
    public Object getItem(int position) {
        return viewData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return viewData.get(position).getID();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView != null) {
            Object tag = convertView.getTag();
            if (tag instanceof IMainPanelItem) {
                IMainPanelItem item = (IMainPanelItem) tag;
                item.onViewDestroy(parent);
            }
        }
        View retView = viewData.get(position).getView(parent);
        retView.setTag(getItem(position));
        return retView;
    }

    public void destroyAllViews() {
        for (IMainPanelItem item : viewData) {
            item.onViewDestroy(null);
        }
    }

    public void notifyViewUpdate(int first, int last) {
        if (System.currentTimeMillis() - timeStart > 150) {
            timeStart = System.currentTimeMillis();
            for (int i = first; i < last + 1; i++) {
                IMainPanelItem item = viewData.get(i);
                item.notifyViewUpdate0();
            }
        }

    }
    public interface IMainPanelItem {
        View getView(ViewGroup parent);

        void onViewDestroy(ViewGroup parent);

        long getID();

        void notifyViewUpdate0();
    }

}
