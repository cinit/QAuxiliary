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

package io.github.qauxv.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.appbar.AppBarLayout
import io.github.qauxv.R
import io.github.qauxv.SyncUtils
import io.github.qauxv.fragment.BaseSettingFragment
import io.github.qauxv.fragment.SettingsMainFragment
import io.github.qauxv.ui.ModuleThemeManager
import io.github.qauxv.ui.ResUtils
import name.mikanoshi.customiuizer.holidays.HolidayHelper

class SettingsUiFragmentHostActivity : AppCompatTransferActivity() {

    private val mFragmentStack = ArrayList<BaseSettingFragment>(4)
    private var mTopVisibleFragment: BaseSettingFragment? = null
    private lateinit var mAppBarLayout: AppBarLayout
    private lateinit var mAppToolBar: androidx.appcompat.widget.Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        // sync theme with host
        AppCompatDelegate.setDefaultNightMode(if (ResUtils.isInNightMode())
            AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        setTheme(ModuleThemeManager.getCurrentStyleId())
        // we don't want the Fragment to be recreated
        super.onCreate(null)
        setContentView(R.layout.activity_settings_ui_host)
        mAppBarLayout = findViewById(R.id.topAppBarLayout)
        mAppToolBar = findViewById(R.id.topAppBar)
        setSupportActionBar(mAppToolBar)
        requestTranslucentStatusBar()
        HolidayHelper.setup(this)
        val intent = intent
        // check if we are requested to show a specific fragment
        val fragmentName: String? = intent.getStringExtra(TARGET_FRAGMENT_KEY)
        val startupFragment: BaseSettingFragment = if (fragmentName != null) {
            val clazz = Class.forName(fragmentName)
            val fragment = clazz.newInstance() as BaseSettingFragment
            val args: Bundle? = intent.getBundleExtra(TARGET_FRAGMENT_ARGS_KEY)
            if (args != null) {
                fragment.arguments = args
            }
            fragment
        } else {
            // otherwise, show the default fragment
            SettingsMainFragment.newInstance(arrayOf())
        }
        // add the fragment to the stack
        presentFragment(startupFragment)
    }

    fun presentFragment(fragment: BaseSettingFragment) {
        rtlAddFragmentToTop(fragment)
    }

    fun finishFragment(fragment: BaseSettingFragment) {
        rtlRemoveFragment(fragment)
    }

    fun popCurrentFragment() {
        val fragment = mFragmentStack.lastOrNull()
        if (fragment != null) {
            rtlRemoveFragment(fragment)
        } else {
            finish()
        }
    }

    override fun onBackPressed() {
        val consumed = mTopVisibleFragment?.doOnBackPressed() ?: false
        if (!consumed) {
            popCurrentFragment()
        }
    }

    private fun updateTitle(fragment: BaseSettingFragment) {
        SyncUtils.postDelayed(1) {
            val text: String? = fragment.title
            this.title = text
            supportActionBar?.let {
                it.title = text
            }
        }
    }

    private fun rtlAddFragmentToTop(fragment: BaseSettingFragment) {
        if (mFragmentStack.isEmpty()) {
            // first fragment
            supportFragmentManager.beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit()
            mTopVisibleFragment = fragment
            mFragmentStack.add(fragment)
            updateTitle(fragment)
        } else {
            // replace the top fragment
            supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                    .hide(mTopVisibleFragment!!)
                    .add(R.id.fragment_container, fragment)
                    .commit()
            mTopVisibleFragment = fragment
            mFragmentStack.add(fragment)
            updateTitle(fragment)
        }
    }

    private fun rtlRemoveFragment(fragment: BaseSettingFragment) {
        // check if we need to show the previous fragment
        if (fragment == mTopVisibleFragment) {
            // this is the visible fragment, so we need to show the previous one
            val transaction = supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_left)
                    .hide(fragment)
            mFragmentStack.remove(fragment)
            mTopVisibleFragment = mFragmentStack.lastOrNull()
            if (mTopVisibleFragment == null) {
                finish()
            } else {
                transaction.show(mTopVisibleFragment!!).commit()
                updateTitle(mTopVisibleFragment!!)
                SyncUtils.postDelayed(300) {
                    // wait 300ms before remove the fragment to allow the animation to play
                    // I don't know why, but it works.
                    supportFragmentManager.beginTransaction().remove(fragment).commit()
                }
            }
        } else {
            // background fragment, just remove it
            supportFragmentManager.beginTransaction()
                    .remove(fragment)
                    .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        HolidayHelper.onResume()
    }

    override fun onPause() {
        HolidayHelper.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        HolidayHelper.onDestroy()
    }

    companion object {
        const val TARGET_FRAGMENT_KEY: String = "SettingsUiFragmentHostActivity.TARGET_FRAGMENT_KEY"
        const val TARGET_FRAGMENT_ARGS_KEY: String = "SettingsUiFragmentHostActivity.TARGET_FRAGMENT_ARGS_KEY"

        @JvmStatic
        fun startFragmentWithContext(context: Context,
                                     fragmentClass: Class<out BaseSettingFragment>,
                                     args: Bundle? = null) {
            // check if we need to start a new activity
            if (context is SettingsUiFragmentHostActivity) {
                // just add the fragment to the top
                context.presentFragment(fragmentClass.newInstance().apply { arguments = args })
            } else {
                // start a new activity for the fragment
                startActivityForFragment(context, fragmentClass, args)
            }
        }

        @JvmStatic
        fun startActivityForFragment(context: Context,
                                     fragmentClass: Class<out BaseSettingFragment>,
                                     args: Bundle? = null) {
            context.startActivity(createStartActivityForFragmentIntent(context, fragmentClass, args))
        }

        @JvmStatic
        fun createStartActivityForFragmentIntent(context: Context,
                                                 fragmentClass: Class<out BaseSettingFragment>,
                                                 args: Bundle? = null): Intent {
            val intent = Intent(context, SettingsUiFragmentHostActivity::class.java)
            intent.putExtra(TARGET_FRAGMENT_KEY, fragmentClass.name)
            intent.putExtra(TARGET_FRAGMENT_ARGS_KEY, args)
            return intent
        }
    }
}
