/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2025 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package io.github.duzhaokun123.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import com.github.kyuubiran.ezxhelper.utils.loadClass
import io.github.qauxv.ui.WindowIsTranslucent
import io.github.qauxv.util.Log
import xyz.nextalone.util.method

class PictureEditProxyActivity : Activity(), WindowIsTranslucent {

    fun interface PictureEditCallback {
        fun onPictureEdited(path: String)
    }

    companion object {

        const val EXTRA_PATH = "PATH"

        /**
         * 启动图片编辑 必须和 [PictureEditProxyActivity] 在一个进程
         */
        @JvmStatic
        fun startEditPicture(activity: Activity, path: String, onResult: PictureEditCallback) {
            callbacks[path] = onResult
            Log.d("startEditPicture: $path")
            activity.startActivity(Intent(activity, PictureEditProxyActivity::class.java).apply {
                putExtra(EXTRA_PATH, path)
            })
        }

        private val callbacks = mutableMapOf<String, PictureEditCallback>()
    }

    val requestCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("PictureEditProxyActivity")

        if (intent.hasExtra("PhotoConst.SINGLE_PHOTO_PATH")) {
            onGetPictureEdited()
        } else {
            startEditPicture()
        }
    }

    fun startEditPicture() {
        val path = intent.getStringExtra(EXTRA_PATH)
        val method_QRoute_api = "Lcom/tencent/mobileqq/qroute/QRoute;->api(Ljava/lang/Class;)Lcom/tencent/mobileqq/qroute/QRouteApi;".method
        val class_IAELaunchEditPic = loadClass("com.tencent.aelight.camera.qqstory.api.IAELaunchEditPic")
        val method_IAELaunchEditPic_startEditPic_8 =
            "Lcom/tencent/aelight/camera/qqstory/api/IAELaunchEditPic;->startEditPic(Landroid/app/Activity;Ljava/lang/String;ZZZZZI)Landroid/content/Intent;".method
        val AELaunchEditPic = method_QRoute_api(null, class_IAELaunchEditPic)
        val editPictureIntent = method_IAELaunchEditPic_startEditPic_8(AELaunchEditPic, this, path, true, true, true, true, true, requestCode) as Intent
        startActivity(editPictureIntent)
        finish()
    }

    fun onGetPictureEdited() {
        val path = intent.getStringExtra(EXTRA_PATH)
        val callback = callbacks.remove(path)
        if (callback == null) {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("图片编辑完成 但回调为 $path 丢失")
                .setOnDismissListener { finish() }
                .show()
        } else {
            callback.onPictureEdited(intent.getStringExtra("PhotoConst.SINGLE_PHOTO_PATH") ?: "")
            finish()
        }
    }
}