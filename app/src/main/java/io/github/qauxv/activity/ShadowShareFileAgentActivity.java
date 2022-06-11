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

package io.github.qauxv.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import cc.ioctl.util.Reflex;
import io.github.qauxv.SyncUtils;
import io.github.qauxv.lifecycle.ShadowFileProvider;
import io.github.qauxv.ui.WindowIsTranslucent;
import io.github.qauxv.util.Log;
import java.io.File;
import java.util.Objects;

public class ShadowShareFileAgentActivity extends Activity implements WindowIsTranslucent {

    public static final String TARGET_INTENT = "ShadowShareFileAgentActivity.TARGET_INTENT";

    public static final String SHADOW_FILE_PROVIDER_BINDER = "ShadowFileProvider.BINDER";

    public static final String TARGET_FILE_PATH = "ShadowShareFileAgentActivity.TARGET_FILE_PATH";
    public static final String TARGET_FILE_URI = "ShadowShareFileAgentActivity.TARGET_FILE_URI";
    public static final String TARGET_FILE_ATTR_MIME_TYPE = "ShadowShareFileAgentActivity.TARGET_FILE_ATTR_MIME_TYPE";
    public static final String TARGET_FILE_ATTR_DISPLAY_NAME = "ShadowShareFileAgentActivity.TARGET_FILE_ATTR_DISPLAY_NAME";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_Translucent_NoTitleBar);
        super.onCreate(savedInstanceState);
        Intent startIntent = getIntent();
        Bundle extras = startIntent.getExtras();
        if (extras == null) {
            finish();
            return;
        }
        Intent targetIntent = extras.getParcelable(TARGET_INTENT);
        String targetFilePath = extras.getString(TARGET_FILE_PATH);
        String targetFileUri = extras.getString(TARGET_FILE_URI);
        if (targetIntent == null || TextUtils.isEmpty(targetFilePath) && TextUtils.isEmpty(targetFileUri)) {
            Log.e("targetIntent or targetFilePath and targetFileUri is null");
            finish();
            return;
        }
        Uri uri;
        IBinder binder = extras.getBinder(SHADOW_FILE_PROVIDER_BINDER);
        if (binder != null) {
            ShadowFileProvider.attachShadowTmpFileProviderBinder(binder);
        }
        if (TextUtils.isEmpty(targetFileUri)) {
            // by path
            File targetFile = new File(targetFilePath);
            if (!targetFile.exists()) {
                Log.e("targetFile not exists: " + targetFilePath);
                Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", targetFile);
        } else {
            uri = Uri.parse(targetFileUri);
        }
        targetIntent.setDataAndType(uri, targetIntent.getType());
        targetIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        targetIntent.putExtra(Intent.EXTRA_STREAM, uri);
        try {
            startActivity(targetIntent);
            finish();
        } catch (ActivityNotFoundException e) {
            new AlertDialog.Builder(this)
                    .setTitle(Reflex.getShortClassName(e))
                    .setMessage(e.getMessage())
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                    .setOnCancelListener(dialog -> finish())
                    .setOnDismissListener(dialog -> finish())
                    .show();
        }
    }

    public static void startShareFileActivity(@NonNull Context context, @NonNull Intent intent, @NonNull File targetFile) {
        Objects.requireNonNull(context, "context == null");
        Objects.requireNonNull(intent, "intent == null");
        Objects.requireNonNull(targetFile, "targetFile == null");
        if (!targetFile.exists()) {
            return;
        }
        Intent startIntent = new Intent(context, ShadowShareFileAgentActivity.class);
        startIntent.putExtra(TARGET_INTENT, intent);
        startIntent.putExtra(TARGET_FILE_PATH, targetFile.getAbsolutePath());
        context.startActivity(startIntent);
    }

    public static void startShareFileActivity(@NonNull Context context, @NonNull Intent intent, @NonNull Uri targetUri) {
        Objects.requireNonNull(context, "context == null");
        Objects.requireNonNull(intent, "intent == null");
        Objects.requireNonNull(targetUri, "targetUri == null");
        Intent startIntent = new Intent(context, ShadowShareFileAgentActivity.class);
        startIntent.putExtra(TARGET_INTENT, intent);
        startIntent.putExtra(TARGET_FILE_URI, targetUri.toString());
        context.startActivity(startIntent);
    }

    public static void startShareFileActivity(@NonNull Context context, @NonNull Intent intent,
                                              @NonNull String displayName, @Nullable String mimeType,
                                              @NonNull ParcelFileDescriptor pfd) {
        if (!SyncUtils.isMainProcess()) {
            throw new IllegalArgumentException("fd only support in main process");
        }
        Objects.requireNonNull(context, "context == null");
        Objects.requireNonNull(intent, "intent == null");
        Objects.requireNonNull(displayName, "displayName == null");
        Objects.requireNonNull(pfd, "pfd == null");
        Intent startIntent = new Intent(context, ShadowShareFileAgentActivity.class);
        startIntent.putExtra(TARGET_INTENT, intent);
        startIntent.putExtra(TARGET_FILE_ATTR_DISPLAY_NAME, displayName);
        startIntent.putExtra(TARGET_FILE_ATTR_MIME_TYPE, mimeType);
        String uriStr = ShadowFileProvider.addItem(displayName, mimeType, pfd);
        startIntent.putExtra(TARGET_FILE_URI, uriStr);
        IBinder binder = ShadowFileProvider.getShadowTmpFileProvider().asBinder();
        Bundle b = new Bundle();
        b.putBinder(SHADOW_FILE_PROVIDER_BINDER, binder);
        startIntent.putExtras(b);
        context.startActivity(startIntent);
    }
}
