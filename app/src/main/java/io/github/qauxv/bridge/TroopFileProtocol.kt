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
package io.github.qauxv.bridge

import android.os.Bundle
import com.tencent.biz.ProtoUtils
import com.tencent.biz.ProtoUtils.TroopProtocolObserver
import com.tencent.mobileqq.app.QQAppInterface
import io.github.qauxv.bridge.protocol.TroopFileGetOneFileInfoObserver
import io.github.qauxv.bridge.protocol.TroopFileRenameObserver
import mqq.app.AppRuntime
import tencent.im.cs.group_file_common.group_file_common
import tencent.im.oidb.cmd0x6d6.oidb_0x6d6
import tencent.im.oidb.cmd0x6d8.oidb_0x6d8

object TroopFileProtocol {
    fun getFileInfo(
        qQAppInterface: QQAppInterface,
        gid: Long,
        fid: String,
        observer: TroopFileGetOneFileInfoObserver
    ) {
        val getFileInfoReqBody = oidb_0x6d8.GetFileInfoReqBody()
        getFileInfoReqBody.uint64_group_code.set(gid)
        getFileInfoReqBody.uint32_app_id.set(0)
        getFileInfoReqBody.str_file_id.set(fid)
        val reqBody = oidb_0x6d8.ReqBody()
        reqBody.file_info_req.set(getFileInfoReqBody)
        startServlet(qQAppInterface, observer, reqBody.toByteArray(), "OidbSvc.0x6d8_0", 0x6d8, 0)
    }

    fun renameFile(
        qQAppInterface: QQAppInterface,
        gid: Long,
        fileInfo: group_file_common.FileInfo,
        name: String,
        troopFileRenameFolderObserver: TroopFileRenameObserver
    ) {
        val renameFileReqBody = oidb_0x6d6.RenameFileReqBody()
        renameFileReqBody.uint64_group_code.set(gid)
        renameFileReqBody.uint32_app_id.set(4)
        renameFileReqBody.uint32_bus_id.set(fileInfo.uint32_bus_id.get())
        renameFileReqBody.str_file_id.set(fileInfo.str_file_id.get())
        renameFileReqBody.str_parent_folder_id.set(fileInfo.str_parent_folder_id.get())
        renameFileReqBody.str_new_file_name.set(name)
        val reqBody = oidb_0x6d6.ReqBody()
        reqBody.rename_file_req.set(renameFileReqBody)
        val bundle = Bundle()
        bundle.putString("fileId", fileInfo.str_file_id.get())
        bundle.putString("fileName", name)
        startServlet(qQAppInterface, troopFileRenameFolderObserver, reqBody.toByteArray(), "OidbSvc.0x6d6_4", 0x6d6  , 4, bundle)
    }

    private fun startServlet(
        appRuntime: AppRuntime,
        troopProtocolObserver: TroopProtocolObserver,
        date: ByteArray,
        cmd: String,
        command: Int,
        serverType: Int,
        bundle: Bundle = Bundle(),
        timeout: Long = 0
    ) {
        val method = ProtoUtils::class.java.declaredMethods.filter {
            it.parameterTypes.contentEquals(
                arrayOf(
                    AppRuntime::class.java,
                    TroopProtocolObserver::class.java,
                    ByteArray::class.java,
                    String::class.java,
                    Int::class.java,
                    Int::class.java,
                    Bundle::class.java,
                    Long::class.java
                )
            )
        }.maxBy { it.parameterTypes.size } ?: return
        method.invoke(null, appRuntime, troopProtocolObserver, date, cmd, command, serverType, bundle, timeout)
    }
}
