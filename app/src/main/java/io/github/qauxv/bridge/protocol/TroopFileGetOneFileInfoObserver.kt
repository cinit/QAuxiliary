package io.github.qauxv.bridge.protocol

import android.os.Bundle
import com.tencent.biz.ProtoUtils
import io.github.qauxv.oidb.group_file_common
import io.github.qauxv.oidb.oidb_0x6d8

abstract class TroopFileGetOneFileInfoObserver : ProtoUtils.TroopProtocolObserver() {
    protected abstract fun onResult(result: Boolean, code: Int, fileInfo: group_file_common.FileInfo?)

    final override fun onResult(code: Int, data: ByteArray, bundle: Bundle) {
        if (code != 0) {
            onResult(false, code, null)
            return
        }
        val rspBody = oidb_0x6d8.RspBody()
        try {
            rspBody.mergeFrom(data)
            val getFileInfoRspBody = rspBody.file_info_rsp.get()
            if (getFileInfoRspBody.int32_ret_code.has()) {
                if (getFileInfoRspBody.int32_ret_code.get() == 0) {
                    val fileInfo = getFileInfoRspBody.file_info.get()
                    if (fileInfo != null) {
                        onResult(true, 0, fileInfo)
                    }
                } else {
                    onResult(false, getFileInfoRspBody.int32_ret_code.get(), null)
                }
            } else if (getFileInfoRspBody.file_info.has()) {
                val fileInfo2 = getFileInfoRspBody.file_info.get()
                if (fileInfo2 != null) {
                    onResult(true, 0, fileInfo2)
                }
            } else {
                onResult(false, -1, null)
            }
        } catch (unused: Exception) {
            onResult(false, -1, null)
        }
    }
}
