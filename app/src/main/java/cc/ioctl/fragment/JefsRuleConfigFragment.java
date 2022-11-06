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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static cc.ioctl.util.LayoutHelper.dip2px;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import cc.ioctl.hook.misc.JumpController;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.LayoutHelper;
import cc.ioctl.util.ui.ViewBuilder;
import cc.ioctl.util.ui.drawable.HighContrastBorder;
import com.tencent.mobileqq.widget.BounceScrollView;
import io.github.qauxv.R;
import io.github.qauxv.fragment.BaseRootLayoutFragment;
import io.github.qauxv.ui.CustomDialog;
import io.github.qauxv.util.UiThread;
import java.text.ParseException;

public class JefsRuleConfigFragment extends BaseRootLayoutFragment implements View.OnClickListener {

    private static final JumpController jmpctl = JumpController.INSTANCE;
    private EditText rulesEt;
    private TextView rulesTv;
    private LinearLayout layoutDisplay;
    private LinearLayout layoutEdit;
    private boolean currEditMode;

    @Nullable
    @Override
    public String getTitle() {
        return "跳转控制";
    }

    @Override
    public View doOnCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                               @Nullable Bundle savedInstanceState) {
        Context context = inflater.getContext();
        ViewGroup bounceScrollView = new BounceScrollView(context, null);
        bounceScrollView.setId(R.id.rootBounceScrollView);
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setId(R.id.rootMainLayout);
        bounceScrollView.addView(mainLayout, MATCH_PARENT, WRAP_CONTENT);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        ViewGroup _tmp = ViewBuilder.newListItemHookSwitchInit(context, "总开关", "关闭后所有规则不生效", JumpController.INSTANCE);
        mainLayout.addView(_tmp, LayoutHelper.newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT, 0));

        int __10 = dip2px(context, 10);
        int __5 = dip2px(context, 5);

        TextView _tv_rules = new TextView(context);
        _tv_rules.setTextSize(16);
        _tv_rules.setText("规则:");
//        _tv_rules.setTextColor(ResUtils.skin_black);
        mainLayout.addView(_tv_rules,
                LayoutHelper.newLinearLayoutParams(WRAP_CONTENT, WRAP_CONTENT, __10));

        {
            layoutDisplay = new LinearLayout(context);
            layoutDisplay.setOrientation(LinearLayout.VERTICAL);
            layoutDisplay.setId(R.id.jefsRulesDisplayLayout);
            {
                String appLabel = HostInfo.getAppName();
                TextView _tmp_1 = new TextView(context);
//                _tmp_1.setTextColor(ResUtils.skin_gray3);
                _tmp_1.setText("本功能用于去除恼人的 \"即将离开" + appLabel + " 前往其他应用\" 对话框, " +
                        "也可用于限制或禁止" + appLabel + "跳转到特定第三方APP或启动某特定活动(自身的也行)");
                layoutDisplay.addView(_tmp_1,
                        LayoutHelper.newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT, __5));
            }
            rulesTv = new TextView(context);
            rulesTv.setTextSize(14);
            rulesTv.setPadding(__5, __5, __5, __5);
            rulesTv.setHorizontallyScrolling(true);
//            rulesTv.setTextColor(ResUtils.skin_black);
            layoutDisplay.addView(rulesTv,
                    LayoutHelper.newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT, __5));

            Button editBtn = new Button(context);
            editBtn.setId(R.id.jefsRulesEditButton);
            editBtn.setOnClickListener(this);
            editBtn.setText("编辑规则");
//            ResUtils.applyStyleCommonBtnBlue(editBtn);
            layoutDisplay.addView(editBtn,
                    LayoutHelper.newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT, __10));

            mainLayout.addView(layoutDisplay, MATCH_PARENT, WRAP_CONTENT);
        }
        {
            layoutEdit = new LinearLayout(context);
            layoutEdit.setOrientation(LinearLayout.VERTICAL);
            layoutEdit.setId(R.id.jefsRulesEditLayout);
            {
                TextView _tmp_1 = new TextView(context);
//                _tmp_1.setTextColor(ResUtils.skin_gray3);
                _tmp_1.setText("规则从上到下按顺序进行匹配\n" +
                        "支持通配符 * 及 **\n" +
                        "规则格式: 动作,匹配项;\n可以有多个匹配项, 构成并且关系, \n如: 动作,匹配项1,匹配项2,匹配项3;\n" +
                        "动作有A/D/Q三种: 允许(A), 禁止(D), 弹窗询问(Q), 匹配项有3种(P/C/A)\n" +
                        "按包名(P): 如  A,P:com.tencent.mm;    允许跳转到微信\n" +
                        "按组件(C): 如  D,C:de.robv.android.xposed.installer/.WelcomeActivity;  禁止跳转到Xposed的主界面\n"
                        +
                        "按动作(A): 如  Q,A:android.intent.action.DIAL;    在每次跳转到拨号前询问\n" +
                        "关于通配符: aa.bbb.*.dddd 可以匹配 aa.bbb.ccccc.dddd 而不匹配 aa.bbb.cc.ee.dddd\n" +
                        " aa.bbb.**.dddd 可以匹配 aa.bbb.cc.dddd 和 aa.bbb.cc.ee.dddd 但不匹配 aa.bbb.dddd");
                layoutEdit.addView(_tmp_1,
                        LayoutHelper.newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT, __5));
            }
            rulesEt = new EditText(context);
            rulesEt.setHorizontallyScrolling(true);
            rulesEt.setTextSize(16);
            rulesEt.setPadding(__5, __5, __5, __5);
            ViewCompat.setBackground(rulesEt, new HighContrastBorder());
//            rulesEt.setTextColor(ResUtils.skin_black);
            rulesEt.setTypeface(Typeface.MONOSPACE);
            layoutEdit.addView(rulesEt,
                    LayoutHelper.newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT, __10));

            Button saveBtn = new Button(context);
            saveBtn.setId(R.id.jefsRulesSaveButton);
            saveBtn.setOnClickListener(this);
            saveBtn.setText("确认");
//            ResUtils.applyStyleCommonBtnBlue(saveBtn);
            layoutEdit.addView(saveBtn,
                    LayoutHelper.newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT, __10));

            {
                RelativeLayout _rl_tmp = new RelativeLayout(context);

                TextView cancelBtn = new TextView(context);
//                cancelBtn.setTextColor(ResUtils.skin_black);
                cancelBtn.setTextSize(16);
                cancelBtn.setId(R.id.jefsRulesCancelButton);
                cancelBtn.setGravity(Gravity.CENTER);
                cancelBtn.setPadding(__10, __5, __10, __10 / 2);
                cancelBtn.setText("取消");
//                cancelBtn.setTextColor(ResUtils.skin_blue);
                cancelBtn.setOnClickListener(this);

                TextView resetBtn = new TextView(context);
//                resetBtn.setTextColor(ResUtils.skin_black);
                resetBtn.setTextSize(16);
                resetBtn.setId(R.id.jefsRulesResetButton);
                resetBtn.setGravity(Gravity.CENTER);
                resetBtn.setPadding(__10, __5, __10, __10 / 2);
                resetBtn.setText("恢复默认规则");
//                resetBtn.setTextColor(ResUtils.skin_blue);
                resetBtn.setOnClickListener(this);

                RelativeLayout.LayoutParams _rlp_l = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                _rlp_l.leftMargin = __10;
                _rlp_l.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                _rl_tmp.addView(resetBtn, _rlp_l);

                RelativeLayout.LayoutParams _rlp_r = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                _rlp_r.rightMargin = __10;
                _rlp_r.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                _rl_tmp.addView(cancelBtn, _rlp_r);

                layoutEdit.addView(_rl_tmp, MATCH_PARENT, WRAP_CONTENT);
            }
            mainLayout.addView(layoutEdit, MATCH_PARENT, WRAP_CONTENT);
        }

        goToDisplayMode();
        setRootLayoutView(bounceScrollView);
        return bounceScrollView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.jefsRulesEditButton: {
                goToEditMode();
                break;
            }
            case R.id.jefsRulesCancelButton: {
                confirmLeaveEditMode();
                break;
            }
            case R.id.jefsRulesResetButton: {
                confirmResetRules();
                break;
            }
            case R.id.jefsRulesSaveButton: {
                checkAndSaveRules(rulesEt.getText().toString());
                break;
            }
        }
    }

    @Override
    public boolean doOnBackPressed() {
        if (currEditMode) {
            CustomDialog.createFailsafe(requireContext())
                    .setPositiveButton("确认", (dialog, which) -> finishFragment())
                    .setNegativeButton("取消", null)
                    .setTitle("退出")
                    .setMessage("未保存的改动将会丢失")
                    .setCancelable(true).show();
            return true;
        } else {
            return false;
        }
    }

    private void goToEditMode() {
        currEditMode = true;
        rulesTv.setText("");
        rulesEt.setText(jmpctl.getRuleString());
        layoutDisplay.setVisibility(View.GONE);
        layoutEdit.setVisibility(View.VISIBLE);
    }

    private void goToDisplayMode() {
        currEditMode = false;
        rulesEt.setText("");
        rulesTv.setText(jmpctl.getRuleString());
        layoutEdit.setVisibility(View.GONE);
        layoutDisplay.setVisibility(View.VISIBLE);
    }

    private void confirmLeaveEditMode() {
        CustomDialog.create(requireContext()).setPositiveButton("确认", (dialog, which) -> goToDisplayMode())
                .setNegativeButton("取消", null).setTitle("放弃修改").setMessage("未保存的改动将会丢失")
                .setCancelable(true).show();
    }

    private void confirmResetRules() {
        CustomDialog.create(requireContext()).setPositiveButton("确认", (dialog, which) -> {
                    jmpctl.setRuleString(JumpController.DEFAULT_RULES);
                    goToDisplayMode();
                }).setNegativeButton("取消", null).setTitle("重置规则").setMessage("将恢复默认规则, 当前规则将会丢失")
                .setCancelable(true).show();
    }

    @UiThread
    private void checkAndSaveRules(String rules) {
        try {
            // check rule syntax
            JumpController.parseRules(rules);
            jmpctl.setRuleString(rules);
            goToDisplayMode();
        } catch (ParseException e) {
            CustomDialog.createFailsafe(requireContext()).setPositiveButton("确认", null)
                    .setTitle("格式错误").setMessage(e.toString()).setCancelable(true).show();
        }
    }
}
