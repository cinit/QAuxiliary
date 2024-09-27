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
import static cc.ioctl.util.DebugUtils.csvenc;
import static cc.ioctl.util.DebugUtils.en;
import static cc.ioctl.util.HostStyledViewBuilder.subtitle;
import static cc.ioctl.util.LayoutHelper.dip2px;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import cc.ioctl.util.ExfriendManager;
import cc.ioctl.util.HostStyledViewBuilder;
import cc.ioctl.util.LayoutHelper;
import cc.ioctl.util.data.FriendRecord;
import cc.ioctl.util.ui.FaultyDialog;
import com.tencent.mobileqq.widget.BounceScrollView;
import io.github.qauxv.R;
import io.github.qauxv.activity.SettingsUiFragmentHostActivity;
import io.github.qauxv.base.IEntityAgent;
import io.github.qauxv.base.ISwitchCellAgent;
import io.github.qauxv.base.IUiItemAgent;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.fragment.BaseRootLayoutFragment;
import io.github.qauxv.hook.CommonClickableStaticFunctionItem;
import io.github.qauxv.util.Toasts;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import kotlinx.coroutines.flow.MutableStateFlow;

public class FriendListExportFragment extends BaseRootLayoutFragment {

    private static final int R_ID_CHECKBOX_CSV = 0x300AFF61;
    private static final int R_ID_CHECKBOX_JSON = 0x300AFF62;
    private static final int R_ID_RB_CRLF = 0x300AFF63;
    private static final int R_ID_RB_CR = 0x300AFF65;
    private static final int R_ID_RB_LF = 0x300AFF64;
    private static final int R_ID_CB_FRIENDS = 0x300AFF66;
    private static final int R_ID_CB_EXFRIENDS = 0x300AFF67;

    @Nullable
    @Override
    public View doOnCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                               @Nullable Bundle savedInstanceState) {
        setTitle("导出好友列表");
        Context ctx = inflater.getContext();
        int firstTextColor = ResourcesCompat.getColor(ctx.getResources(), R.color.firstTextColor, ctx.getTheme());
        LinearLayout ll = new LinearLayout(ctx);
        ll.setOrientation(LinearLayout.VERTICAL);
        ViewGroup.LayoutParams mmlp = new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        LinearLayout __ll = new LinearLayout(ctx);
        __ll.setOrientation(LinearLayout.VERTICAL);
        final ViewGroup bounceScrollView = new BounceScrollView(ctx, null);
        bounceScrollView.setLayoutParams(mmlp);
        bounceScrollView.setId(R.id.rootBounceScrollView);
        ll.setId(R.id.rootMainLayout);
        bounceScrollView.addView(ll, new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        LinearLayout.LayoutParams fixlp = new LinearLayout.LayoutParams(MATCH_PARENT, dip2px(ctx, 48));
        RelativeLayout.LayoutParams __lp_l = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        int mar = (int) (dip2px(ctx, 12) + 0.5f);
        __lp_l.setMargins(mar, 0, mar, 0);
        __lp_l.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        __lp_l.addRule(RelativeLayout.CENTER_VERTICAL);
        RelativeLayout.LayoutParams __lp_r = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        __lp_r.setMargins(mar, 0, mar, 0);
        __lp_r.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        __lp_r.addRule(RelativeLayout.CENTER_VERTICAL);

        LinearLayout.LayoutParams stdlp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        stdlp.setMargins(mar, mar / 4, mar, mar / 4);

        ll.addView(subtitle(ctx, "导出范围"));

        final CheckBox exfonly = new CheckBox(ctx);
        exfonly.setText("历史好友");
        exfonly.setTextColor(firstTextColor);
        exfonly.setId(R_ID_CB_EXFRIENDS);
        ll.addView(exfonly, stdlp);
        final CheckBox frionly = new CheckBox(ctx);
        frionly.setText("当前好友");
        frionly.setTextColor(firstTextColor);
        frionly.setId(R_ID_CB_FRIENDS);
        ll.addView(frionly, stdlp);

        frionly.setChecked(true);

        ll.addView(subtitle(ctx, "导出格式"));

        final CheckBox cbCsv = new CheckBox(ctx);
        cbCsv.setText("CSV");
        cbCsv.setTextColor(firstTextColor);
        cbCsv.setId(R_ID_CHECKBOX_CSV);
        ll.addView(cbCsv, stdlp);
        final CheckBox cbJson = new CheckBox(ctx);
        cbJson.setText("JSON");
        cbJson.setTextColor(firstTextColor);
        cbJson.setId(R_ID_CHECKBOX_JSON);
        ll.addView(cbJson, stdlp);

        LinearLayout llcsvopt = new LinearLayout(ctx);
        llcsvopt.setOrientation(LinearLayout.VERTICAL);
        llcsvopt.addView(HostStyledViewBuilder.subtitle(ctx, "CSV 设定"));

        final RadioGroup gcsvcrlf = new RadioGroup(ctx);
        gcsvcrlf.setOrientation(RadioGroup.VERTICAL);
        ll.addView(gcsvcrlf, stdlp);
        gcsvcrlf.addView(subtitle(ctx, "换行符"));
        RadioButton crlf = new RadioButton(ctx);
        crlf.setText("CRLF - \\r\\n");
        crlf.setTextColor(firstTextColor);

        crlf.setId(R_ID_RB_CRLF);
        gcsvcrlf.addView(crlf, stdlp);
        RadioButton cr = new RadioButton(ctx);
        cr.setText("CR - \\r");
        cr.setTextColor(firstTextColor);

        cr.setId(R_ID_RB_CR);
        gcsvcrlf.addView(cr, stdlp);
        RadioButton lf = new RadioButton(ctx);
        lf.setText("LF - \\n");
        lf.setTextColor(firstTextColor);
        lf.setId(R_ID_RB_LF);
        gcsvcrlf.addView(lf, stdlp);

        View.OnClickListener formatListener = v -> {
            switch (v.getId()) {
                case R_ID_CHECKBOX_CSV:
                    cbCsv.setChecked(true);
                    cbJson.setChecked(false);
                    gcsvcrlf.setVisibility(View.VISIBLE);
                    break;
                case R_ID_CHECKBOX_JSON:
                    cbCsv.setChecked(false);
                    cbJson.setChecked(true);
                    gcsvcrlf.setVisibility(View.GONE);

            }
        };
        cbCsv.setOnClickListener(formatListener);
        cbJson.setOnClickListener(formatListener);

        lf.setChecked(true);
        formatListener.onClick(cbCsv);

        ll.addView(subtitle(ctx, "请输入要导出列表的 QQ 号 (默认为当前登录的 QQ 号):"));
        final EditText etuin = new EditText(ctx);
        etuin.setTextSize(LayoutHelper.dip2sp(ctx, 18));
        etuin.setTextColor(firstTextColor);
        ll.addView(etuin, stdlp);
        long currentUin = -1;
        try {
            currentUin = Long.parseLong(AppRuntimeHelper.getAccount());
            etuin.setHint(currentUin + "");
        } catch (Throwable e) {
            etuin.setHint("输入 QQ 号");
        }

        ll.addView(subtitle(ctx, "导出文件保存路径 (默认在下载目录下):"));
        final EditText expath = new EditText(ctx);
        expath.setTextSize(LayoutHelper.dip2sp(ctx, 18));
        expath.setTextColor(firstTextColor);
        String refpath = getDefaultOutputFile(ctx);
        expath.setHint(refpath);
        ll.addView(expath, stdlp);

        Button exportbtn = new Button(ctx);
        exportbtn.setText("导出");

        final long refuin = currentUin;

        exportbtn.setOnClickListener(v -> {
            String t_uin = etuin.getText().toString();
            if (t_uin.equals("")) {
                t_uin = "" + refuin;
            }
            String export = expath.getText().toString();
            if (export.equals("")) {
                export = expath.getHint().toString();
            }
            int format = cbCsv.isChecked() ? 1 : 2;
            int rn = gcsvcrlf.getCheckedRadioButtonId();
            doExportFile(t_uin, frionly.isChecked(), exfonly.isChecked(), export, format, rn);
        });
        ll.addView(exportbtn, stdlp);

        __ll.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        LinearLayout.LayoutParams _lp_fat = new LinearLayout.LayoutParams(MATCH_PARENT, 0);
        _lp_fat.weight = 1;
        setRootLayoutView(bounceScrollView);
        return bounceScrollView;
    }

    private void doExportFile(String suin, boolean fri, boolean exf, String output, int format, int crlf) {
        Context ctx = requireContext();
        long luin;
        try {
            luin = Long.parseLong(suin);
        } catch (NumberFormatException ignored) {
            Toasts.error(ctx, "请输入有效 QQ 号", Toast.LENGTH_LONG);
            return;
        }
        if (!exf && !fri) {
            Toasts.error(ctx, "请至少选择一个进行导出", Toast.LENGTH_LONG);
            return;
        }
        String rn;
        ExfriendManager exm = ExfriendManager.get(luin);
        StringBuilder sb = new StringBuilder();
        switch (format) {
            case 1: {
                // CSV
                switch (crlf) {
                    case R_ID_RB_CRLF:
                        rn = "\r\n";
                        break;
                    case R_ID_RB_CR:
                        rn = "\r";
                        break;
                    case R_ID_RB_LF:
                        rn = "\n";
                        break;
                    default:
                        Toasts.error(ctx, "无效换行符", Toast.LENGTH_LONG);
                        return;
                }
                if (fri) {
                    Map<Long, FriendRecord> friends = exm.getPersons();
                    for (Map.Entry<Long, FriendRecord> ent : friends.entrySet()) {
                        long uin = ent.getKey();
                        FriendRecord rec = ent.getValue();
                        if (rec.friendStatus != FriendRecord.STATUS_EXFRIEND) {
                            sb.append(uin).append(",");
                            sb.append(csvenc(rec.remark)).append(",");
                            sb.append(csvenc(rec.nick)).append(",");
                            sb.append(rec.friendStatus).append(rn);
                        }
                    }
                }
                if (exf) {
                    Map<Long, FriendRecord> friends = exm.getPersons();
                    for (Map.Entry<Long, FriendRecord> ent : friends.entrySet()) {
                        long uin = ent.getKey();
                        FriendRecord rec = ent.getValue();
                        if (!fri || rec.friendStatus != FriendRecord.STATUS_FRIEND_MUTUAL) {
                            sb.append(uin).append(",");
                            sb.append(csvenc(rec.remark)).append(",");
                            sb.append(csvenc(rec.nick)).append(",");
                            sb.append(rec.friendStatus).append(rn);
                        }
                    }
                }
                if (sb.length() > 1 && sb.charAt(sb.length() - 1) < 17) {
                    sb.delete(sb.length() - rn.length(), sb.length());
                }
                break;
            }
            case 2: {
                // JSON
                sb.append('[');
                if (fri) {
                    Map<Long, FriendRecord> friends = exm.getPersons();
                    for (Map.Entry<Long, FriendRecord> ent : friends.entrySet()) {
                        long uin = ent.getKey();
                        FriendRecord rec = ent.getValue();
                        if (rec.friendStatus != FriendRecord.STATUS_EXFRIEND) {
                            sb.append('{');
                            sb.append("\"uin\":").append(uin).append(",");
                            sb.append("\"remark\":").append(en(rec.remark)).append(",");
                            sb.append("\"nick\":").append(en(rec.nick)).append(",");
                            sb.append("\"status\":").append(rec.friendStatus).append('}');
                            sb.append(',');
                        }
                    }
                }
                if (exf) {
                    Map<Long, FriendRecord> friends = exm.getPersons();
                    for (Map.Entry<Long, FriendRecord> ent : friends.entrySet()) {
                        long uin = ent.getKey();
                        FriendRecord rec = ent.getValue();
                        if (!fri || rec.friendStatus != FriendRecord.STATUS_FRIEND_MUTUAL) {
                            sb.append('{');
                            sb.append("\"uin\":").append(uin).append(",");
                            sb.append("\"remark\":").append(en(rec.remark)).append(",");
                            sb.append("\"nick\":").append(en(rec.nick)).append(",");
                            sb.append("\"status\":").append(rec.friendStatus).append('}');
                            sb.append(',');
                        }
                    }
                }
                if (sb.length() > 1 && sb.charAt(sb.length() - 1) == ',') {
                    sb.delete(sb.length() - 1, sb.length());
                }
                sb.append(']');
                break;
            }
            default: {
                Toasts.error(ctx, "格式转换错误", Toast.LENGTH_LONG);
                return;
            }
        }
        if (sb.length() == 0) {
            Toasts.error(ctx, "格式转换错误", Toast.LENGTH_LONG);
            return;
        }
        File f = new File(output);
        if (!f.exists()) {
            try {
                f.createNewFile();
                FileOutputStream fout = new FileOutputStream(f);
                fout.write(sb.toString().getBytes());
                fout.flush();
                fout.close();
                Toasts.success(ctx, "操作完成");
            } catch (IOException e) {
                FaultyDialog.show(ctx, "创建输出文件失败", e);
            }
        }
    }

    @NonNull
    private static String getDefaultOutputFile(@NonNull Context ctx) {
        // qauxv_firiends_yyyy-MM-dd_HH-mm-ss.txt
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT);
        String name = "qauxv_firiends_" + sdf.format(new Date()) + ".txt";
        // save to Android standard Download folder
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloadDir == null) {
            downloadDir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        }
        return new File(downloadDir, name).getAbsolutePath();
    }

    @UiItemAgentEntry
    public static class ItemEntry extends CommonClickableStaticFunctionItem {

        public static final ItemEntry INSTANCE = new ItemEntry();

        private ItemEntry() {
        }

        @NonNull
        @Override
        public IUiItemAgent getUiItemAgent() {
            return this;
        }

        @NonNull
        @Override
        public String[] getUiItemLocation() {
            return FunctionEntryRouter.Locations.Auxiliary.FRIEND_CATEGORY;
        }

        @NonNull
        @Override
        public String getItemAgentProviderUniqueIdentifier() {
            return getClass().getName();
        }

        @NonNull
        @Override
        public Function1<IEntityAgent, String> getTitleProvider() {
            return agent -> "导出好友列表";
        }

        @Nullable
        @Override
        public Function2<IEntityAgent, Context, CharSequence> getSummaryProvider() {
            return (agent, ctx) -> "支持 CSV/JSON 格式";
        }

        @Nullable
        @Override
        public MutableStateFlow<String> getValueState() {
            return null;
        }

        @Nullable
        @Override
        public Function1<IUiItemAgent, Boolean> getValidator() {
            return null;
        }

        @Nullable
        @Override
        public ISwitchCellAgent getSwitchProvider() {
            return null;
        }

        @Nullable
        @Override
        public Function3<IUiItemAgent, Activity, View, Unit> getOnClickListener() {
            return (agent, activity, view) -> {
                SettingsUiFragmentHostActivity.startFragmentWithContext(activity, FriendListExportFragment.class);
                return Unit.INSTANCE;
            };
        }

        @Nullable
        @Override
        public Function2<IUiItemAgent, Context, String[]> getExtraSearchKeywordProvider() {
            return null;
        }
    }
}
