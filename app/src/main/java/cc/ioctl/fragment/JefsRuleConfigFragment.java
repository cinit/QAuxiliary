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
import cc.ioctl.hook.JumpController;
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
        return "????????????";
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

        ViewGroup _tmp = ViewBuilder.newListItemHookSwitchInit(context, "?????????", "??????????????????????????????", JumpController.INSTANCE);
        mainLayout.addView(_tmp, LayoutHelper.newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT, 0));

        int __10 = dip2px(context, 10);
        int __5 = dip2px(context, 5);

        TextView _tv_rules = new TextView(context);
        _tv_rules.setTextSize(16);
        _tv_rules.setText("??????:");
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
                _tmp_1.setText("?????????????????????????????? \"????????????" + appLabel + " ??????????????????\" ?????????, " +
                        "???????????????????????????" + appLabel + "????????????????????????APP????????????????????????(???????????????)");
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
            editBtn.setText("????????????");
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
                _tmp_1.setText("???????????????????????????????????????\n" +
                        "??????????????? * ??? **\n" +
                        "????????????: ??????,?????????;\n????????????????????????, ??????????????????, \n???: ??????,?????????1,?????????2,?????????3;\n" +
                        "?????????A/D/Q??????: ??????(A), ??????(D), ????????????(Q), ????????????3???(P/C/A)\n" +
                        "?????????(P): ???  A,P:com.tencent.mm;    ?????????????????????\n" +
                        "?????????(C): ???  D,C:de.robv.android.xposed.installer/.WelcomeActivity;  ???????????????Xposed????????????\n"
                        +
                        "?????????(A): ???  Q,A:android.intent.action.DIAL;    ?????????????????????????????????\n" +
                        "???????????????: aa.bbb.*.dddd ???????????? aa.bbb.ccccc.dddd ???????????? aa.bbb.cc.ee.dddd\n" +
                        " aa.bbb.**.dddd ???????????? aa.bbb.cc.dddd ??? aa.bbb.cc.ee.dddd ???????????? aa.bbb.dddd");
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
            saveBtn.setText("??????");
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
                cancelBtn.setText("??????");
//                cancelBtn.setTextColor(ResUtils.skin_blue);
                cancelBtn.setOnClickListener(this);

                TextView resetBtn = new TextView(context);
//                resetBtn.setTextColor(ResUtils.skin_black);
                resetBtn.setTextSize(16);
                resetBtn.setId(R.id.jefsRulesResetButton);
                resetBtn.setGravity(Gravity.CENTER);
                resetBtn.setPadding(__10, __5, __10, __10 / 2);
                resetBtn.setText("??????????????????");
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
                    .setPositiveButton("??????", (dialog, which) -> finishFragment())
                    .setNegativeButton("??????", null)
                    .setTitle("??????")
                    .setMessage("??????????????????????????????")
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
        CustomDialog.create(requireContext()).setPositiveButton("??????", (dialog, which) -> goToDisplayMode())
                .setNegativeButton("??????", null).setTitle("????????????").setMessage("??????????????????????????????")
                .setCancelable(true).show();
    }

    private void confirmResetRules() {
        CustomDialog.create(requireContext()).setPositiveButton("??????", (dialog, which) -> {
                    jmpctl.setRuleString(JumpController.DEFAULT_RULES);
                    goToDisplayMode();
                }).setNegativeButton("??????", null).setTitle("????????????").setMessage("?????????????????????, ????????????????????????")
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
            CustomDialog.createFailsafe(requireContext()).setPositiveButton("??????", null)
                    .setTitle("????????????").setMessage(e.toString()).setCancelable(true).show();
        }
    }
}
