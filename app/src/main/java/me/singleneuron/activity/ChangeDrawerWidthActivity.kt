/*
 * QNotified - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 dmca@ioctl.cc
 * https://github.com/ferredoxin/QNotified
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/ferredoxin/QNotified/blob/master/LICENSE.md>.
 */
package me.singleneuron.activity

import android.os.Bundle
import io.github.qauxv.R
import io.github.qauxv.activity.AppCompatTransferActivity
import io.github.qauxv.databinding.ActivityChangeDrawerWidthBinding
import io.github.qauxv.ui.WindowIsTranslucent
import me.singleneuron.hook.ChangeDrawerWidth

class ChangeDrawerWidthActivity : AppCompatTransferActivity(), WindowIsTranslucent {

    private lateinit var binding: ActivityChangeDrawerWidthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.MaterialDialogActivity)
        super.onCreate(savedInstanceState)
        binding = ActivityChangeDrawerWidthBinding.inflate(layoutInflater)
        val slider = binding.slider
        slider.valueFrom = 0f
        slider.valueTo = ChangeDrawerWidth.getMaxWidth(this).toInt().toFloat()
        slider.stepSize = 1f
        binding.textView6.text = ChangeDrawerWidth.width.toString()
        slider.value = ChangeDrawerWidth.width.toFloat()
        slider.addOnChangeListener { _, value, _ ->
            binding.textView6.text = value.toInt().toString()
        }
        binding.button2.setOnClickListener {
            ChangeDrawerWidth.width = slider.value.toInt()
            finish()
        }
        setContentView(binding.root)
    }
}
