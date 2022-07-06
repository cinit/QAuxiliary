package io.github.qauxv.bridge.protocol

import android.os.Bundle
import com.tencent.biz.ProtoUtils
import tencent.im.oidb.cmd0x6d6.oidb_0x6d6

abstract class TroopFileRenameObserver : ProtoUtils.TroopProtocolObserver() {
    protected abstract fun onResult(result: Boolean, code: Int, fileName: String, fileId: String)

    final override fun onResult(code: Int, data: ByteArray, bundle: Bundle) {
        val fileId: String = bundle.getString("fileId", "")
        val fileName: String = bundle.getString("fileName", "")
        if (code != 0) {
            onResult(false, code, fileName, fileId)
            return
        }
        val rspBody = oidb_0x6d6.RspBody()
        try {
            rspBody.mergeFrom(data)
            val renameFileRspBody = rspBody.rename_file_rsp.get()
            if (!renameFileRspBody.int32_ret_code.has()) {
                onResult(false, -1, fileName, fileId)
            } else if (renameFileRspBody.int32_ret_code.get() == 0) {
                onResult(true, 0, fileName, fileId)
            } else {
                onResult(false, renameFileRspBody.int32_ret_code.get(), fileName, fileId)
            }
        } catch (unused: Exception) {
            onResult(false, -1, fileName, fileId)
        }
    }
}

