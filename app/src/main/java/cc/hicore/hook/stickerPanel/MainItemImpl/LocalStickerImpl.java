package cc.hicore.hook.stickerPanel.MainItemImpl;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import cc.hicore.Env;
import cc.hicore.Utils.HttpUtils;
import cc.hicore.hook.stickerPanel.ICreator;
import cc.hicore.hook.stickerPanel.LocalDataHelper;
import cc.hicore.hook.stickerPanel.MainPanelAdapter;
import cc.hicore.hook.stickerPanel.RecentStickerHelper;
import cc.hicore.message.chat.SessionUtils;
import cc.hicore.message.common.MsgSender;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.LayoutHelper;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import de.robv.android.xposed.XposedBridge;
import io.github.qauxv.R;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.Toasts;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class LocalStickerImpl implements MainPanelAdapter.IMainPanelItem {
    ViewGroup cacheView;
    Context mContext;
    LinearLayout panelContainer;
    HashSet<ViewInfo> cacheImageView = new HashSet<>();
    TextView tv_title;
    LocalDataHelper.LocalPath mPathInfo;
    List<LocalDataHelper.LocalPicItems> mPicItems;

    public LocalStickerImpl(LocalDataHelper.LocalPath pathInfo, List<LocalDataHelper.LocalPicItems> picItems, Context mContext) {
        mPathInfo = pathInfo;
        mPicItems = picItems;
        this.mContext = mContext;


        cacheView = (ViewGroup) View.inflate(mContext, R.layout.sticker_panel_plus_pack_item, null);
        tv_title = cacheView.findViewById(R.id.Sticker_Panel_Item_Name);
        panelContainer = cacheView.findViewById(R.id.Sticker_Item_Container);
        tv_title.setText(mPathInfo.Name);

        View setButton = cacheView.findViewById(R.id.Sticker_Panel_Set_Item);
        setButton.setOnClickListener(v -> onSetButtonClick());

        try {
            LinearLayout itemLine = null;
            for (int i = 0; i < mPicItems.size(); i++) {
                LocalDataHelper.LocalPicItems item = mPicItems.get(i);
                if (i % 5 == 0) {
                    itemLine = new LinearLayout(mContext);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.bottomMargin = (int) LayoutHelper.dip2px(mContext, 16);
                    panelContainer.addView(itemLine, params);
                }
                if (item.type == 2) {
                    itemLine.addView(getItemContainer(mContext, item.url, i % 5, item));
                } else if (item.type == 1) {
                    itemLine.addView(getItemContainer(mContext, LocalDataHelper.getLocalItemPath(mPathInfo, item), i % 5, item));
                }

            }
        } catch (Exception e) {
            XposedBridge.log(Log.getStackTraceString(e));
        }
    }

    private void onSetButtonClick() {
        new AlertDialog.Builder(mContext, 3)
                .setTitle("选择你的操作").setItems(new String[]{
                        "删除该表情包", "表情包本地化"
                }, (dialog, which) -> {
                    if (which == 0) {
                        new AlertDialog.Builder(mContext, 3)
                                .setTitle("提示")
                                .setMessage("是否删除该表情包(" + tv_title.getText() + "),该表情包内的本地表情将被删除并不可恢复")
                                .setNeutralButton("确定删除", (dialog1, which1) -> {
                                    LocalDataHelper.deletePath(mPathInfo);
                                    ICreator.dismissAll();
                                })
                                .setNegativeButton("取消", (dialog12, which12) -> {

                                }).show();
                    } else if (which == 1) {
                        updateAllResToLocal();
                    }
                }).show();
    }

    private void updateAllResToLocal() {
        ProgressDialog progressDialog = new ProgressDialog(mContext, 3);
        progressDialog.setTitle("正在更新表情包");
        progressDialog.setMessage("正在更新表情包,请稍等...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        new Thread(() -> {
            try {
                ExecutorService threadPool = Executors.newFixedThreadPool(8);
                AtomicInteger finishCount = new AtomicInteger();
                int taskCount = mPicItems.size();
                for (LocalDataHelper.LocalPicItems item : mPicItems) {
                    threadPool.execute(() -> {
                        try {
                            if (item.url.startsWith("http")) {
                                String localStorePath = LocalDataHelper.getLocalItemPath(mPathInfo, item);
                                if (!TextUtils.isEmpty(localStorePath)) {
                                    if (cc.hicore.Utils.HttpUtils.DownloadToFile(item.url, localStorePath)){
                                        item.type = 1;
                                        item.fileName = item.MD5;


                                        if (!TextUtils.isEmpty(item.thumbUrl)) {
                                            String localThumbPath = LocalDataHelper.getLocalThumbPath(mPathInfo, item);
                                            HttpUtils.DownloadToFile(item.thumbUrl, localThumbPath);
                                            item.thumbName = item.MD5 + "_thumb";
                                        }
                                        LocalDataHelper.updatePicItemInfo(mPathInfo, item);
                                    }


                                }
                            }
                        } catch (Exception e) {
                            XposedBridge.log(Log.getStackTraceString(e));
                        } finally {
                            SyncUtils.runOnUiThread(() -> progressDialog.setMessage("正在更新表情包,请稍等...(" + finishCount.getAndIncrement() + "/" + mPicItems.size() + ")"));
                        }
                    });
                }
                while (true) {
                    if (finishCount.get() == taskCount) {
                        break;
                    }
                    Thread.sleep(100);
                }
                SyncUtils.runOnUiThread(progressDialog::dismiss);
                Toasts.info(mContext,"已更新完成");
                SyncUtils.runOnUiThread(ICreator::dismissAll);

            } catch (Exception e) {
            }
        }).start();

    }

    @Override
    public View getView(ViewGroup parent) {
        onViewDestroy(null);
        return cacheView;
    }

    private View getItemContainer(Context context, String coverView, int count, LocalDataHelper.LocalPicItems item) {
        int width_item = LayoutHelper.getScreenWidth(context) / 6;
        int item_distance = (LayoutHelper.getScreenWidth(context) - width_item * 5) / 4;

        ImageView img = new ImageView(context);
        ViewInfo info = new ViewInfo();
        info.view = img;
        info.status = 0;

        cacheImageView.add(info);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width_item, width_item);
        if (count > 0) params.leftMargin = item_distance;
        img.setLayoutParams(params);

        img.setTag(coverView);
        img.setOnClickListener(v -> {
            if (coverView.startsWith("http://") || coverView.startsWith("https://")) {
                HttpUtils.ProgressDownload(coverView, Env.app_save_path + "Cache/" + coverView.substring(coverView.lastIndexOf("/")), () -> {
                    MsgSender.send_pic(SessionUtils.getCurrentSession(), Env.app_save_path + "Cache/" + coverView.substring(coverView.lastIndexOf("/")));
                    RecentStickerHelper.addPicItemToRecentRecord(mPathInfo, item);
                }, mContext);
                ICreator.dismissAll();

            } else {
                MsgSender.send_pic(SessionUtils.getCurrentSession(),
                        LocalDataHelper.getLocalItemPath(mPathInfo, item));
                RecentStickerHelper.addPicItemToRecentRecord(mPathInfo, item);
                ICreator.dismissAll();
            }
        });

        img.setOnLongClickListener(v -> {
            ImageView preView = new ImageView(context);
            preView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            preView.setLayoutParams(new ViewGroup.LayoutParams(LayoutHelper.getScreenWidth(v.getContext()) / 2, LayoutHelper.getScreenWidth(v.getContext()) / 2));
            if (coverView.startsWith("http://") || coverView.startsWith("https://")) {
                try {
                    Glide.with(HostInfo.getApplication()).load(new URL(coverView)).override(LayoutHelper.getScreenWidth(v.getContext()) / 2, LayoutHelper.getScreenWidth(v.getContext()) / 2).into(preView);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            } else {
                Glide.with(HostInfo.getApplication()).load(coverView).fitCenter().diskCacheStrategy(DiskCacheStrategy.RESOURCE).override(LayoutHelper.getScreenWidth(v.getContext()) / 2, LayoutHelper.getScreenWidth(v.getContext()) / 2).into(preView);
            }
            new AlertDialog.Builder(mContext, 3)
                    .setTitle("选择你对该表情的操作")
                    .setView(preView)
                    .setOnDismissListener(dialog -> {
                        Glide.with(HostInfo.getApplication()).clear(preView);
                    }).setNegativeButton("删除该表情", (dialog, which) -> {
                        LocalDataHelper.deletePicItem(mPathInfo, item);
                        ICreator.dismissAll();
                    }).setNeutralButton("设置为标题预览", (dialog, which) -> {
                        LocalDataHelper.setPathCover(mPathInfo, item);
                        ICreator.dismissAll();
                    }).show();
            return true;
        });

        return img;

    }

    @Override
    public void onViewDestroy(ViewGroup parent) {
        for (ViewInfo img : cacheImageView) {
            img.view.setImageBitmap(null);
            Glide.with(HostInfo.getApplication()).clear(img.view);
        }
    }

    @Override
    public long getID() {
        return 0;
    }

    @Override
    public void notifyViewUpdate0() {
        for (ViewInfo v : cacheImageView) {
            if (LayoutHelper.isSmallWindowNeedPlay(v.view)) {
                if (v.status != 1) {
                    v.status = 1;
                    int width_item = LayoutHelper.getScreenWidth(mContext) / 6;
                    String coverView = (String) v.view.getTag();
                    try {
                        if (coverView.startsWith("http://") || coverView.startsWith("https://")) {
                            Glide.with(HostInfo.getApplication()).load(new URL(coverView)).override(width_item, width_item).into(v.view);
                        } else {
                            if(new File(coverView + "_thumb").exists()){
                                Glide.with(HostInfo.getApplication()).load(coverView + "_thumb").fitCenter().diskCacheStrategy(DiskCacheStrategy.RESOURCE).override(width_item, width_item).into(v.view);
                            }else {
                                Glide.with(HostInfo.getApplication()).load(coverView).fitCenter().diskCacheStrategy(DiskCacheStrategy.RESOURCE).override(width_item, width_item).into(v.view);
                            }

                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }

            } else {
                if (v.status != 0) {
                    Glide.with(HostInfo.getApplication()).clear(v.view);
                    v.status = 0;
                }


            }
        }
    }

    public static class ViewInfo {
        ImageView view;
        volatile int status = 0;
    }


}
