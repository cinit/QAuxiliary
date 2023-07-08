package cc.hicore.hook.stickerPanel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import cc.hicore.Env;
import cc.hicore.Utils.ContextUtils;
import cc.hicore.hook.stickerPanel.MainItemImpl.InputFromLocalImpl;
import cc.hicore.hook.stickerPanel.MainItemImpl.LocalStickerImpl;
import cc.hicore.hook.stickerPanel.MainItemImpl.RecentStickerImpl;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.LayoutHelper;
import com.bumptech.glide.Glide;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;
import com.lxj.xpopup.core.BottomPopupView;
import com.lxj.xpopup.util.XPopupUtils;
import io.github.qauxv.R;
import io.github.qauxv.lifecycle.Parasitics;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("ResourceType")
public class ICreator extends BottomPopupView implements AbsListView.OnScrollListener {
    private static BasePopupView popupView;
    MainPanelAdapter adapter = new MainPanelAdapter();
    int IdOfShareGroup;
    LinearLayout topSelectBar;
    int myLovePos = 0;
    int recentUsePos = 0;
    int IdOfConvertFromTg;

    int IdOfInputPic;
    private final List<ViewGroup> newTabView = new ArrayList<>();
    private ListView listView;

    public ICreator(@NonNull Context context) {
        super(context);
    }

    public static void createPanel(Context context) {

        Parasitics.injectModuleResources(context.getResources());
        Context fixContext = ContextUtils.getFixContext(context);
        XPopup.Builder NewPop = new XPopup.Builder(fixContext).isDestroyOnDismiss(true);
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

    private void initListView() {
        listView = findViewById(R.id.Sticker_Panel_Main_List_View);
        listView.setAdapter(adapter);
        listView.setVerticalScrollBarEnabled(false);
        listView.setOnScrollListener(this);
    }

    private void initStickerPacks() {
        List<LocalDataHelper.LocalPath> paths = LocalDataHelper.readPaths();
        for (LocalDataHelper.LocalPath path : paths) {
            int localPathPos = adapter.addItemData(new LocalStickerImpl(path, LocalDataHelper.getPicItems(path.storePath), getContext()));
            ViewGroup sticker_pack_item = (ViewGroup) createPicImage(path.coverName, path.Name, v -> {
                listView.setSelection(localPathPos);
                listView.smoothScrollToPositionFromTop(localPathPos, -5);

            }, path);
            sticker_pack_item.setTag(localPathPos);
            topSelectBar.addView(sticker_pack_item);
        }
    }

    private void initDefItemsBefore() {
        /*
        ViewGroup likeTab = (ViewGroup) createPicImage(R.drawable.sticker_like,"收藏表情", v->{
            listView.setSelection(myLovePos);
            listView.smoothScrollToPositionFromTop(myLovePos,-5);
        });
        myLovePos = adapter.addItemData(new MyLoveStickerImpl());
        likeTab.setTag(myLovePos);
        topSelectBar.addView(likeTab);

         */


        ViewGroup recentUse = (ViewGroup) createPicImage(R.drawable.sticker_recent, "最近使用", v -> {
            listView.setSelection(recentUsePos);
            listView.smoothScrollToPositionFromTop(recentUsePos, -5);
        });
        recentUsePos = adapter.addItemData(new RecentStickerImpl(getContext()));
        recentUse.setTag(recentUsePos);
        topSelectBar.addView(recentUse);
    }

    private void initDefItemsLast() {

        ViewGroup inputView = (ViewGroup) createPicImage(R.drawable.input, "导入表情", v -> {
            listView.setSelection(IdOfInputPic);
            listView.smoothScrollToPositionFromTop(IdOfInputPic, -5);
        });
        IdOfInputPic = adapter.addItemData(new InputFromLocalImpl());
        topSelectBar.addView(inputView);


        //topSelectBar.addView(createPicImage(R.drawable.sticker_pack_set_icon,"设置分组",v -> Utils.ShowToast("Click")));
    }

    public void notifyTabViewSelect(ViewGroup vg) {
        for (ViewGroup v : newTabView) {
            v.findViewById(887533).setVisibility(GONE);
        }
        vg.findViewById(887533).setVisibility(VISIBLE);
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
        titleView.setTextColor(0xff888888);
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        titleView.setTextSize(10);
        titleView.setSingleLine();
        panel.addView(titleView);


        if (imgPath.startsWith("http://") || imgPath.startsWith("https://")) {
            Glide.with(HostInfo.getApplication()).load(imgPath).into(img);
        } else {
            Glide.with(HostInfo.getApplication()).load(Env.app_save_path + "本地表情包/" + path.storePath + "/" + imgPath).into(img);
        }


        LinearLayout.LayoutParams panel_param = new LinearLayout.LayoutParams(LayoutHelper.dip2px(getContext(), 50), ViewGroup.LayoutParams.WRAP_CONTENT);
        panel_param.leftMargin = LayoutHelper.dip2px(getContext(), 5);
        panel_param.rightMargin = LayoutHelper.dip2px(getContext(), 5);
        panel.setLayoutParams(panel_param);

        View greenTip = new View(getContext());
        greenTip.setBackgroundColor(0xff339933);
        panel_param = new LinearLayout.LayoutParams(LayoutHelper.dip2px(getContext(), 30), 50);
        panel_param.leftMargin = LayoutHelper.dip2px(getContext(), 5);
        panel_param.rightMargin = LayoutHelper.dip2px(getContext(), 5);
        greenTip.setLayoutParams(panel_param);
        greenTip.setId(887533);
        greenTip.setVisibility(GONE);
        panel.addView(greenTip);

        newTabView.add(panel);
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
        titleView.setTextColor(0xff888888);
        titleView.setTextSize(10);
        titleView.setSingleLine();
        panel.addView(titleView);

        Glide.with(HostInfo.getApplication()).load(resID).into(img);

        LinearLayout.LayoutParams panel_param = new LinearLayout.LayoutParams(LayoutHelper.dip2px(getContext(), 50), ViewGroup.LayoutParams.WRAP_CONTENT);
        panel_param.leftMargin = LayoutHelper.dip2px(getContext(), 5);
        panel_param.rightMargin = LayoutHelper.dip2px(getContext(), 5);
        panel.setLayoutParams(panel_param);

        View greenTip = new View(getContext());
        greenTip.setBackgroundColor(0xff339933);
        panel_param = new LinearLayout.LayoutParams(LayoutHelper.dip2px(getContext(), 30), 50);
        panel_param.leftMargin = LayoutHelper.dip2px(getContext(), 5);
        panel_param.rightMargin = LayoutHelper.dip2px(getContext(), 5);
        greenTip.setLayoutParams(panel_param);
        greenTip.setId(887533);
        greenTip.setVisibility(GONE);
        panel.addView(greenTip);

        newTabView.add(panel);

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


            adapter.notifyDataSetChanged();
        }, 200);


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
    protected void onDismiss() {
        super.onDismiss();
        adapter.destroyAllViews();
        Glide.get(HostInfo.getApplication()).clearMemory();
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.sticker_panel_plus_main;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        ViewGroup vg = findViewByItemNumber(firstVisibleItem);
        if (vg != null) {
            notifyTabViewSelect(vg);
        }

        int first = view.getFirstVisiblePosition();
        int last = view.getLastVisiblePosition();
        adapter.notifyViewUpdate(first, last);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        adapter.destroyAllViews();
    }

    private ViewGroup findViewByItemNumber(int number) {
        for (int i = 0; i < newTabView.size(); i++) {
            Object tag = newTabView.get(i).getTag();
            if (tag instanceof Integer && ((Integer) tag) == number) {
                return newTabView.get(i);
            }
        }
        return null;
    }
}
