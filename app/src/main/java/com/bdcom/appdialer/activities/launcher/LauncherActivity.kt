/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.bdcom.appdialer.activities.launcher

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bdcom.appdialer.LinphoneApplication
import com.bdcom.appdialer.LinphoneApplication.Companion.corePreferences
import com.bdcom.appdialer.R
import com.bdcom.appdialer.activities.BottomTabsActivity
import com.bdcom.appdialer.activities.LoginActivity
import com.bdcom.appdialer.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.linphone.core.tools.Log

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.launcher_activity)
    }

    override fun onStart() {
        super.onStart()
        // coreContext.handler.postDelayed({ onReady() }, 500)
        CoroutineScope(Dispatchers.Main.immediate).launch {
            delay(500)
            onReady()
        }
    }

    private fun onReady() {
        Log.i("[Launcher] Core is ready")

//        if (corePreferences.preventInterfaceFromShowingUp) {
//            Log.w("[Context] We were asked to not show the user interface")
//            finish()
//            return
//        }

        val preferences = LinphoneApplication.instance.getSharedPreferences()
        val isLoggedIn = preferences.getBoolean(Constants.LOGIN_KEY, false)
        if (!isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            val intent = Intent()
            intent.setClass(this, BottomTabsActivity::class.java)

            // Propagate current intent action, type and data
            if (getIntent() != null) {
                val extras = getIntent().extras
                if (extras != null) intent.putExtras(extras)
            }
            intent.action = getIntent().action
            intent.type = getIntent().type
            intent.data = getIntent().data

            startActivity(intent)
            if (corePreferences.enableAnimations) {
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
    }
}
