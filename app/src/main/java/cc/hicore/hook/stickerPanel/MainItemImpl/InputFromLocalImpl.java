package cc.hicore.hook.stickerPanel.MainItemImpl;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import cc.hicore.Utils.DataUtils;
import cc.hicore.Utils.FileUtils;
import cc.hicore.Utils.RandomUtils;
import cc.hicore.hook.stickerPanel.ICreator;
import cc.hicore.hook.stickerPanel.LocalDataHelper;
import com.lxj.xpopup.XPopup;
import io.github.duzhaokun123.util.FilePicker;
import io.github.qauxv.R;
import io.github.qauxv.util.SyncUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import kotlin.Unit;
import org.jetbrains.annotations.Async;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class InputFromLocalImpl implements ICreator.IMainPanelItem {
    private Context mContext;
    public InputFromLocalImpl(Context mContext){
        this.mContext = mContext;
    }
    @Override
    public View getView() {
        ViewGroup vgInput = (ViewGroup) View.inflate(mContext, R.layout.sticker_panel_impl_input_from_local, null);
        EditText edPath = vgInput.findViewById(R.id.input_path);
        edPath.setFocusable(true);
        Button btnPath = vgInput.findViewById(R.id.btn_confirm_input);
        btnPath.setOnClickListener(v->{
            String path = edPath.getText().toString();
            File[] f = new File(path).listFiles();
            if (f == null){
                new AlertDialog.Builder(mContext)
                        .setTitle("错误")
                        .setMessage("路径无效")
                        .setPositiveButton("确定", null)
                        .show();
            }else {
                inputWorker(mContext, path);
                EditText ed = new EditText(mContext);

            }

        });
        Button btnPick = vgInput.findViewById(R.id.btn_pick);
        btnPick.setOnClickListener(v -> {
            FilePicker.INSTANCE.pickDir(mContext, "选择表情包路径", Environment.getExternalStorageDirectory().getAbsolutePath(), path -> {
                edPath.setText(path);
                return Unit.INSTANCE;
            });
        });
        return vgInput;
    }
    private static void inputWorker(Context context,String path){
        List<LocalDataHelper.LocalPath> paths = LocalDataHelper.readPaths();
        ArrayList<String> names = new ArrayList<>();
        for (LocalDataHelper.LocalPath p : paths){
            names.add(p.Name);
        }

        new AlertDialog.Builder(context)
                .setTitle("选择需要导入到的分组")
                .setItems(names.toArray(new String[0]), (dialog, which) -> {
                    String id = paths.get(which).storePath;
                    ProgressDialog progressDialog = new ProgressDialog(context);
                    progressDialog.setTitle("正在导入...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                    SyncUtils.async(() -> {
                        File[] f = new File(path).listFiles();
                        for (File file : f) {
                            if (file.isDirectory()) continue;
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inJustDecodeBounds = true;
                            BitmapFactory.decodeFile(file.getAbsolutePath(),options);
                            if (options.outWidth > 0 && options.outHeight > 0){
                                LocalDataHelper.LocalPicItems localItem = new LocalDataHelper.LocalPicItems();
                                localItem.url = "";
                                localItem.MD5 = DataUtils.getFileMD5(file);
                                localItem.addTime = System.currentTimeMillis();
                                localItem.fileName = localItem.MD5;

                                FileUtils.copy(file.getAbsolutePath(),LocalDataHelper.getLocalItemPath(paths.get(which),localItem));
                                LocalDataHelper.addPicItem(id, localItem);
                            }

                        }
                        SyncUtils.runOnUiThread(progressDialog::dismiss);
                    });
                })
                .setNegativeButton("创建全新的表情包", (dialog, which) -> {
                    EditText editText = new EditText(context);
                    new AlertDialog.Builder(context)
                            .setTitle("输入显示的名字")
                            .setView(editText)
                            .setNegativeButton("确定导入", (dialogaaa, whichaaa) -> {
                                ProgressDialog progressDialog = new ProgressDialog(context);
                                progressDialog.setTitle("正在导入...");
                                progressDialog.setCancelable(false);
                                progressDialog.show();
                                SyncUtils.async(() -> {
                                    File[] f = new File(path).listFiles();
                                    if (f == null){
                                        SyncUtils.runOnUiThread(()->{
                                            progressDialog.dismiss();
                                            new AlertDialog.Builder(context)
                                                    .setTitle("错误")
                                                    .setMessage("路径无效")
                                                    .setPositiveButton("确定", null)
                                                    .show();
                                        });
                                        return;
                                    }
                                    String ID = RandomUtils.getRandomString(8);
                                    LocalDataHelper.LocalPath newPath = new LocalDataHelper.LocalPath();
                                    newPath.storePath = ID;
                                    newPath.coverName = "";
                                    newPath.Name = editText.getText().toString();
                                    LocalDataHelper.addPath(newPath);

                                    int size = f.length;
                                    int finish = 0;
                                    int available = 0;
                                    for (File file : f) {
                                        if (file.isFile()){
                                            BitmapFactory.Options options = new BitmapFactory.Options();
                                            options.inJustDecodeBounds = true;
                                            BitmapFactory.decodeFile(file.getAbsolutePath(),options);
                                            if (options.outWidth > 0 && options.outHeight > 0){
                                                LocalDataHelper.LocalPicItems localItem = new LocalDataHelper.LocalPicItems();
                                                localItem.url = "";
                                                localItem.MD5 = DataUtils.getFileMD5(file);
                                                localItem.addTime = System.currentTimeMillis();
                                                localItem.fileName = localItem.MD5;
                                                localItem.thumbUrl = "";


                                                FileUtils.copy(file.getAbsolutePath(),LocalDataHelper.getLocalItemPath(newPath,localItem));

                                                LocalDataHelper.addPicItem(ID,localItem);
                                                available++;
                                            }
                                        }
                                        finish++;
                                        int finalFinish = finish;
                                        int finalAvailable = available;
                                        SyncUtils.runOnUiThread(()->{
                                            progressDialog.setMessage("已完成"+ finalFinish +"/"+size+"个文件,有效文件"+ finalAvailable +"个");
                                        });
                                    }

                                    List<LocalDataHelper.LocalPicItems> list =  LocalDataHelper.getPicItems(ID);
                                    if (list.size() > 0){
                                        LocalDataHelper.setPathCover(newPath,list.get(0));
                                    }
                                    SyncUtils.runOnUiThread(()->{
                                        progressDialog.dismiss();
                                        new AlertDialog.Builder(context)
                                                .setTitle("导入完成")
                                                .setMessage("导入完成")
                                                .setPositiveButton("确定", null)
                                                .show();

                                        ICreator.dismissAll();
                                    });
                                });
                            }).show();

                }).show();



    }
    private static boolean isImageFile(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        if (options.outWidth == -1) {
            return false;
        }
        return true;
    }

    @Override
    public void onViewDestroy() {

    }

    @Override
    public long getID() {
        return 8888;
    }

    @Override
    public void notifyViewUpdate0() {

    }
}
