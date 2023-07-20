package cc.hicore.hook.stickerPanel.MainItemImpl;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import cc.hicore.Utils.ContextUtils;
import cc.hicore.Utils.DataUtils;
import cc.hicore.Utils.FileUtils;
import cc.hicore.Utils.RandomUtils;
import cc.hicore.hook.stickerPanel.ICreator;
import cc.hicore.hook.stickerPanel.LocalDataHelper;
import cc.hicore.hook.stickerPanel.MainPanelAdapter;
import cc.ioctl.util.ui.FaultyDialog;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.impl.LoadingPopupView;
import io.github.qauxv.R;
import io.github.qauxv.ui.CommonContextWrapper;
import io.github.qauxv.util.SyncUtils;
import java.io.File;
import java.util.List;

public class InputFromLocalImpl implements MainPanelAdapter.IMainPanelItem {

    @Override
    public View getView(ViewGroup parent) {
        ViewGroup vgInput = (ViewGroup) View.inflate(parent.getContext(), R.layout.sticker_panel_impl_input_from_local, null);
        EditText edPath = vgInput.findViewById(R.id.input_path);
        Button btnPath = vgInput.findViewById(R.id.btn_confirm_input);
        btnPath.setOnClickListener(v -> {
            String path = edPath.getText().toString();
            File[] f = new File(path).listFiles();
            if (f == null) {
                FaultyDialog.show(parent.getContext(), "错误", "路径无效");
            } else {
                EditText ed = new EditText(parent.getContext());
                new AlertDialog.Builder(CommonContextWrapper.createAppCompatContext(parent.getContext()))
                        .setTitle("输入分组名称")
                        .setView(ed)
                        .setPositiveButton("确定导入", (dialog, which) -> inputWorker(parent.getContext(), path, ed.getText().toString()))
                        .setNeutralButton("取消", null)
                        .show();
            }

        });
        return vgInput;
    }

    private static void inputWorker(Context context, String path, String name) {
        if (TextUtils.isEmpty(name)) {
            FaultyDialog.show(context, "错误", "名称不能为空");
            return;
        }
        LoadingPopupView progress = new XPopup.Builder(ContextUtils.getFixContext(CommonContextWrapper.createAppCompatContext(context)))
                .dismissOnBackPressed(false)
                .dismissOnTouchOutside(false)
                .asLoading("正在导入...");

        progress.show();

        SyncUtils.async(() -> {
            File[] f = new File(path).listFiles();
            if (f == null) {
                SyncUtils.runOnUiThread(() -> {
                    progress.dismiss();
                    FaultyDialog.show(context, "错误", "路径无效");
                });
                return;
            }
            String ID = RandomUtils.getRandomString(8);
            LocalDataHelper.LocalPath newPath = new LocalDataHelper.LocalPath();
            newPath.storePath = ID;
            newPath.coverName = "";
            newPath.Name = name;
            LocalDataHelper.addPath(newPath);

            int size = f.length;
            int finish = 0;
            int available = 0;
            for (File file : f) {
                if (file.isFile()) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                    if (options.outWidth > 0 && options.outHeight > 0) {
                        LocalDataHelper.LocalPicItems localItem = new LocalDataHelper.LocalPicItems();
                        localItem.url = "";
                        localItem.type = 1;
                        localItem.MD5 = DataUtils.getFileMD5(file);
                        localItem.addTime = System.currentTimeMillis();
                        localItem.fileName = localItem.MD5;
                        localItem.thumbUrl = "";

                        FileUtils.copy(file.getAbsolutePath(), LocalDataHelper.getLocalItemPath(newPath, localItem));

                        LocalDataHelper.addPicItem(ID, localItem);
                        available++;
                    }
                }
                finish++;
                int finalFinish = finish;
                int finalAvailable = available;
                SyncUtils.runOnUiThread(() -> progress.setTitle("已完成" + finalFinish + "/" + size + "个文件，有效文件" + finalAvailable + "个"));
            }

            List<LocalDataHelper.LocalPicItems> list = LocalDataHelper.getPicItems(ID);
            if (list.size() > 0) {
                LocalDataHelper.setPathCover(newPath, list.get(0));
            }
            SyncUtils.runOnUiThread(() -> {
                progress.dismiss();
                FaultyDialog.show(context, "导入完成", "导入完成");
                ICreator.dismissAll();
            });
        });
    }

    @Override
    public void onViewDestroy(ViewGroup parent) {

    }

    @Override
    public long getID() {
        return 8888;
    }

    @Override
    public void notifyViewUpdate0() {

    }
}
