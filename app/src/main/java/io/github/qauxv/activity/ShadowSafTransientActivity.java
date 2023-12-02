package io.github.qauxv.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import io.github.qauxv.util.Log;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Transparent and transient activity that is used to access the SAF API without overriding the onActivityResult() method.
 * <p>
 * This activity is used by {@link io.github.qauxv.util.SafUtils}. Generally, this activity is not intended to be used directly.
 * <p>
 * See {@link io.github.qauxv.util.SafUtils} for more information.
 */
@SuppressWarnings("FieldCanBeLocal")
public class ShadowSafTransientActivity extends Activity {

    public static final String PARAM_TARGET_ACTION = "ShadowSafTransientActivity.PARAM_TARGET_ACTION";
    public static final String PARAM_SEQUENCE = "ShadowSafTransientActivity.PARAM_SEQUENCE";
    public static final String PARAM_FILE_NAME = "ShadowSafTransientActivity.PARAM_FILE_NAME";
    public static final String PARAM_MINE_TYPE = "ShadowSafTransientActivity.PARAM_MINE_TYPE";
    public static final int TARGET_ACTION_READ = 1;
    public static final int TARGET_ACTION_CREATE_AND_WRITE = 2;

    private static final int REQ_READ_FILE = 10001;
    private static final int REQ_WRITE_FILE = 10002;

    private int mSequence;
    private int mTargetAction;
    private int mOriginRequest;
    private String mMimeType = null;
    private String mFileName = null;

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
        mTargetAction = extras.getInt(PARAM_TARGET_ACTION, -1);
        mSequence = extras.getInt(PARAM_SEQUENCE, -1);
        mFileName = extras.getString(PARAM_FILE_NAME);
        mMimeType = extras.getString(PARAM_MINE_TYPE);
        if (mTargetAction < 0 || mSequence < 0) {
            finish();
            return;
        }
        if (!sRequestMap.containsKey(mSequence)) {
            Log.e("sequence not found: " + mSequence + ", finishing");
            finish();
            return;
        }
        Intent intent;
        switch (mTargetAction) {
            case TARGET_ACTION_READ: {
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                // any mine type
                intent.setType(Objects.requireNonNullElse(mMimeType, "*/*"));
                mOriginRequest = REQ_READ_FILE;
                break;
            }
            case TARGET_ACTION_CREATE_AND_WRITE: {
                intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                if (mFileName != null) {
                    intent.putExtra(Intent.EXTRA_TITLE, mFileName);
                }
                intent.setType(Objects.requireNonNullElse(mMimeType, "*/*"));
                mOriginRequest = REQ_WRITE_FILE;
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown target action: " + mTargetAction);
        }
        if (!sRequestMap.containsKey(mSequence)) {
            throw new AssertionError("Unknown sequence: " + mSequence + ", are we in the same process?");
        }
        // query SAF here
        try {
            startActivityForResult(intent, mOriginRequest);
        } catch (ActivityNotFoundException e) {
            Request request = sRequestMap.get(mSequence);
            if (request != null) {
                request.callback.onException(e);
            }
            sRequestMap.remove(mSequence);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == mOriginRequest) {
            Uri resultUri = (resultData != null) ? resultData.getData() : null;
            Request request = sRequestMap.get(mSequence);
            if (request != null) {
                request.callback.onResult(resultUri);
            }
            sRequestMap.remove(mSequence);
            finish();
        }
    }

    public static class Request {

        public int sequence;
        public int targetAction;
        public String mimeType;
        public String fileName;
        public RequestResultCallback callback;

        public Request(int sequence, int targetAction, String mimeType, String fileName, RequestResultCallback callback) {
            this.sequence = sequence;
            this.targetAction = targetAction;
            this.mimeType = mimeType;
            this.fileName = fileName;
            this.callback = callback;
        }
    }

    private static final ConcurrentHashMap<Integer, Request> sRequestMap = new ConcurrentHashMap<>();
    private static final AtomicInteger sSequenceGenerator = new AtomicInteger(10000);

    public static void startActivityForRequest(@NonNull Context host, int targetAction,
                                               @Nullable String mimeType, @Nullable String fileName,
                                               @NonNull RequestResultCallback callback) {
        int sequence = sSequenceGenerator.incrementAndGet();
        sRequestMap.put(sequence, new Request(sequence, targetAction, mimeType, fileName, callback));
        Intent intent = new Intent(host, ShadowSafTransientActivity.class);
        intent.putExtra(ShadowSafTransientActivity.PARAM_SEQUENCE, sequence);
        intent.putExtra(ShadowSafTransientActivity.PARAM_TARGET_ACTION, targetAction);
        intent.putExtra(ShadowSafTransientActivity.PARAM_FILE_NAME, fileName);
        intent.putExtra(ShadowSafTransientActivity.PARAM_MINE_TYPE, mimeType);
        host.startActivity(intent);
    }

    public interface RequestResultCallback {

        /**
         * Called when the request is finished.
         *
         * @param uri the uri of the file, may be null if the request is canceled.
         */
        @UiThread
        void onResult(@Nullable Uri uri);

        /**
         * Called when an exception is thrown.
         *
         * @param e the exception, typically an {@link ActivityNotFoundException}.
         */
        @UiThread
        void onException(@NonNull Throwable e);
    }
}
