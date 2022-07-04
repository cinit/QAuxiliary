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

package me.ketal.data

class TroopFileInfo(info: Any) {
    val path: String
    val fileName: String
    val busId: Int
    val size: Long

    init {
        // TroopFileInfo{TAG='TroopFileInfo', Id=UUID, str_file_path='/UUID',
        // str_file_name='aaaaaa.png', uint64_file_size=9191837, uint32_bus_id=102, uint32_upload_uin=10001,
        // uint64_uploaded_size=9191837, uint32_upload_time=1653382557, uint32_dead_time=0, uint32_modify_time=1656913958,
        // uint32_download_times=0, str_uploader_name='aaaaa', Status=7, _sStatus='null',
        // ProgressValue=0, ErrorCode=0, LocalFile='null', UploadCreateTime=0, Unread=false,
        // ThumbnailFile_Small='/storage/emulated/0/Android/data/com.tencent.mobileqq/Tencent/QQfile_recv/.trooptmp/[Thumb]gid-UUID',
        // ThumbnailFile_Large='null', IsGhost=false, IsNewStatus=false, NickName='aaaaa', lastNickNameUpdateMS=97431121}
        with(info.toString()) {
            path = substringAfter("str_file_path='").substringBefore("'")
            fileName = substringAfter("str_file_name='").substringBefore("'")
            busId = substringAfter("uint32_bus_id=").substringBefore(",").toInt()
            size = substringAfter("uint64_file_size=").substringBefore(",").toLong()
        }
    }
}
