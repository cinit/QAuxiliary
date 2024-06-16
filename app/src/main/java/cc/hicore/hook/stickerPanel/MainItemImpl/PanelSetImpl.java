package cc.hicore.hook.stickerPanel.MainItemImpl;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import cc.hicore.Env;
import cc.hicore.Utils.Async;
import cc.hicore.Utils.FunConf;
import cc.hicore.Utils.ImageUtils;
import cc.hicore.hook.stickerPanel.ICreator;
import cc.hicore.hook.stickerPanel.LocalDataHelper;
import cc.hicore.ui.SimpleDragSortView;
import cc.ioctl.util.LayoutHelper;
import io.github.qauxv.R;
import java.util.ArrayList;
import java.util.List;

public class PanelSetImpl implements ICreator.IMainPanelItem {
    private Context mContext;
    private ViewGroup cacheView;
    public PanelSetImpl(Context context) {
        mContext = context;
        cacheView = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.sticker_panel_set,null);

        EditText edit_show_title = cacheView.findViewById(R.id.sticker_panel_set_ed_change_title);
        edit_show_title.setEnabled(FunConf.getBoolean("global", "sticker_panel_set_ch_change_title", false));
        edit_show_title.setText(FunConf.getString("global", "sticker_panel_set_ed_change_title", ""));
        edit_show_title.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                FunConf.setString("global", "sticker_panel_set_ed_change_title", s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        CheckBox open_show_title = cacheView.findViewById(R.id.sticker_panel_set_ch_change_title);
        open_show_title.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()){
                edit_show_title.setEnabled(isChecked);
                FunConf.setBoolean("global", "sticker_panel_set_ch_change_title", isChecked);
            }
        });
        open_show_title.setChecked(FunConf.getBoolean("global", "sticker_panel_set_ch_change_title", false));




        CheckBox dont_auto_close = cacheView.findViewById(R.id.sticker_panel_set_dont_close_panel);
        dont_auto_close.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()){
                FunConf.setBoolean("global", "sticker_panel_set_dont_close_panel", isChecked);
            }
        });
        dont_auto_close.setChecked(FunConf.getBoolean("global", "sticker_panel_set_dont_close_panel", false));

        CheckBox open_last_select = cacheView.findViewById(R.id.sticker_panel_set_open_last_select);
        open_last_select.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()){
                FunConf.setBoolean("global", "sticker_panel_set_open_last_select", isChecked);
            }
        });
        open_last_select.setChecked(FunConf.getBoolean("global", "sticker_panel_set_open_last_select", false));

        RadioButton show_anim_always = cacheView.findViewById(R.id.sticker_panel_set_rb_show_anim_always);
        show_anim_always.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()){
                FunConf.setInt("global", "sticker_panel_set_rb_show_anim", 0);
            }
        });
        show_anim_always.setChecked(FunConf.getInt("global", "sticker_panel_set_rb_show_anim", 1) == 0);

        RadioButton show_anim_need = cacheView.findViewById(R.id.sticker_panel_set_rb_show_anim_need);
        show_anim_need.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()){
                FunConf.setInt("global", "sticker_panel_set_rb_show_anim", 1);
            }
        });
        show_anim_need.setChecked(FunConf.getInt("global", "sticker_panel_set_rb_show_anim", 1) == 1);

        RadioButton show_anim_never = cacheView.findViewById(R.id.sticker_panel_set_rb_show_never);
        show_anim_never.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()){
                FunConf.setInt("global", "sticker_panel_set_rb_show_anim", 2);
            }
        });
        show_anim_never.setChecked(FunConf.getInt("global", "sticker_panel_set_rb_show_anim", 1) == 2);

        Button sort_group = cacheView.findViewById(R.id.sticker_panel_set_btn_sort_group);
        sort_group.setOnClickListener(v->{
            Async.runAsyncLoading(mContext,"正在处理图片",()->{
                List<LocalDataHelper.LocalPath> paths = LocalDataHelper.readPaths();
                ArrayList<String> picPaths = new ArrayList<>();
                ArrayList<String> sources = new ArrayList<>();
                for (LocalDataHelper.LocalPath path : paths) {
                    sources.add(path.storePath);
                    int width_item = LayoutHelper.getScreenWidth(mContext) / 6;
                    picPaths.add(ImageUtils.getResizePicPath(Env.app_save_path + "本地表情包/" + path.storePath + "/" + path.coverName,width_item));
                }
                Async.runOnUi(()->{
                    SimpleDragSortView.createDrag(mContext,picPaths,sources,()->{
                        for (LocalDataHelper.LocalPath path : paths){
                            LocalDataHelper.deletePathName(path);
                        }
                        for (int i = 0; i < sources.size(); i++) {
                            for (LocalDataHelper.LocalPath path : paths){
                                if (path.storePath.equals(sources.get(i))){
                                    LocalDataHelper.addPath(path);
                                }
                            }
                        }
                        ICreator.dismissAll();
                    });

                });
            });

        });
    }
    @Override
    public View getView() {
        return cacheView;
    }

    @Override
    public void onViewDestroy() {

    }

    @Override
    public long getID() {
        return 10010;
    }

    @Override
    public void notifyViewUpdate0() {

    }
}
