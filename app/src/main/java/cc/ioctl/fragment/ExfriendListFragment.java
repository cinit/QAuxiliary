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

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import cc.ioctl.hook.friend.OpenFriendChatHistory;
import cc.ioctl.hook.profile.OpenProfileCard;
import cc.ioctl.util.ExfriendManager;
import cc.ioctl.util.HostStyledViewBuilder;
import cc.ioctl.util.LayoutHelper;
import cc.ioctl.util.data.EventRecord;
import cc.ioctl.util.data.FriendRecord;
import cc.ioctl.util.ui.FaultyDialog;
import com.tencent.widget.XListView;
import io.github.qauxv.R;
import io.github.qauxv.activity.SettingsUiFragmentHostActivity;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.bridge.FaceImpl;
import io.github.qauxv.fragment.BaseRootLayoutFragment;
import io.github.qauxv.fragment.TroubleshootFragment;
import io.github.qauxv.ui.CommonContextWrapper;
import io.github.qauxv.ui.CustomDialog;
import io.github.qauxv.ui.ResUtils;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.Toasts;
import io.github.qauxv.util.UiThread;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import kotlin.collections.CollectionsKt;
import xyz.nextalone.util.SystemServiceUtils;

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.exfriend_list_fragment_options, menu);
    }

    @Nullable
    @Override
    public View doOnCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                               @Nullable Bundle savedInstanceState) {
        setTitle("历史好友");
        Context context = inflater.getContext();
        try {
            face = FaceImpl.getInstance();
        } catch (Throwable e) {
            Log.e(e);
        }
        exm = ExfriendManager.getCurrent();
        reload();

        XListView sdlv = new XListView(context, null);
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

        rlayout.addView(llayout, MATCH_PARENT, WRAP_CONTENT);
        rlayout.addView(stat, statlp);

        rlayout.setClickable(true);
        rlayout.setOnClickListener(mOnListItemClickListener);
        rlayout.setOnLongClickListener(mOnListItemLongClickListener);
        return rlayout;
    }

    private final View.OnClickListener mOnListItemClickListener = v -> {
        long uin = ((EventRecord) v.getTag()).operand;
        OpenProfileCard.openUserProfileCard(v.getContext(), uin);
    };

    private final View.OnLongClickListener mOnListItemLongClickListener = v -> {
        Context ctx = CommonContextWrapper.createAppCompatContext(v.getContext());
        EventRecord r = (EventRecord) Objects.requireNonNull(v.getTag(), "v.getTag() == null");
        // long click menu
        String[] options = new String[]{"复制 QQ 号", "删除记录", "查看本地聊天记录"};
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("操作");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: {
                    SystemServiceUtils.copyToClipboard(ctx, String.valueOf(r.operand));
                    Toasts.info(ctx, "已复制 QQ 号");
                    break;
                }
                case 1: {
                    confirmAndDeleteRecord(ctx, r);
                    break;
                }
                case 2: {
                    OpenFriendChatHistory.startFriendChatHistoryActivity(ctx, r.operand);
                    break;
                }
                default:
                    break;
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setCancelable(true);
        builder.show();
        return true;
    };

    @UiThread
    private void confirmAndDeleteRecord(Context context, EventRecord r) {
        try {
            CustomDialog dialog = CustomDialog.createFailsafe(context);
            dialog.setTitle("删除记录");
            dialog.setCancelable(true);
            dialog.setMessage("确认删除历史记录(" + r._remark + ")");
            dialog.setPositiveButton("确认", (dialog12, which) -> {
                dialog12.dismiss();
                exm.getEvents().values().remove(r);
                exm.saveConfigure();
                reload();
                adapter.notifyDataSetChanged();
            });
            dialog.setNegativeButton("取消", (dialog1, which) -> dialog1.dismiss());
            dialog.show();
        } catch (Exception e) {
            FaultyDialog.show(context, e);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_item_add_record) {
            showManualAddRecordDialog();
            return true;
        } else if (id == R.id.menu_item_edit_exclusion_list) {
            showEditExclusionListDialog();
            return true;
        } else if (id == R.id.menu_item_clear_all_record) {
            Context ctx = requireContext();
            new AlertDialog.Builder(ctx)
                    .setTitle("删除所有记录")
                    .setMessage("您可以前往模块设置的故障排除页面来删除所有历史记录。\n如果您需要删除单个记录，请长按记录进行删除。")
                    .setPositiveButton("前往", (dialog, which) -> {
                        dialog.dismiss();
                        SettingsUiFragmentHostActivity.startFragmentWithContext(ctx, TroubleshootFragment.class);
                    })
                    .setNegativeButton(R.string.dialog_btn_cancel, null)
                    .setCancelable(true)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @UiThread
    public void showManualAddRecordDialog() {
        Context ctx = requireContext();
        // title, description, input(uin), input(nick), input(time), ok, cancel
        TextView desc = new TextView(ctx);
        desc.setText("您可以手动添加历史好友被删记录，如手动将 QNotified 的历史好友记录搬过来。添加的记录将会被保存到本地。\n" +
                "其中备注(昵称) 不能为空，填写你给对方设置的备注，或者对方的昵称。\n"
                + "时间格式为 yyyy-MM-dd HH:mm:ss，填写对方删除您的时间，如 "
                + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis())
                + "\n请填写 QQ 号、备注(昵称)、时间，然后点击添加。");
        desc.setPadding(0, 0, 0, 16);
        desc.setTextColor(ResourcesCompat.getColor(ctx.getResources(), R.color.firstTextColor, ctx.getTheme()));
        desc.setTextSize(13);
        EditText inputUin = new EditText(ctx);
        inputUin.setHint("QQ 号");
        inputUin.setSingleLine();
        inputUin.setTextSize(16);
        inputUin.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText inputNick = new EditText(ctx);
        inputNick.setHint("备注(昵称)");
        inputNick.setSingleLine();
        inputNick.setTextSize(16);
        EditText inputTime = new EditText(ctx);
        inputTime.setHint("yyyy-MM-dd HH:mm:ss");
        inputTime.setSingleLine();
        inputTime.setTextSize(16);
        TextView hintTextV = new TextView(ctx);
        hintTextV.setTextSize(10);
        hintTextV.setTextColor(ResourcesCompat.getColor(ctx.getResources(), R.color.thirdTextColor, ctx.getTheme()));
        hintTextV.setText("友情提示：对方真的值得您这么做吗？(验证码：看不清？换一张)\n"
                + "\"My name is Linus Torvalds and I am your god.\" -- Linus Torvalds");
        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.addView(desc);
        layout.addView(inputUin);
        layout.addView(inputNick);
        layout.addView(inputTime);
        layout.addView(hintTextV);
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("添加历史好友记录");
        builder.setView(layout);
        builder.setPositiveButton("添加", null); // set later
        builder.setNegativeButton(R.string.dialog_btn_cancel, null);
        AlertDialog dialog = builder.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String uinStr = inputUin.getText().toString();
            String nickStr = inputNick.getText().toString();
            String timeStr = inputTime.getText().toString();
            if (uinStr.isEmpty() || nickStr.isEmpty() || timeStr.isEmpty()) {
                Toasts.error(ctx, "输入不能为空");
                return;
            }
            long uin;
            try {
                uin = Long.parseLong(uinStr);
            } catch (NumberFormatException e) {
                Toasts.error(ctx, "QQ 号格式错误");
                return;
            }
            if (uin < 10000) {
                Toasts.error(ctx, "QQ 号格式错误");
                return;
            }
            if (!timeStr.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                Toasts.error(ctx, "时间格式错误");
                return;
            }
            long time;
            try {
                time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(timeStr).getTime() / 1000;
            } catch (Exception e) {
                Toasts.error(ctx, "时间格式错误");
                return;
            }
            EventRecord ev = new EventRecord();
            ev._friendStatus = FriendRecord.STATUS_EXFRIEND;
            ev._nick = nickStr;
            ev._remark = nickStr;
            ev.event = EventRecord.EVENT_FRIEND_DELETE;
            ev.operand = uin;
            ev.executor = -1;
            ev.timeRangeBegin = time;
            ev.timeRangeEnd = time;
            exm.reportEventWithoutSave(ev, null);
            exm.saveConfigure();
            // reload
            reload();
            adapter.notifyDataSetChanged();
            Toasts.success(ctx, "添加成功");
            dialog.dismiss();
        });
    }

    @UiThread
    public void showEditExclusionListDialog() {
        Context ctx = requireContext();
        String currentUinList = TextUtils.join("\n", exm.getDeletionDetectionExclusionList());
        // title, description, input(uin list), save, cancel
        TextView desc = new TextView(ctx);
        desc.setText("您可以编辑被删好友检测排除列表，QAuxiliary 在检测到您被处于列表中的好友删除时，将不会向您发送通知，也不会记录历史记录。\n"
                + "您可以在此处添加或删除排除列表中的好友，每行一个 QQ 号。\n"
                + "请注意，此列表会被保存到本地，不会被同步到其他设备。");
        desc.setPadding(0, 0, 0, 16);
        desc.setTextColor(ResourcesCompat.getColor(ctx.getResources(), R.color.firstTextColor, ctx.getTheme()));
        desc.setTextSize(13);
        EditText input = new EditText(ctx);
        input.setHint("QQ 号列表");
        input.setSingleLine(false);
        input.setMinLines(2);
        input.setGravity(Gravity.TOP);
        input.setTextSize(16);
        input.setText(currentUinList);
        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.addView(desc);
        layout.addView(input);
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("编辑排除列表");
        builder.setView(layout);
        builder.setPositiveButton("保存", null); // set later
        builder.setNegativeButton(R.string.dialog_btn_cancel, null);
        AlertDialog dialog = builder.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String uinListStr = input.getText().toString();
            String[] uinList = uinListStr.split("[ \n\r,，;；]");
            HashSet<Long> uinListLong = new HashSet<>();
            for (String uinStr : uinList) {
                uinStr = uinStr.trim();
                if (uinStr.isEmpty()) {
                    continue;
                }
                long uin;
                try {
                    uin = Long.parseLong(uinStr);
                } catch (NumberFormatException e) {
                    Toasts.error(ctx, "QQ 号格式错误");
                    return;
                }
                if (uin < 10000) {
                    Toasts.error(ctx, "QQ 号格式错误");
                    return;
                }
                uinListLong.add(uin);
            }
            exm.setDeletionDetectionExclusionList(CollectionsKt.map(uinListLong, Object::toString).toArray(new String[0]));
            Toasts.success(ctx, "保存成功");
            dialog.dismiss();
        });
    }
}
