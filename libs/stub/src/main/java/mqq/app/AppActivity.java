package mqq.app;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;


public class AppActivity extends Activity {

    protected static int sResumeCount = 0;

    protected boolean mIsResume = false;

    protected AppRuntime mRuntime = null;

    public final AppRuntime getAppRuntime() {
        return this.mRuntime;
    }

    void setAppRuntime(AppRuntime runtime) {
        this.mRuntime = runtime;
    }

    protected void requestWindowFeature(Intent intent) {
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        throw new RuntimeException("Stub!");
    }

    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        throw new RuntimeException("Stub!");
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        throw new RuntimeException("Stub!");
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        throw new RuntimeException("Stub!");
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    public void realOnCreate() {
        throw new RuntimeException("Stub!");
    }

    protected void onNewIntent(Intent intent) {
        throw new RuntimeException("Stub!");
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        throw new RuntimeException("Stub!");
    }

    protected void onStart() {
        super.onStart();
        throw new RuntimeException("Stub!");
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        throw new RuntimeException("Stub!");
    }

    protected boolean doDispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    protected void onStop() {
        super.onStop();
        throw new RuntimeException("Stub!");
    }

    protected void onResume() {
        super.onResume();
        throw new RuntimeException("Stub!");
    }

    public void onConfigurationChanged(Configuration newConfig) {
        throw new RuntimeException("Stub!");
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        throw new RuntimeException("Stub!");
    }

    public void onBackPressed() {
        throw new RuntimeException("Stub!");
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        throw new RuntimeException("Stub!");
    }

    protected void onUserLeaveHint() {
        throw new RuntimeException("Stub!");
    }

    protected void onPause() {
        super.onPause();
        throw new RuntimeException("Stub!");
    }

    protected void onAccountChanged() {
    }

    protected void onAccoutChangeFailed() {
    }

    protected void onLogout(Constants.LogoutReason reason) {
        finish();
    }

    protected boolean doOnCreate(Bundle savedInstanceState) {
        throw new RuntimeException("Stub!");
    }

    protected void doOnPostCreate(Bundle savedInstanceState) {
        throw new RuntimeException("Stub!");
    }

    protected void doOnRestoreInstanceState(Bundle savedInstanceState) {
    }

    protected void doOnSaveInstanceState(Bundle outState) {
    }

    protected void doOnStart() {
    }

    protected void doOnNewIntent(Intent intent) {
    }

    protected void doOnActivityResult(int requestCode, int requestCode2, Intent data) {
    }

    protected void doOnResume() {
    }

    protected void doOnWindowFocusChanged(boolean isFocused) {
    }

    protected void doOnConfigurationChanged(Configuration newConfig) {
    }

    protected void doOnBackPressed() {
        super.onBackPressed();
        throw new RuntimeException("Stub!");
    }

    protected boolean doOnKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    protected void doOnUserLeaveHint() {
    }

    protected void doOnPause() {
    }

    protected void doOnStop() {
    }

    protected void doOnDestroy() {
        throw new RuntimeException("Stub!");
    }

    public final boolean isResume() {
        return this.mIsResume;
    }

    public void superFinish() {
        super.finish();
    }

    public boolean showPreview() {
        return false;
    }

    public boolean preloadData(AppRuntime runtime, boolean more) {
        return false;
    }

    public void preloadUi() {
    }
}
