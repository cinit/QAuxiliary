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

import static android.text.InputType.TYPE_CLASS_NUMBER;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static cc.ioctl.util.HostStyledViewBuilder.subtitle;
import static cc.ioctl.util.LayoutHelper.dip2px;
import static cc.ioctl.util.LayoutHelper.dip2sp;
import static cc.ioctl.util.LayoutHelper.newLinearLayoutParams;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import cc.ioctl.hook.experimental.FakeBatteryHook;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.ui.drawable.HighContrastBorder;
import com.tencent.mobileqq.widget.BounceScrollView;
import io.github.qauxv.R;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.core.HookInstaller;
import io.github.qauxv.fragment.BaseRootLayoutFragment;
import io.github.qauxv.ui.CustomDialog;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.Toasts;

public class FakeBatteryConfigFragment extends BaseRootLayoutFragment implements View.OnClickListener {

    private TextView tvStatus;
    private Button btnApply, btnDisable;
    private EditText inputBatteryLevel;
    private CheckBox checkBoxCharging;

    private boolean mMsfResponsive = false;

    @Nullable
    @Override
    public String getTitle() {
        return "自定义电量";
    }

    @Override
    public View doOnCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                               @Nullable Bundle savedInstanceState) {
        Context context = inflater.getContext();
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ViewGroup.LayoutParams mmlp = new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        LinearLayout __ll = new LinearLayout(context);
        __ll.setOrientation(LinearLayout.VERTICAL);
        ViewGroup bounceScrollView = new BounceScrollView(context, null);
        bounceScrollView.setLayoutParams(mmlp);
        bounceScrollView.setId(R.id.rootBounceScrollView);
        ll.setId(R.id.rootMainLayout);
        bounceScrollView.addView(ll, new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        LinearLayout.LayoutParams fixlp = new LinearLayout.LayoutParams(MATCH_PARENT, dip2px(context, 48));
        RelativeLayout.LayoutParams __lp_l = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        int mar = (int) (dip2px(context, 12) + 0.5f);
        __lp_l.setMargins(mar, 0, mar, 0);
        __lp_l.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        __lp_l.addRule(RelativeLayout.CENTER_VERTICAL);
        RelativeLayout.LayoutParams __lp_r = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        __lp_r.setMargins(mar, 0, mar, 0);
        __lp_r.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        __lp_r.addRule(RelativeLayout.CENTER_VERTICAL);

        ll.addView(subtitle(context, "!!! 此功能仅在 QQ>=8.2.6 且在线状态为 我的电量 时生效"));
        ll.addView(subtitle(context, "服务器的电量数据有6分钟的延迟属于正常情况"));
        ll.addView(subtitle(context, "请不要把电量设置为 0 ,因为 0 会被TX和谐掉"));
        FakeBatteryHook bat = FakeBatteryHook.INSTANCE;
        boolean enabled = bat.isEnabled();
        LinearLayout _t;
        ll.addView(_t = subtitle(context, ""));
        tvStatus = (TextView) _t.getChildAt(0);
        ll.addView(subtitle(context, "设置自定义电量百分比:"));
        int _5dp = dip2px(context, 5);
        EditText pct = new EditText(context);
        inputBatteryLevel = pct;
        pct.setInputType(TYPE_CLASS_NUMBER);
//        pct.setTextColor(ResUtils.skin_black);
        pct.setTextSize(dip2sp(context, 18));
        ViewCompat.setBackground(pct, null);
        pct.setGravity(Gravity.CENTER);
        pct.setPadding(_5dp, _5dp / 2, _5dp, _5dp / 2);
        ViewCompat.setBackground(pct, new HighContrastBorder());
        pct.setHint("电量百分比, 取值范围 [1,100]");
        pct.setText(bat.getFakeBatteryCapacity() + "");
        pct.setSelection(pct.getText().length());
        ll.addView(pct, newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT, 2 * _5dp, _5dp, 2 * _5dp, _5dp));
        CheckBox charging = new CheckBox(context);
        checkBoxCharging = charging;
        charging.setText("正在充电");
        charging.setTextSize(17);
//        charging.setTextColor(ResUtils.skin_black);
//        charging.setButtonDrawable(ResUtils.getCheckBoxBackground());
        charging.setPadding(_5dp, _5dp, _5dp, _5dp);
        charging.setChecked(FakeBatteryHook.INSTANCE.isFakeBatteryCharging());
        ll.addView(charging, newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT, 3 * _5dp, _5dp, 2 * _5dp, _5dp));
        Button apply = new Button(context);
        btnApply = apply;
        apply.setId(R.id.btn_apply);
        apply.setOnClickListener(this);
//        ResUtils.applyStyleCommonBtnBlue(apply);
        ll.addView(apply, newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT, 2 * _5dp, _5dp, 2 * _5dp, _5dp));
        Button dis = new Button(context);
        dis.setId(R.id.btn_disable);
        btnDisable = dis;
        dis.setOnClickListener(this);
//        ResUtils.applyStyleCommonBtnBlue(dis);
        dis.setText("停用");
        ll.addView(dis, newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT, 2 * _5dp, _5dp, 2 * _5dp, _5dp));
        __ll.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        showStatus();
        setRootLayoutView(bounceScrollView);
        return bounceScrollView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tvStatus = null;
        inputBatteryLevel = null;
        checkBoxCharging = null;
        btnApply = null;
        btnDisable = null;
    }

    private void showStatus() {
        FakeBatteryHook bat = FakeBatteryHook.INSTANCE;
        boolean enabled = bat.isEnabled();
        String desc = "当前状态: ";
        if (enabled) {
            desc += "已开启 " + bat.getFakeBatteryCapacity() + "%";
            if (bat.isFakeBatteryCharging()) {
                desc += "+";
            }
        } else {
            desc += "禁用";
        }
        tvStatus.setText(desc);
        Button apply = btnApply, disable = btnDisable;
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
        Context context = requireContext();
        switch (v.getId()) {
            case R.id.btn_apply:
                if (mMsfResponsive) {
                    doUpdateBatCfg();
                } else {
                    final Dialog waitDialog = CustomDialog.create(context).setCancelable(true)
                            .setTitle("请稍候")
                            .setMessage("等待 :MSF 进程响应").show();
                    SyncUtils.enumerateProc(context, SyncUtils.PROC_MSF, 3000,
                            new SyncUtils.EnumCallback() {
                                private boolean mFinished = false;

                                @Override
                                public void onResponse(SyncUtils.EnumRequestHolder holder,
                                        SyncUtils.ProcessInfo process) {
                                    if (mFinished) {
                                        return;
                                    }
                                    mFinished = true;
                                    mMsfResponsive = true;
                                    waitDialog.dismiss();
                                    doUpdateBatCfg();
                                }

                                @Override
                                public void onEnumResult(SyncUtils.EnumRequestHolder holder) {
                                    if (mFinished) {
                                        return;
                                    }
                                    Context context = requireContext();
                                    mFinished = true;
                                    mMsfResponsive = holder.result.size() > 0;
                                    waitDialog.dismiss();
                                    if (mMsfResponsive) {
                                        doUpdateBatCfg();
                                    } else {
                                        CustomDialog.create(context).setTitle("操作失败")
                                                .setCancelable(true).setPositiveButton("确认", null)
                                                .setMessage("发生错误:\n" + HostInfo.getApplication().getPackageName()
                                                        + ":MSF 进程响应超时\n" +
                                                        "如果您的" + HostInfo.getAppName() + "刚刚启动,您可以在十几秒后再试一次\n" +
                                                        "如果您是太极(含无极)用户,请确认您的太极版本至少为 湛泸-6.0.2(1907) ,如低于此版本,请尽快升级")
                                                .show();
                                    }
                                }
                            });
                }
                break;
            case R.id.btn_disable:
                FakeBatteryHook.INSTANCE.setEnabled(false);
                showStatus();
                break;
            default: // nothing
        }
    }

    private void doUpdateBatCfg() {
        FakeBatteryHook bat = FakeBatteryHook.INSTANCE;
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        EditText pct;
        CheckBox charging;
        int val;
        Activity context = requireActivity();
        pct = inputBatteryLevel;
        charging = checkBoxCharging;
        try {
            val = Integer.parseInt(pct.getText().toString());
        } catch (NumberFormatException e) {
            Toasts.error(context, "请输入电量");
            return;
        }
        if (val < 0 || val > 100) {
            Toasts.error(context, "电量取值范围: [1,100]");
            return;
        }
        if (charging.isChecked()) {
            val |= 128;
        }
        bat.setFakeSendBatteryStatus(val);
        if (!bat.isEnabled()) {
            bat.setEnabled(true);
            try {
                cfg.save();
                boolean success = true;
                if (!bat.isInitialized()) {
                    success = bat.initialize();
                }
                SyncUtils.requestInitHook(HookInstaller.getHookIndex(bat), bat.getTargetProcesses());
                if (!success) {
                    Toasts.error(context, "初始化错误: 可能是版本不支持");
                }
            } catch (Exception e) {
                Toasts.error(context, "错误:" + e, Toast.LENGTH_LONG);
                Log.e(e);
            }
        }
        showStatus();
    }

}
