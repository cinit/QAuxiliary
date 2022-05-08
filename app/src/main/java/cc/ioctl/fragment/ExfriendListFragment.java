/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */
package cc.ioctl.fragment;

import static android.widget.LinearLayout.LayoutParams.MATCH_PARENT;
import static android.widget.LinearLayout.LayoutParams.WRAP_CONTENT;
import static cc.ioctl.util.DateTimeUtil.getIntervalDspMs;
import static cc.ioctl.util.DateTimeUtil.getRelTimeStrSec;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import cc.ioctl.hook.OpenProfileCard;
import cc.ioctl.util.ExfriendManager;
import cc.ioctl.util.HostStyledViewBuilder;
import cc.ioctl.util.LayoutHelper;
import cc.ioctl.util.data.EventRecord;
import cc.ioctl.util.data.FriendRecord;
import com.tencent.widget.XListView;
import io.github.qauxv.R;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.bridge.FaceImpl;
import io.github.qauxv.fragment.BaseRootLayoutFragment;
import io.github.qauxv.ui.CustomDialog;
import io.github.qauxv.ui.ResUtils;
import io.github.qauxv.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExfriendListFragment extends BaseRootLayoutFragment {

    private static final int R_ID_EXL_TITLE = 0x300AFF01;
    private static final int R_ID_EXL_SUBTITLE = 0x300AFF02;
    private static final int R_ID_EXL_FACE = 0x300AFF03;
    private static final int R_ID_EXL_STATUS = 0x300AFF04;

    private FaceImpl face;
    private ExfriendManager exm;
    private ArrayList<EventRecord> evs;
    private final BaseAdapter adapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return ExfriendListFragment.this.getCount();
        }

        @Override
        public Object getItem(int position) {
            return ExfriendListFragment.this.getItem(position);
        }

        @Override
        public long getItemId(int position) {
            return ExfriendListFragment.this.getItemId(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return ExfriendListFragment.this.getView(position, convertView, parent);
        }
    };

    @Nullable
    @Override
    public String getTitle() {
        return "历史好友";
    }

    @Nullable
    @Override
    public View doOnCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        Context context = inflater.getContext();
        try {
            face = FaceImpl.getInstance();
        } catch (Throwable e) {
            Log.e(e);
        }
        exm = ExfriendManager.getCurrent();
        reload();

        XListView sdlv = new XListView(context, null);
        ViewGroup.LayoutParams mmlp = new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        RelativeLayout.LayoutParams mwllp = new RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        sdlv.setId(R.id.rootMainList);
        mwllp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        mwllp.addRule(RelativeLayout.CENTER_VERTICAL);

        sdlv.setDivider(null);
        long uin = AppRuntimeHelper.getLongAccountUin();
        ExfriendManager exm = ExfriendManager.get(uin);
        exm.clearUnreadFlag();
        setSubtitle("最后更新: " + getRelTimeStrSec(exm.getLastUpdateTimeSec()));
        sdlv.setAdapter(adapter);
        setRootLayoutView(sdlv);
        return sdlv;
    }

    public void reload() {
        ConcurrentHashMap<Integer, EventRecord> eventsMap = exm.getEvents();
        if (evs == null) {
            evs = new ArrayList<>();
        } else {
            evs.clear();
        }
        if (eventsMap == null) {
            return;
        }
        Iterator<Map.Entry<Integer, EventRecord>> it = eventsMap.entrySet().iterator();
        EventRecord ev;
        while (it.hasNext()) {
            ev = it.next().getValue();
            evs.add(ev);
        }
        Collections.sort(evs);
    }

    public int getCount() {
        return evs.size();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        Context context = requireContext();
        EventRecord ev = evs.get(position);
        if (convertView == null) {
            convertView = inflateItemView(context, ev);//tv;
        }
        convertView.setTag(ev);
        TextView title = convertView.findViewById(R_ID_EXL_TITLE);
        title.setText(ev.getShowStr());
        boolean isfri = false;

        TextView stat = convertView.findViewById(R_ID_EXL_STATUS);
        try {
            if (exm.getPersons().get(ev.operand).friendStatus
                    == FriendRecord.STATUS_FRIEND_MUTUAL) {
                isfri = true;
            }
        } catch (Exception e) {
        }

        if (isfri) {
            stat.setTextColor(HostStyledViewBuilder.getColorSkinGray3());
            stat.setText("已恢复");
        } else {
            stat.setTextColor(Color.argb(255, 220, 50, 50));
            stat.setText("已删除");
        }
        TextView subtitle = convertView.findViewById(R_ID_EXL_SUBTITLE);
        subtitle.setText(getIntervalDspMs(ev.timeRangeBegin * 1000, ev.timeRangeEnd * 1000));
        ImageView imgview = convertView.findViewById(R_ID_EXL_FACE);
        Bitmap bm = face.getBitmapFromCache(FaceImpl.TYPE_USER, "" + ev.operand);
        if (bm == null) {
            imgview.setImageDrawable(
                    ResUtils.loadDrawableFromAsset("face.png", context));
            face.registerView(FaceImpl.TYPE_USER, "" + ev.operand, imgview);
        } else {
            imgview.setImageBitmap(bm);
        }

        return convertView;
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private View inflateItemView(Context context, EventRecord ev) {
        int tmp;
        RelativeLayout rlayout = new RelativeLayout(context);
        LinearLayout llayout = new LinearLayout(context);
        llayout.setGravity(Gravity.CENTER_VERTICAL);
        llayout.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout textlayout = new LinearLayout(context);
        textlayout.setOrientation(LinearLayout.VERTICAL);
        rlayout.setBackground(HostStyledViewBuilder.getListItemBackground());

        LinearLayout.LayoutParams imglp = new LinearLayout.LayoutParams(
                LayoutHelper.dip2px(context, 50),
                LayoutHelper.dip2px(context, 50));
        imglp.setMargins(tmp = LayoutHelper.dip2px(context, 6), tmp, tmp, tmp);
        ImageView imgview = new ImageView(context);
        imgview.setFocusable(false);
        imgview.setClickable(false);
        imgview.setId(R_ID_EXL_FACE);

        imgview.setScaleType(ImageView.ScaleType.FIT_XY);
        llayout.addView(imgview, imglp);
        LinearLayout.LayoutParams ltxtlp = new LinearLayout.LayoutParams(MATCH_PARENT,
                WRAP_CONTENT);
        LinearLayout.LayoutParams textlp = new LinearLayout.LayoutParams(MATCH_PARENT,
                WRAP_CONTENT);
        ltxtlp.setMargins(tmp = LayoutHelper.dip2px(context, 2), tmp, tmp, tmp);
        textlp.setMargins(tmp = LayoutHelper.dip2px(context, 1), tmp, tmp, tmp);
        llayout.addView(textlayout, ltxtlp);

        TextView title = new TextView(context);
        title.setId(R_ID_EXL_TITLE);
        title.setSingleLine();
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setTextColor(ResourcesCompat.getColor(context.getResources(), R.color.firstTextColor, context.getTheme()));
        title.setTextSize(LayoutHelper.px2sp(context, LayoutHelper.dip2px(context, 16)));

        TextView subtitle = new TextView(context);
        subtitle.setId(R_ID_EXL_SUBTITLE);
        subtitle.setSingleLine();
        subtitle.setGravity(Gravity.CENTER_VERTICAL);
        subtitle.setTextColor(ResourcesCompat.getColor(context.getResources(), R.color.secondTextColor, context.getTheme()));
        subtitle.setTextSize(LayoutHelper.px2sp(context, LayoutHelper.dip2px(context, 14)));

        textlayout.addView(title, textlp);
        textlayout.addView(subtitle, textlp);

        RelativeLayout.LayoutParams statlp = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);

        TextView stat = new TextView(context);
        stat.setId(R_ID_EXL_STATUS);
        stat.setSingleLine();
        stat.setGravity(Gravity.CENTER);
        stat.setTextSize(LayoutHelper.px2sp(context, LayoutHelper.dip2px(context, 16)));
        statlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        statlp.addRule(RelativeLayout.CENTER_VERTICAL);
        statlp.rightMargin = LayoutHelper.dip2px(context, 16);

        rlayout.addView(llayout);
        rlayout.addView(stat, statlp);

        rlayout.setClickable(true);
        rlayout.setOnClickListener(v -> {
            long uin = ((EventRecord) v.getTag()).operand;
            OpenProfileCard.openUserProfileCard(v.getContext(), uin);
        });
        rlayout.setOnLongClickListener(v -> {
            try {
                CustomDialog dialog = CustomDialog.createFailsafe(context);
                dialog.setTitle("删除记录");
                dialog.setCancelable(true);
                dialog.setMessage("确认删除历史记录(" + ((EventRecord) v.getTag())._remark + ")");
                dialog.setPositiveButton("确认", (dialog12, which) -> {
                    dialog12.dismiss();
                    exm.getEvents().values().remove(v.getTag());
                    exm.saveConfigure();
                    reload();
                    adapter.notifyDataSetChanged();
                });
                dialog.setNegativeButton("取消", (dialog1, which) -> dialog1.dismiss());
                dialog.show();
            } catch (Exception e) {
                Log.e(e);
            }
            return true;
        });
        return rlayout;
    }
}
