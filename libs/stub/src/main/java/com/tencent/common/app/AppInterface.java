package com.tencent.common.app;

import android.os.Bundle;
import mqq.app.AppRuntime;

public abstract class AppInterface extends AppRuntime {

    public AppInterface(BaseApplicationImpl baseApplicationImpl, String str) {
        throw new RuntimeException("Stub!");
    }

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    public void onDestroy() {
        super.onDestroy();
    }

}
