package cc.hicore.hook.stickerPanel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import cc.hicore.Env;
import cc.hicore.Utils.Async;
import cc.hicore.Utils.FunConf;
import cc.hicore.hook.stickerPanel.MainItemImpl.InputFromLocalImpl;
import cc.hicore.hook.stickerPanel.MainItemImpl.LocalStickerImpl;
import cc.hicore.hook.stickerPanel.MainItemImpl.PanelSetImpl;
import cc.hicore.hook.stickerPanel.MainItemImpl.RecentStickerImpl;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.LayoutHelper;
import com.bumptech.glide.Glide;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;
import com.lxj.xpopup.core.BottomPopupView;
import com.lxj.xpopup.util.XPopupUtils;
import io.github.qauxv.R;
import io.github.qauxv.ui.CommonContextWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@SuppressLint("ResourceType")
public class ICreator extends BottomPopupView{
    private static BasePopupView popupView;
    LinearLayout topSelectBar;
    private ScrollView itemContainer;
    private IMainPanelItem currentTab;

    ArrayList<ViewGroup> mItems = new ArrayList<>();

    private boolean open_last_select;
    private static long savedSelectID;
    private static long lastSelectTime;
    private static int savedScrollTo;

    private ViewGroup recentUse;

    public ICreator(@NonNull Context context) {
        super(context);
    }

    public static void createPanel(Context context) {
        Context fixContext = CommonContextWrapper.createAppCompatContext(context);
        XPopup.Builder NewPop = new XPopup.Builder(fixContext).moveUpToKeyboard(false).isDestroyOnDismiss(true);
        popupView = NewPop.asCustom(new ICreator(fixContext));
        popupView.show();
    }

    public static void dismissAll() {
        if (popupView != null) {
            popupView.dismiss();
        }
    }

    private void initTopSelectBar() {
        topSelectBar = findViewById(R.id.Sticker_Pack_Select_Bar);
    }

    long scrollTime = 0;
    private void initListView() {
        itemContainer = findViewById(R.id.sticker_panel_pack_container);

        itemContainer.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (System.currentTimeMillis() - scrollTime > 20){
                currentTab.notifyViewUpdate0();
                scrollTime = System.currentTimeMillis();
            }
        });
    }

    private void initStickerPacks() {
        List<LocalDataHelper.LocalPath> paths = LocalDataHelper.readPaths();
        for (LocalDataHelper.LocalPath path : paths) {
            IMainPanelItem newItem = new LocalStickerImpl(path, getContext());
            AtomicReference<ViewGroup> sItemView = new AtomicReference<>();
            ViewGroup sticker_pack_item = (ViewGroup) createPicImage(path.coverName, path.Name, v -> {
                itemContainer.scrollTo(0, 0);
                switchToItem(sItemView.get());
            }, path);
            sticker_pack_item.setTag(newItem);
            topSelectBar.addView(sticker_pack_item);
            sItemView.set(sticker_pack_item);
        }
    }

    private void initDefItemsBefore() {
        IMainPanelItem newItem = new RecentStickerImpl(getContext());
        AtomicReference<ViewGroup> sItemView = new AtomicReference<>();
        ViewGroup recentUse = (ViewGroup) createPicImage(R.drawable.sticker_panel_recent_icon, "最近使用", v -> switchToItem(sItemView.get()));
        sItemView.set(recentUse);
        recentUse.setTag(newItem);
        topSelectBar.addView(recentUse);
        recentUse.setTag(newItem);

        this.recentUse = recentUse;
    }
    private void switchToItem(ViewGroup item){
        for (ViewGroup i : mItems) {
            IMainPanelItem mItem = (IMainPanelItem) i.getTag();
            mItem.onViewDestroy();
            i.findViewById(887533).setVisibility(GONE);
        }

        currentTab = (IMainPanelItem) item.getTag();
        itemContainer.removeAllViews();
        itemContainer.addView(currentTab.getView());
        item.findViewById(887533).setVisibility(VISIBLE);


        Async.runOnUi(currentTab::notifyViewUpdate0);



    }

    private void initDefItemsLast() {
        IMainPanelItem inputPic = new InputFromLocalImpl(getContext());
        AtomicReference<ViewGroup> sticker_panel_input_view = new AtomicReference<>();
        ViewGroup inputView = (ViewGroup) createPicImage(R.drawable.sticker_panel_input_icon, "导入图片", v -> switchToItem(sticker_panel_input_view.get()));
        sticker_panel_input_view.set(inputView);
        inputView.setTag(inputPic);
        topSelectBar.addView(inputView);

        IMainPanelItem setItem = new PanelSetImpl(getContext());
        AtomicReference<ViewGroup> sticker_panel_set_view = new AtomicReference<>();
        ViewGroup setView = (ViewGroup) createPicImage(R.drawable.sticker_panen_set_button_icon, "设置", v -> switchToItem(sticker_panel_set_view.get()));
        sticker_panel_set_view.set(setView);
        setView.setTag(setItem);
        topSelectBar.addView(setView);
    }

    //创建贴纸包面板的滑动按钮
    private View createPicImage(String imgPath, String title, OnClickListener clickListener, LocalDataHelper.LocalPath path) {
        ImageView img = new ImageView(getContext());
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);

        LinearLayout panel = new LinearLayout(getContext());
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setOnClickListener(clickListener);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutHelper.dip2px(getContext(), 30), LayoutHelper.dip2px(getContext(), 30));
        params.topMargin = LayoutHelper.dip2px(getContext(), 3);
        params.bottomMargin = LayoutHelper.dip2px(getContext(), 3);
        params.leftMargin = LayoutHelper.dip2px(getContext(), 3);
        params.rightMargin = LayoutHelper.dip2px(getContext(), 3);
        img.setLayoutParams(params);
        panel.addView(img);

        TextView titleView = new TextView(getContext());
        titleView.setText(title);
        titleView.setTextColor(getResources().getColor(R.color.global_font_color, null));
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        titleView.setTextSize(10);
        titleView.setSingleLine();
        panel.addView(titleView);


        Glide.with(HostInfo.getApplication()).load(Env.app_save_path + "本地表情包/" + path.storePath + "/" + imgPath).into(img);


        LinearLayout.LayoutParams panel_param = new LinearLayout.LayoutParams(LayoutHelper.dip2px(getContext(), 50), ViewGroup.LayoutParams.WRAP_CONTENT);
        panel_param.leftMargin = LayoutHelper.dip2px(getContext(), 5);
        panel_param.rightMargin = LayoutHelper.dip2px(getContext(), 5);
        panel.setLayoutParams(panel_param);

        View greenTip = new View(getContext());
        greenTip.setBackgroundColor(Color.GREEN);
        panel_param = new LinearLayout.LayoutParams(LayoutHelper.dip2px(getContext(), 30), 50);
        panel_param.leftMargin = LayoutHelper.dip2px(getContext(), 5);
        panel_param.rightMargin = LayoutHelper.dip2px(getContext(), 5);
        greenTip.setLayoutParams(panel_param);
        greenTip.setId(887533);
        greenTip.setVisibility(GONE);
        panel.addView(greenTip);

        mItems.add(panel);
        return panel;
    }

    @SuppressLint("ResourceType")
    private View createPicImage(int resID, String title, OnClickListener clickListener) {
        ImageView img = new ImageView(getContext());
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);

        LinearLayout panel = new LinearLayout(getContext());
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setOnClickListener(clickListener);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutHelper.dip2px(getContext(), 30), LayoutHelper.dip2px(getContext(), 30));
        params.topMargin = LayoutHelper.dip2px(getContext(), 3);
        params.bottomMargin = LayoutHelper.dip2px(getContext(), 3);
        params.leftMargin = LayoutHelper.dip2px(getContext(), 3);
        params.rightMargin = LayoutHelper.dip2px(getContext(), 3);
        img.setLayoutParams(params);
        panel.addView(img);

        TextView titleView = new TextView(getContext());
        titleView.setText(title);
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        titleView.setTextColor(getContext().getColor(R.color.global_font_color));
        titleView.setTextSize(10);
        titleView.setSingleLine();
        panel.addView(titleView);

        Glide.with(HostInfo.getApplication()).load(resID).into(img);

        LinearLayout.LayoutParams panel_param = new LinearLayout.LayoutParams(LayoutHelper.dip2px(getContext(), 50), ViewGroup.LayoutParams.WRAP_CONTENT);
        panel_param.leftMargin = LayoutHelper.dip2px(getContext(), 5);
        panel_param.rightMargin = LayoutHelper.dip2px(getContext(), 5);
        panel.setLayoutParams(panel_param);

        View greenTip = new View(getContext());
        greenTip.setBackgroundColor(Color.GREEN);
        panel_param = new LinearLayout.LayoutParams(LayoutHelper.dip2px(getContext(), 30), 50);
        panel_param.leftMargin = LayoutHelper.dip2px(getContext(), 5);
        panel_param.rightMargin = LayoutHelper.dip2px(getContext(), 5);
        greenTip.setLayoutParams(panel_param);
        greenTip.setId(887533);
        greenTip.setVisibility(GONE);
        panel.addView(greenTip);

        mItems.add(panel);
        return panel;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            initTopSelectBar();


            initListView();


            initDefItemsBefore();


            initStickerPacks();


            initDefItemsLast();

            open_last_select = FunConf.getBoolean("global", "sticker_panel_set_open_last_select", false);
            if (open_last_select) savedSelectID = Long.parseLong(FunConf.getString("global", "sticker_panel_set_last_select", String.valueOf(savedSelectID)));
            if (savedSelectID != 0){
                for (ViewGroup item : mItems){
                    IMainPanelItem iMainPanelItem = (IMainPanelItem) item.getTag();
                    if (iMainPanelItem.getID() == savedSelectID){
                        switchToItem(item);

                        Async.runOnUi(iMainPanelItem::notifyViewUpdate0,100);
                        Async.runOnUi(()-> itemContainer.scrollTo(0,savedScrollTo));
                        break;
                    }
                }
            }else {
                switchToItem(recentUse);
            }

        }, 50);


    }

    @Override
    protected int getMaxHeight() {
        return (int) (XPopupUtils.getScreenHeight(getContext()) * .7f);
    }

    @Override
    protected int getPopupHeight() {
        return (int) (XPopupUtils.getScreenHeight(getContext()) * .7f);
    }

    @Override
    protected void beforeDismiss() {
        if (currentTab != null){
            savedScrollTo = itemContainer.getScrollY();
            savedSelectID = currentTab.getID();
            if (open_last_select) FunConf.setString("global", "sticker_panel_set_last_select",String.valueOf(savedSelectID));
            lastSelectTime = System.currentTimeMillis();
        }
        super.beforeDismiss();
    }

    @Override
    protected void onDismiss() {

        super.onDismiss();
        for (ViewGroup item : mItems){
            IMainPanelItem iMainPanelItem = (IMainPanelItem) item.getTag();
            iMainPanelItem.onViewDestroy();
        }

        Glide.get(HostInfo.getApplication()).clearMemory();
    }

    @Override
    protected int getImplLayoutId() {
        return io.github.qauxv.R.layout.sticker_panel_plus_main;
    }

    public interface IMainPanelItem {
        View getView();

        void onViewDestroy();

        long getID();

        void notifyViewUpdate0();
    }
}
