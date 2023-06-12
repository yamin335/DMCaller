/*
 * Copyright (c) 2010-2019 Belledonne Communications SARL.
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
package com.bdcom.appdialer.firebase

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ResolveInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.bdcom.appdialer.LinphoneApplication
import com.bdcom.appdialer.models.FCMTokenRegisterRequest
import com.bdcom.appdialer.utils.AppUtils
import com.bdcom.appdialer.utils.Constants
import com.bdcom.appdialer.utils.Constants.API_KEY
import com.bdcom.appdialer.utils.Constants.COMPANY_ID_KEY
import com.bdcom.appdialer.utils.Constants.FCM_KEY
import com.bdcom.appdialer.utils.Constants.LAT_KEY
import com.bdcom.appdialer.utils.Constants.LOGIN_KEY
import com.bdcom.appdialer.utils.Constants.LONG_KEY
import com.bdcom.appdialer.utils.Constants.MOBILE_NUMBER_KEY
import com.bdcom.appdialer.utils.Constants.PLATFORM
import com.bdcom.appdialer.utils.Constants.USER_ID_KEY
import com.bdcom.appdialer.utils.NotificationUtils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.linphone.core.tools.Log
import org.linphone.core.tools.service.AndroidDispatcher
import org.linphone.core.tools.service.CoreManager

class MyFirebaseMessagingService : FirebaseMessagingService(), LifecycleObserver {

    private var isAppInForeground = false

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onForegroundStart() {
        isAppInForeground = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onForegroundStop() {
        isAppInForeground = false
    }

    override fun onNewToken(token: String) {
        android.util.Log.i("FirebaseIdService", "[Push Notification] Refreshed token: $token")
        if (CoreManager.isReady()) {
            CoreManager.instance().setPushToken(token)
        }

        val preferences = LinphoneApplication.instance.getSharedPreferences()
        val preferenceEditor = preferences.edit()
        preferenceEditor.putString(FCM_KEY, token)
        preferenceEditor.apply()

        if (preferences.getBoolean(LOGIN_KEY, false)) {
            val fcmTokenRegisterRequest = FCMTokenRegisterRequest(preferences.getString(USER_ID_KEY, "")
                ?: "", API_KEY,
                preferences.getString(MOBILE_NUMBER_KEY, "")
                    ?: "", preferences.getString(COMPANY_ID_KEY, "") ?: "",
                preferences.getString(FCM_KEY, "")
                    ?: "", AppUtils.getDeviceUniqueID(this),
                preferences.getString(LAT_KEY, "") ?: "",
                preferences.getString(LONG_KEY, "") ?: "", PLATFORM)
            AppUtils.registerFirebaseToken(fcmTokenRegisterRequest)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        android.util.Log.i("FirebaseMessaging", "[Push Notification] Received")

        if (isAppInForeground && remoteMessage.notification?.clickAction == "kotha_dialer_account_activated") {
            try {
                val title = remoteMessage.notification?.title
                val body = remoteMessage.notification?.body
                val time = System.currentTimeMillis()

                NotificationUtils.showBigTextNotification(applicationContext, title, body, time, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val preferences = LinphoneApplication.instance.getSharedPreferences()
        val isLoggedIn = preferences.getBoolean(Constants.LOGIN_KEY, false)
        if (!isLoggedIn) {
            return
        }

        val pushRunnable = Runnable { this@MyFirebaseMessagingService.onPushReceived() }
        AndroidDispatcher.dispatchOnUIThread(pushRunnable)
    }

    private fun onPushReceived() {
        if (!CoreManager.isReady()) {
            notifyAppPushReceivedWithoutCoreAvailable()
        } else if (CoreManager.instance() != null) {
            val core = CoreManager.instance().core
            if (core != null) {
                Log.i(*arrayOf<Any>("[Push Notification] Notifying Core"))
                core.ensureRegistered()
            } else {
                Log.i(*arrayOf<Any>("[Push Notification] Notifying application"))
                notifyAppPushReceivedWithoutCoreAvailable()
            }
        }
    }

    private fun notifyAppPushReceivedWithoutCoreAvailable() {
        val intent = Intent()
        intent.action = "org.linphone.core.action.PUSH_RECEIVED"
        val pm = this.packageManager
        val matches = pm.queryBroadcastReceivers(intent, 0)
        val var4: Iterator<*> = matches.iterator()
        while (var4.hasNext()) {
            val resolveInfo = var4.next() as ResolveInfo
            val packageName = resolveInfo.activityInfo.applicationInfo.packageName
            if (packageName == packageName) {
                val explicit = Intent(intent)
                val cn = ComponentName(packageName, resolveInfo.activityInfo.name)
                explicit.component = cn
                this.sendBroadcast(explicit)
                break
            }
        }
    }

//    private val mPushReceivedRunnable = Runnable {
//        if (!LinphoneContext.isReady()) {
//            Log.i("FirebaseMsgService", "[Push Notification] Starting context")
//            LinphoneContext(applicationContext)
//            LinphoneContext.instance().start(true)
//        } else {
//            Log.i("[Push Notification]", "Notifying Core")
//            if (LinphoneManager.getInstance() != null) {
//                val core = LinphoneManager.getCore()
//                core?.ensureRegistered()
//            }
//        }
//    }

//    override fun onNewToken(token: String) {
//        Log.i("FirebaseIdService", "[Push Notification] Refreshed token: $token")
// //        LinphoneUtils.dispatchOnUIThread { LinphonePreferences.instance().setPushNotificationRegistrationID(token) }
// //
// //        val preferences = AppMain.instance.getSharedPreferences()
// //        val preferenceEditor = preferences.edit()
// //        preferenceEditor.putString(FCM_KEY, token)
// //        preferenceEditor.apply()
// //
// //        if (preferences.getBoolean(LOGIN_KEY, false)) {
// //            val fcmTokenRegisterRequest = FCMTokenRegisterRequest(preferences.getString(USER_ID_KEY, "")
// //                    ?: "", API_KEY,
// //                    preferences.getString(MOBILE_NUMBER_KEY, "")
// //                            ?: "", preferences.getString(COMPANY_ID_KEY, "") ?: "",
// //                    preferences.getString(FCM_KEY, "")
// //                            ?: "", AppUtils.getDeviceUniqueID(this),
// //                    preferences.getString(LAT_KEY, "") ?: "",
// //                    preferences.getString(LONG_KEY, "") ?: "", PLATFORM)
// //            AppUtils.registerFirebaseToken(fcmTokenRegisterRequest)
// //        }
//    }

//    override fun onMessageReceived(remoteMessage: RemoteMessage) {
//
//        val notificaton = remoteMessage.notification
// //
// //        if (data.isEmpty()) return
// //
// //        val jsonObject = JsonObject().apply {
// //            for (key in data.keys) {
// //                addProperty(key, data[key])
// //            }
// //        }
//
//        Log.i("FirebaseMsgService", "[Push Notification] Received")
// //        LinphoneUtils.dispatchOnUIThread(mPushReceivedRunnable)
// //
// //        if (isAppInForeground && remoteMessage.notification?.clickAction == "kotha_dialer_account_activated") {
// //            try {
// //                val title = remoteMessage.notification?.title
// //                val body = remoteMessage.notification?.body
// //                val time = System.currentTimeMillis()
// //
// //                NotificationUtils.showBigTextNotification(applicationContext, title, body, time, null)
// //            } catch (e: Exception) {
// //                e.printStackTrace()
// //            }
// //        }
//
// //        if (isAppInForeground) {
// //            //BottomTabsActivity.navigatedFromPushNotification = true
// //            // app is in foreground, broadcast the push message
// // //            val pushNotification = Intent(PUSH_NOTIFICATION_RECEIVER)
// // //            pushNotification.putExtra("notification", "Incoming")
// // //            LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification)
// //
// //            handleForegroundMessageData(jsonObject)
// //        } else {
// //            handleBackgroundMessageData(jsonObject)
// //        }
//    }

//    private fun handleForegroundMessageData(data: JsonObject) {
//        if (data.get(DATA_KEY_CALL_TYPE).asString == DATA_KEY_INCOMING) {
//            startIncomingCallNotificationService(data)
//        }
//    }
//
//    private fun handleBackgroundMessageData(data: JsonObject) {
//        if (data.get(DATA_KEY_CALL_TYPE).asString == DATA_KEY_INCOMING) {
//            startIncomingCallNotificationService(data)
//        }
//    }

//    private fun startIncomingCallNotificationService(data: JsonObject) {
//        val intentService = Intent(this, IncomingCallNotificationService::class.java)
//        intentService.putExtra(DATA_KEY_MOBILE, data.get(DATA_KEY_MOBILE).asString)
//        intentService.putExtra(DATA_KEY_EXT, data.get(DATA_KEY_EXT).asString)
//        intentService.putExtra(DATA_KEY_ID, data.get(DATA_KEY_ID).asString)
//        intentService.putExtra(DATA_KEY_DATE, data.get(DATA_KEY_DATE).asString)
//        intentService.putExtra(DATA_KEY_CALL_TYPE, data.get(DATA_KEY_CALL_TYPE).asString)
//        intentService.putExtra(DATA_KEY_SRC, data.get(DATA_KEY_SRC).asString)
//        intentService.putExtra(DATA_KEY_DEVICE_TYPE, data.get(DATA_KEY_DEVICE_TYPE).asString)
//        intentService.action = START_FOREGROUND_ACTION
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(intentService)
//        } else {
//            startService(intentService)
//        }
//    }
}
