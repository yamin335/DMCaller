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
package com.bdcom.appdialer

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.bdcom.appdialer.core.CoreContext
import com.bdcom.appdialer.core.CorePreferences
import com.bdcom.appdialer.utils.Constants
import com.bdcom.appdialer.utils.DatabaseHelper
import com.bdcom.appdialer.utils.NotificationUtils
import com.google.firebase.FirebaseApp
import org.linphone.core.Factory
import org.linphone.core.LogCollectionState
import org.linphone.core.tools.Log

class LinphoneApplication : Application() {
    companion object {

        lateinit var instance: LinphoneApplication

        var SERVER_IP = ""

        fun getApplicationContext(): Context {
            return instance.applicationContext
        }

        lateinit var corePreferences: CorePreferences
        lateinit var coreContext: CoreContext

        fun ensureCoreExists(context: Context, pushReceived: Boolean = false) {
            if (Companion::coreContext.isInitialized && !coreContext.stopped) {
                Log.d("[Application] Skipping Core creation (push received? $pushReceived)")
                return
            }

            Factory.instance().setLogCollectionPath(context.filesDir.absolutePath)
            Factory.instance().enableLogCollection(LogCollectionState.Enabled)

            corePreferences = CorePreferences(context)
            corePreferences.copyAssetsFromPackage()

            if (corePreferences.vfsEnabled) {
                CoreContext.activateVFS()
            }

            val config = Factory.instance().createConfigWithFactory(corePreferences.configPath, corePreferences.factoryConfigPath)
            corePreferences.config = config

            val appName = context.getString(R.string.app_name)
            Factory.instance().setDebugMode(corePreferences.debugLogs, appName)

            Log.i("[Application] Core context created ${if (pushReceived) "from push" else ""}")

            // coreContext.start()
            val preferences = instance.getSharedPreferences("KothaDialer", Context.MODE_PRIVATE)
            val isLoggedIn = preferences.getBoolean(Constants.LOGIN_KEY, false)
            if (isLoggedIn) {
                coreContext = CoreContext(context, config)
                coreContext.start()
                Log.i("[Application] Created")
            }
        }
    }

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate() {
        super.onCreate()
        val appName = getString(R.string.app_name)
        // android.util.Log.i("[$appName]", "Application is being created")

        // Log.i("[Application] Created")

        instance = this
        sharedPreferences = getSharedPreferences("KothaDialer", Context.MODE_PRIVATE)
        databaseHelper = DatabaseHelper(applicationContext)
        ensureCoreExists(applicationContext)
        prepareNotificationChannel(this)
        FirebaseApp.initializeApp(this@LinphoneApplication)
    }

    fun getSharedPreferences(): SharedPreferences {
        return sharedPreferences
    }

    fun getDatabaseHelper(): DatabaseHelper {
        android.util.Log.d("Kotha Dialer", "AppMain getDatabaseHelper")
        return databaseHelper
    }

    private fun prepareNotificationChannel(context: Context) {
        // User invisible channel ID
        val mChannelId = context.getString(R.string.firebase_push_notification_channel_id)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.prepareChannel(context, mChannelId)
        }
    }
}
