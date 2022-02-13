/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * context software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by QAuxiliary contributors.
 *
 * context software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with context software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */
package io.github.qauxv.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import cc.ioctl.hook.FakeBatteryHook
import cc.ioctl.util.LayoutHelper.*
import com.tencent.mobileqq.widget.BounceScrollView
import io.github.qauxv.BuildConfig
import io.github.qauxv.R
import io.github.qauxv.activity.SettingsUiFragmentHostActivity
import io.github.qauxv.core.InjectDelayableHooks
import io.github.qauxv.ui.ResUtils
import io.github.qauxv.util.LicenseStatus
import io.github.qauxv.util.LicenseStatus.CURRENT_EULA_VERSION
import io.github.qauxv.util.Log
import io.github.qauxv.util.Toasts
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class EulaFragment : BaseSettingFragment(), View.OnClickListener {

    override fun getTitle() = "EULA"

    private var mCheckBoxHaveRead: CheckBox? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = inflater.context
        val bounceScrollView: ViewGroup = BounceScrollView(context, null).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            id = R.id.rootBounceScrollView
        }
        val ll = LinearLayout(context).apply {
            id = R.id.rootMainLayout
            orientation = LinearLayout.VERTICAL
            setPadding(dip2px(context, 16f), 0, dip2px(context, 16f), 0)

            if (LicenseStatus.hasEulaUpdated()) {
                val tv_updated = TextView(context)
                tv_updated.textSize = 22f
                tv_updated.gravity = Gravity.CENTER
                tv_updated.paint.isFakeBoldText = true
                tv_updated.setTextColor(ResourcesCompat.getColor(resources, R.color.colorAccent, context.theme))
                tv_updated.text = "用户协议发生变更, 您需要同意接受下方《协议》及《隐私条款》才能继续使用本模块"
                addView(tv_updated, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            }
            var tv = TextView(context)
            tv.textSize = 28f
            tv.paint.isFakeBoldText = true
            tv.gravity = Gravity.CENTER
            tv.setTextColor(ResourcesCompat.getColor(resources, R.color.firstTextColor, context.theme))
            tv.text = "QAuxiliary 最终用户许可协议\n与《隐私条款》"
            addView(tv, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

            val sb = SpannableStringBuilder()
            try {
                sb.append(ResUtils.openAsset("eula.txt").readText())
                sb.append("\n\n")
                sb.append(ResUtils.openAsset("privacy_license.txt").readText())
            } catch (e: IOException) {
                sb.append(Log.getStackTraceString(e))
            }

            tv = TextView(context)
            tv.textSize = 16f
            tv.setTextColor(ResourcesCompat.getColor(resources, R.color.firstTextColor, context.theme))
            tv.text = sb
            tv.setTextIsSelectable(true)
            addView(tv, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

            tv = TextView(context)
            tv.textSize = 23f
            tv.gravity = Gravity.CENTER
            tv.setTextColor(ResourcesCompat.getColor(resources, R.color.firstTextColor, context.theme))
            tv.text = "\n注意: 本软件是免费软件!\nQAuxiliary自始至终都是免费且非商业使用，如果有你发现有人在违反AGPL和Eula，请拒绝并不遗余力地在一切平台举报投诉他！\n"
            addView(tv, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

            val _5dp: Int = dip2px(context, 5f)

            if (!LicenseStatus.hasUserAcceptEula()) {
                val iHaveRead = CheckBox(context)
                mCheckBoxHaveRead = iHaveRead
                iHaveRead.text = "我已阅读<<协议>>和<<隐私条款>>并自愿承担由使用本软件导致的一切后果"
                iHaveRead.textSize = 17f
                iHaveRead.setTextColor(ResourcesCompat.getColor(resources, R.color.firstTextColor, context.theme))
                iHaveRead.setPadding(_5dp, _5dp, _5dp, _5dp)
                iHaveRead.isChecked = FakeBatteryHook.INSTANCE.isFakeBatteryCharging
                addView(iHaveRead,
                        newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT, 3 * _5dp, _5dp, 2 * _5dp, _5dp))
                val agree = Button(context)
                agree.id = R.id.btn_allow
                agree.setOnClickListener(this@EulaFragment)
                agree.text = "我同意并继续"
                addView(agree, newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT, 2 * _5dp, _5dp, 2 * _5dp, _5dp))
                val deny = Button(context)
                deny.setId(R.id.btn_deny)
                deny.setOnClickListener(this@EulaFragment)
                deny.setText("我拒绝")
                addView(deny, newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT, 2 * _5dp, _5dp, 2 * _5dp, _5dp))
            } else {
                tv = TextView(context)
                tv.textSize = 17f
                tv.paint.isFakeBoldText = true
                tv.gravity = Gravity.CENTER
                tv.setTextColor(ResourcesCompat.getColor(resources, R.color.thirdTextColor, context.theme))
                tv.text = "你已阅读并同意<<协议>>和<<隐私条款>>"
                addView(tv, newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT, 2 * _5dp, _5dp, 2 * _5dp, _5dp))
            }
        }
        bounceScrollView.addView(ll)
        return bounceScrollView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mCheckBoxHaveRead = null
    }

    override fun onClick(v: View) {
        val iHaveRead: CheckBox = mCheckBoxHaveRead!!
        val read = iHaveRead.isChecked
        val context = v.context
        when (v.id) {
            R.id.btn_allow -> if (!read) {
                Toasts.error(context, "请先勾选\"我已阅读<<协议>>\"")
                return
            } else {
                LicenseStatus.setEulaStatus(CURRENT_EULA_VERSION)
                InjectDelayableHooks.doInitDelayableHooksMP()
                context.startActivity(Intent(context, SettingsUiFragmentHostActivity::class.java))
                settingsHostActivity!!.finish()
            }
            R.id.btn_deny -> {
                try {
                    val uri: Uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                    val intent = Intent(Intent.ACTION_DELETE, uri)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toasts.error(context, e.toString() + "", Toast.LENGTH_LONG)
                }
                Toasts.error(context, "请立即卸载 QAuxiliary", Toast.LENGTH_LONG)
            }
        }
    }

    @Throws(IOException::class)
    private fun InputStream.readText(): String {
        this.use {
            val sb = StringBuilder()
            val reader = BufferedReader(InputStreamReader(it, StandardCharsets.UTF_8))
            var line: String? = reader.readLine()
            while (line != null) {
                sb.append(line)
                sb.append('\n')
                line = reader.readLine()
            }
            return sb.toString()
        }
    }
}
