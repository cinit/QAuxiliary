package mqq.app;

import android.app.Activity;
import android.os.Bundle;

public class BaseActivity extends Activity {

    protected static int sResumeCount = 0;
    protected boolean mIsShadow;
    private AppRuntime app;
    private boolean isResume;

    protected boolean isShadow() {
        return false;
    }

    protected void onCreate(Bundle savedInstanceState) {
        onCreateNoRuntime(savedInstanceState);
        if (!isLatecyWaitRuntime()) {
            waitAppRuntime();
        }
        super.onCreate(savedInstanceState);
    }

    protected boolean isLatecyWaitRuntime() {
        return false;
    }

    protected void onCreateNoRuntime(Bundle savedInstanceState) {
        this.mIsShadow = isShadow();
        if (!this.mIsShadow) {
            super.onCreate(savedInstanceState);
        }
        throw new RuntimeException("Stub!");
    }

    public AppRuntime waitAppRuntime() {
        throw new RuntimeException("Stub!");
    }

    protected void onStart() {
        super.onStart();
        throw new RuntimeException("Stub!");
    }

    protected void onStop() {
        if (!this.mIsShadow) {
            super.onStop();
        }
        throw new RuntimeException("Stub!");
    }

    protected void onResume() {
        if (!this.mIsShadow) {
            super.onResume();
        }
        int i = sResumeCount + 1;
        sResumeCount = i;
        if (i > 0 && this.app != null) {
            this.app.isBackground_Pause = false;
        }
        this.isResume = true;
    }

    protected void onPause() {
        if (!this.mIsShadow) {
            super.onPause();
        }
        int i = sResumeCount - 1;
        sResumeCount = i;
        if (i <= 0 && this.app != null) {
            this.app.isBackground_Pause = true;
        }
        this.isResume = false;
    }

    protected void onDestroy() {
        if (!this.mIsShadow) {
            super.onDestroy();
        }
        throw new RuntimeException("Stub!");
    }

    public final AppRuntime getAppRuntime() {
        return this.app;
    }

    void setAppRuntime(AppRuntime app2) {
        this.app = app2;
    }

    protected void finalize() throws Throwable {
        super.finalize();
        throw new RuntimeException("Stub!");
    }

    protected void onAccountChanged() {
    }

    protected void onAccoutChangeFailed() {
    }

    protected void onLogout(Constants.LogoutReason reason) {
        finish();
    }

    public final void superFinish() {
        super.finish();
    }

    public final boolean isResume() {
        return this.isResume;
    }
}
