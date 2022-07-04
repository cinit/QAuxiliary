package com.tencent.biz;

import android.os.Bundle;

public class ProtoUtils {
    public static abstract class TroopProtocolObserver {
        public abstract void onResult(int i, byte[] bArr, Bundle bundle);
    }
}
