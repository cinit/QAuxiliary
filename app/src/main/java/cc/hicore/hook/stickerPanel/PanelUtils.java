package cc.hicore.hook.stickerPanel;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import cc.hicore.Env;
import cc.hicore.Utils.FileUtils;
import cc.hicore.Utils.RandomUtils;
import cc.ioctl.util.HostInfo;
import com.bumptech.glide.Glide;
import io.github.qauxv.R;
import io.github.qauxv.util.Toasts;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PanelUtils {

    static LocalDataHelper.LocalPath choicePath;

    //显示确认保存的对话框
    public static void PreSavePicToList(String URL, String MD5, Context context) {
        choicePath = null;
        LayoutInflater inflater = LayoutInflater.from(context);
        LinearLayout mRoot = (LinearLayout) inflater.inflate(R.layout.sticker_pre_save, null);
        ImageView preView = mRoot.findViewById(R.id.emo_pre_container);
        preView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        EmoPanel.EmoInfo NewInfo = new EmoPanel.EmoInfo();
        NewInfo.URL = URL;
        NewInfo.type = 2;
        NewInfo.MD5 = MD5.toUpperCase(Locale.ROOT);

        if (URL.startsWith("http")) {
            EmoOnlineLoader.submit(NewInfo, () -> {
                Glide.with(HostInfo.getApplication())
                        .load(new File(NewInfo.Path))
                        .fitCenter()
                        .into(preView);
            });
        } else {
            NewInfo.Path = URL;
            Glide.with(HostInfo.getApplication())
                    .load(new File(NewInfo.Path))
                    .fitCenter()
                    .into(preView);
        }

        List<LocalDataHelper.LocalPath> paths = LocalDataHelper.readPaths();

        RadioGroup group = mRoot.findViewById(R.id.emo_pre_list_choser);
        for (LocalDataHelper.LocalPath path : paths) {
            RadioButton button = new RadioButton(context);
            button.setText(path.Name);
            button.setTextSize(16);
            button.setTextColor(context.getResources().getColor(R.color.global_font_color, null));
            button.setOnCheckedChangeListener((v, ischeck) -> {
                if (v.isPressed() && ischeck) {
                    choicePath = path;
                }
            });
            group.addView(button);
        }
        //新建列表按钮
        Button btnCreate = mRoot.findViewById(R.id.createNew);
        btnCreate.setOnClickListener(v -> {
            EditText edNew = new EditText(context);
            new AlertDialog.Builder(context)
                    .setTitle("创建新目录")
                    .setView(edNew)
                    .setNeutralButton("确定创建", (dialog, which) -> {
                        String newName = edNew.getText().toString();
                        if (TextUtils.isEmpty(newName)) {
                            Toasts.show("名字不能为空");
                            return;
                        }
                        LocalDataHelper.LocalPath path = new LocalDataHelper.LocalPath();
                        path.Name = newName;
                        path.storePath = RandomUtils.getRandomString(16);
                        LocalDataHelper.addPath(path);

                        List<LocalDataHelper.LocalPath> allPaths = LocalDataHelper.readPaths();
                        group.removeAllViews();
                        //确认添加列表后会重新扫描列表并显示
                        for (LocalDataHelper.LocalPath pathItem : allPaths) {
                            RadioButton button = new RadioButton(context);
                            button.setText(pathItem.Name);
                            button.setTextSize(16);
                            button.setTextColor(context.getResources().getColor(R.color.global_font_color, null));
                            button.setOnCheckedChangeListener((vaa, ischeck) -> {
                                if (vaa.isPressed() && ischeck) {
                                    choicePath = pathItem;
                                }
                            });
                            group.addView(button);
                        }
                    })
                    .show();

        });

        new AlertDialog.Builder(context)
                .setTitle("是否保存")
                .setView(mRoot)
                .setNegativeButton("保存", (dialog, which) -> {
                    if (choicePath == null) {
                        Toasts.show("没有选择任何的保存列表");
                    } else if (TextUtils.isEmpty(NewInfo.Path)) {
                        Toasts.show("图片尚未加载完毕,保存失败");
                    } else {
                        FileUtils.copy(NewInfo.Path, Env.app_save_path + "本地表情包/" + choicePath.storePath + "/" + MD5);
                        LocalDataHelper.LocalPicItems item = new LocalDataHelper.LocalPicItems();
                        item.MD5 = MD5;
                        item.fileName = MD5;
                        item.addTime = System.currentTimeMillis();
                        LocalDataHelper.addPicItem(choicePath.storePath, item);


                        Toasts.show("已保存到:" + Env.app_save_path + "本地表情包/" + choicePath.storePath + "/" + MD5);
                    }
                }).setOnDismissListener(dialog -> {
                    Glide.with(HostInfo.getApplication()).clear(preView);
                }).show();
    }

    //如果要保存的是多张图片则弹出MD5选择,选择后才弹出确认图片保存框
    public static void PreSaveMultiPicList(ArrayList<String> url, ArrayList<String> MD5, Context context) {
        new AlertDialog.Builder(context)
                .setTitle("选择需要保存的图片")
                .setItems(MD5.toArray(new String[0]), (dialog, which) -> {
                    PreSavePicToList(url.get(which), MD5.get(which), context);
                }).setOnDismissListener(dialog -> {

                }).show();
    }
}
