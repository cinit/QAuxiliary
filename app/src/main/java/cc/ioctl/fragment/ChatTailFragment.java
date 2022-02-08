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

import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static cc.ioctl.util.HostStyledViewBuilder.newListItemButton;
import static cc.ioctl.util.HostStyledViewBuilder.subtitle;
import static cc.ioctl.util.LayoutHelper.dip2px;
import static cc.ioctl.util.LayoutHelper.dip2sp;
import static cc.ioctl.util.LayoutHelper.newLinearLayoutParams;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import cc.ioctl.dialog.RikkaCustomMsgTimeFormatDialog;
import cc.ioctl.hook.ChatTailHook;
import cc.ioctl.hook.FakeBatteryHook;
import cc.ioctl.util.ExfriendManager;
import cc.ioctl.util.HostStyledViewBuilder;
import cc.ioctl.util.ui.drawable.HighContrastBorder;
import com.tencent.mobileqq.widget.BounceScrollView;
import io.github.qauxv.R;
import io.github.qauxv.config.ConfigItems;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.fragment.BaseSettingFragment;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.Toasts;
import java.text.SimpleDateFormat;
import java.util.Date;
import me.kyuubiran.util.UtilsKt;

public class ChatTailFragment extends BaseSettingFragment implements View.OnClickListener {

    public static final String delimiter = "#msg#";
    private static final int R_ID_APPLY = 0x300AFF81;
    private static final int R_ID_DISABLE = 0x300AFF82;
    private static final int R_ID_PERCENT_VALUE = 0x300AFF83;
    private static final int R_ID_REGEX_VALUE = 0x300AFF84;
    private static int battery = 0;
    private static String power = "未充电";
    private BatteryReceiver mReceiver = new BatteryReceiver();

    TextView tvStatus;

    private boolean mMsfResponsive = false;
    private TextView __tv_chat_tail_groups, __tv_chat_tail_friends, __tv_chat_tail_time_format;

    public static int getBattery() {
        return battery;
    }

    public static String getPower() {
        if (FakeBatteryHook.INSTANCE.isEnabled()) {
            return FakeBatteryHook.INSTANCE.isFakeBatteryCharging() ? "充电中" : "未充电";
        }
        return power;
    }

    @Nullable
    @Override
    public String getTitle() {
        return "设置小尾巴";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        if (!FakeBatteryHook.INSTANCE.isEnabled()) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            filter.addAction(Intent.ACTION_POWER_CONNECTED);
            filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
            requireContext().registerReceiver(mReceiver, filter);//注册BroadcastReceiver
        }
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ViewGroup.LayoutParams mmlp = new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        LinearLayout __ll = new LinearLayout(requireContext());
        __ll.setOrientation(LinearLayout.VERTICAL);
        ViewGroup bounceScrollView = new BounceScrollView(context, null);
        bounceScrollView.setLayoutParams(mmlp);
        bounceScrollView.setId(R.id.rootBounceScrollView);
        ll.setId(R.id.rootMainLayout);
        bounceScrollView.addView(ll, new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        RelativeLayout.LayoutParams __lp_l = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        int mar = (int) (dip2px(requireContext(), 12) + 0.5f);
        __lp_l.setMargins(mar, 0, mar, 0);
        __lp_l.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        __lp_l.addRule(RelativeLayout.CENTER_VERTICAL);
        RelativeLayout.LayoutParams __lp_r = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        __lp_r.setMargins(mar, 0, mar, 0);
        __lp_r.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        __lp_r.addRule(RelativeLayout.CENTER_VERTICAL);

        ll.addView(subtitle(requireContext(), "在这里设置然后每次聊天自动添加"));
        ChatTailHook ct = ChatTailHook.INSTANCE;
        boolean enabled = ct.isEnabled();
        ViewGroup _s;
        LinearLayout _t;
        ll.addView(_t = subtitle(requireContext(), ""));
        tvStatus = (TextView) _t.getChildAt(0);
        ll.addView(subtitle(requireContext(), "默认不换行，换行符号请输入\\n"));

//        ll.addView(_s = newListItemButton(context, "选择生效的群", "未选择的群将不展示小尾巴", "N/A",
//                v -> TroopSelectActivity.startToSelectTroopsAndSaveToExfMgr(requireContext(),
//                        ConfigItems.qn_chat_tail_troops, "选择小尾巴生效群")));
//        ll.addView(_s = newListItemButton(context, "选择生效的好友", "未选择的好友将不展示小尾巴", "N/A",
//                v -> FriendSelectActivity.startToSelectFriendsAndSaveToExfMgr(requireContext(),
//                        ConfigItems.qn_chat_tail_friends, "选择小尾巴生效好友")));
        ll.addView(_s = newListItemButton(context, "设置日期格式", "请在QN内置花Q的\"聊天页自定义时间格式\"中设置",
                RikkaCustomMsgTimeFormatDialog.getTimeFormat(),
                view -> Toasts.info(requireContext(), "请在QN内置花Q的\"聊天页自定义时间格式\"中设置")));
        ll.addView(subtitle(requireContext(), "设置小尾巴"));
        ll.addView(subtitle(requireContext(), "可用变量(点击自动输入): "));
        LinearLayout _a, _b, _c, _d, _e, _f, _g, _h;
        ll.addView(_a = subtitle(requireContext(), delimiter + "         : 当前消息"));
        ll.addView(_b = subtitle(requireContext(), "#model#   : 手机型号"));
        ll.addView(_c = subtitle(requireContext(), "#brand#   : 手机厂商"));
        ll.addView(_d = subtitle(requireContext(), "#battery# : 当前电量"));
        ll.addView(_e = subtitle(requireContext(), "#power#   : 是否正在充电"));
        ll.addView(_f = subtitle(requireContext(), "#time#    : 当前时间"));
        ll.addView(_g = subtitle(requireContext(), "#Spacemsg#    : 空格消息"));
        ll.addView(_h = subtitle(requireContext(), "\\n       : 换行"));
        int _5dp = dip2px(requireContext(), 5);
        EditText pct = createEditText(R_ID_PERCENT_VALUE, _5dp,
                ct.getTailCapacity().replace("\n", "\\n"),
                ChatTailFragment.delimiter + " 将会被替换为消息");
        _a.setOnClickListener(v -> pct.setText(pct.getText() + delimiter));
        _b.setOnClickListener(v -> pct.setText(pct.getText() + "#model#"));
        _c.setOnClickListener(v -> pct.setText(pct.getText() + "#brand#"));
        _d.setOnClickListener(v -> pct.setText(pct.getText() + "#battery#"));
        _e.setOnClickListener(v -> pct.setText(pct.getText() + "#power#"));
        _f.setOnClickListener(v -> pct.setText(pct.getText() + "#time#"));
        _g.setOnClickListener(v -> pct.setText(pct.getText() + "#Spacemsg#"));
        _h.setOnClickListener(v -> pct.setText(pct.getText() + "\\n"));
        ll.addView(pct, newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT, 2 * _5dp, _5dp, 2 * _5dp, _5dp));
//        ll.addView(newListItemSwitchFriendConfigNext(this, "正则开关",
//                "通过正则表达式的消息不会携带小尾巴(无需重启" + HostInfo.getAppName() + ")",
//        ConfigItems.qn_chat_tail_regex, false));
        ll.addView(createEditText(R_ID_REGEX_VALUE, _5dp, ChatTailHook.getTailRegex(),
                        "需要有正则表达式相关知识(部分匹配)"),
                newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT,
                        2 * _5dp, _5dp, 2 * _5dp, _5dp));
//        ll.addView(newListItemSwitchFriendConfigNext(this, "全局开关",
//                "开启将无视生效范围(无需重启" + HostInfo.getAppName() + ")",
//        ConfigItems.qn_chat_tail_global, false));
        Button apply = new Button(requireContext());
        apply.setId(R_ID_APPLY);
        apply.setOnClickListener(this);
//        ResUtils.applyStyleCommonBtnBlue(apply);
        ll.addView(apply, newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT, 2 * _5dp, _5dp, 2 * _5dp, _5dp));
        Button dis = new Button(requireContext());
        dis.setId(R_ID_DISABLE);
        dis.setOnClickListener(this);
//        ResUtils.applyStyleCommonBtnBlue(dis);
        dis.setText("停用");
        ll.addView(dis, newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT, 2 * _5dp, _5dp, 2 * _5dp, _5dp));
        __ll.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        showStatus();
        return bounceScrollView;
    }

    private EditText createEditText(int id, int _5dp, String text, String hint) {
        EditText pct = new EditText(requireContext());
        pct.setId(id);
        pct.setInputType(TYPE_CLASS_TEXT);
        pct.setTextColor(HostStyledViewBuilder.getColorSkinBlack());
        pct.setTextSize(dip2sp(requireContext(), 18));
        ViewCompat.setBackground(pct, null);
        pct.setGravity(Gravity.CENTER);
        pct.setPadding(_5dp, _5dp / 2, _5dp, _5dp / 2);
        ViewCompat.setBackground(pct, new HighContrastBorder());
        pct.setHint(hint);
        pct.setText(text);
        pct.setSelection(pct.getText().length());
        //吐槽一下，如果返回为空的话，上一行代码是会报错的啊，Android本身在设置时有帮忙处理
        return pct;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireContext().unregisterReceiver(mReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        ConfigManager cfg = ExfriendManager.getCurrent().getConfig();
        String str = cfg.getString(ConfigItems.qn_chat_tail_troops);
        int n = 0;
        if (str != null && str.length() > 4) {
            n = str.split(",").length;
        }
        __tv_chat_tail_groups.setText(n + "个群");
        str = cfg.getString(ConfigItems.qn_chat_tail_friends);
        n = 0;
        if (str != null && str.length() > 4) {
            n = str.split(",").length;
        }
        __tv_chat_tail_friends.setText(n + "个好友");
    }

    private void showStatus() {
        Activity activity = requireActivity();
        ChatTailHook ct = ChatTailHook.INSTANCE;
        boolean enabled = ct.isEnabled();
        String desc = "当前状态: ";
        if (enabled) {
            if (!ChatTailHook.isRegex() || !ChatTailHook.isPassRegex("示例消息")) {
                desc += "已开启: \n" + ct.getTailCapacity()
                        .replace(ChatTailFragment.delimiter, "示例消息")
                        .replace("#model#", Build.MODEL)
                        .replace("#brand#", Build.BRAND)
                        .replace("#battery#", battery + "")
                        .replace("#power#", ChatTailFragment.getPower())
                        .replace("#time#",
                                new SimpleDateFormat(RikkaCustomMsgTimeFormatDialog.getTimeFormat())
                                        .format(new Date()));
                if (desc.contains("#Spacemsg#")) {
                    desc = desc.replace("#Spacemsg#", "");
                    desc = UtilsKt.makeSpaceMsg(desc);
                }
            } else {
                desc += "已开启: \n示例消息";
            }
        } else {
            desc += "禁用";
        }
        tvStatus.setText(desc);
        Button apply, disable;
        apply = activity.findViewById(R_ID_APPLY);
        disable = activity.findViewById(R_ID_DISABLE);
        if (!enabled) {
            apply.setText("保存并启用");
        } else {
            apply.setText("确认");
        }
        if (!enabled) {
            disable.setVisibility(View.GONE);
        } else {
            disable.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        ConfigManager cfg = ExfriendManager.getCurrent().getConfig();
        switch (v.getId()) {
            case R_ID_APPLY:
                doUpdateTailCfg();
                Log.i("isRegex:" + String.valueOf(ChatTailHook.isRegex()));
                Log.i("isPassRegex:" + String.valueOf(ChatTailHook.isPassRegex("示例消息")));
                Log.i("getTailRegex:" + ChatTailHook.getTailRegex());
                break;
            case R_ID_DISABLE:
                cfg.putBoolean(ChatTailHook.qn_chat_tail_enable, false);
                try {
                    cfg.save();
                } catch (Exception e) {
                    Toasts.error(requireContext(), "错误:" + e.toString(), Toast.LENGTH_LONG);
                    Log.e(e);
                }
                showStatus();
        }
    }

    private void doUpdateTailCfg() {
        Activity activity = requireActivity();
        ChatTailHook ct = ChatTailHook.INSTANCE;
        ConfigManager cfg = ExfriendManager.getCurrent().getConfig();
        EditText pct;
        pct = activity.findViewById(R_ID_PERCENT_VALUE);
        String val = pct.getText().toString();
        if (TextUtils.isEmpty(val)) {
            Toasts.error(requireContext(), "请输入小尾巴");
            return;
        }
        if (!val.contains(ChatTailFragment.delimiter)) {
            Toasts.error(requireContext(), "请在小尾巴中加入" + ChatTailFragment.delimiter + "");
            return;
        }
        ct.setTail(val);
        val = ((EditText) activity.findViewById(R_ID_REGEX_VALUE)).getText()
                .toString();
        if (!TextUtils.isEmpty(val)) {
            ChatTailHook.setTailRegex(val);
        }
        if (!ct.isEnabled()) {
            cfg.putBoolean(ChatTailHook.qn_chat_tail_enable, true);
            try {
                cfg.save();
            } catch (Exception e) {
                Toasts.error(requireContext(), "错误:" + e.toString(), Toast.LENGTH_LONG);
                Log.e(e);
            }
        }
        showStatus();
    }

    private static class BatteryReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case Intent.ACTION_BATTERY_CHANGED: {
                    int current = intent.getExtras().getInt("level");//获得当前电量
                    int total = intent.getExtras().getInt("scale");//获得总电量
                    int percent = current * 100 / total;
                    ChatTailFragment.battery = percent;
                    break;
                }
                case Intent.ACTION_POWER_DISCONNECTED: {
                    ChatTailFragment.power = "未充电";
                    break;
                }
                case Intent.ACTION_POWER_CONNECTED: {
                    ChatTailFragment.power = "充电中";
                    break;
                }
                default: // ignore
            }
        }
    }
}
