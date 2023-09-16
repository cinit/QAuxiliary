package cc.hicore.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import cc.hicore.ui.handygridview.HandyGridView;
import cc.hicore.ui.handygridview.scrollrunner.OnItemMovedListener;
import cc.ioctl.util.LayoutHelper;
import java.util.List;

@SuppressLint("ResourceType")
public class SimpleDragSortView {
    public static SimpleDragSortView createDrag(Context mContext,List<String> localFilePath,List<String> sourceInfo,Runnable onDone){
        SimpleDragSortView dragView = new SimpleDragSortView(mContext,localFilePath,sourceInfo);
        Dialog mDialog = new Dialog(mContext,2);
        mDialog.setContentView(dragView.getView());
        mDialog.setOnDismissListener(dialog -> onDone.run());
        mDialog.show();
        return dragView;
    }
    List<String> localFilePath;
    List<String> sourceInfo;
    private HandyGridView mGridView;
    private Context mContext;
    public SimpleDragSortView(Context context,List<String> localFilePath,List<String> sourceInfo) {
        if (localFilePath.size() == 0)return;
        this.localFilePath = localFilePath;
        this.mContext = context;
        this.sourceInfo = sourceInfo;
        mGridView = new HandyGridView(context);
        mGridView.setNumColumns(5);
        mGridView.setStretchMode(HandyGridView.STRETCH_COLUMN_WIDTH);
        mGridView.setVerticalSpacing(10);
        mGridView.setPadding(10, 10, 10, 10);
        mGridView.setAdapter(new GridViewAdapter(localFilePath,sourceInfo));

    }
    public View getView(){
        return mGridView;
    }

    private static class GridViewAdapter extends BaseAdapter implements OnItemMovedListener {
        private List<String> picPath;
        private List<String> sourceInfo;
        public GridViewAdapter(List<String> picPath,List<String> sourceInfo){
            this.picPath = picPath;
            this.sourceInfo = sourceInfo;
        }

        @Override
        public int getCount() {
            return picPath.size();
        }

        @Override
        public String getItem(int position) {
            return picPath.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int width_item = LayoutHelper.getScreenWidth(parent.getContext()) / 5;
            TextView textView = new TextView(parent.getContext());
            textView.setHeight(width_item);
            textView.setWidth(width_item);
            textView.setBackground(Drawable.createFromPath(picPath.get(position)));
            return textView;
        }

        @Override
        public void onItemMoved(int from, int to) {
            String path = picPath.remove(from);
            picPath.add(to, path);

            String info = sourceInfo.remove(from);
            sourceInfo.add(to, info);
        }

        @Override
        public boolean isFixed(int position) {
            return false;
        }
    }

}
