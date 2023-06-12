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
package com.bdcom.appdialer.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.os.Build
import android.telephony.TelephonyManager
import android.text.format.Formatter.formatShortFileSize
import android.util.TypedValue
import androidx.emoji.text.EmojiCompat
import com.bdcom.appdialer.LinphoneApplication
import com.bdcom.appdialer.LinphoneApplication.Companion.coreContext
import com.bdcom.appdialer.R
import com.bdcom.appdialer.models.FCMTokenRegisterRequest
import com.bdcom.appdialer.models.FCMTokenRegistrationResponse
import com.bdcom.appdialer.network_utils.ApiHelper
import java.util.*
import org.linphone.core.tools.Log

/**
 * Various utility methods for application
 */
class AppUtils {
    companion object {

        var isConference = false

        // Kotha Dialer Start

        fun directCall(to: String) {
            coreContext.startCall(to)
        }

        @SuppressLint("MissingPermission", "HardwareIds")
        fun getDeviceUniqueID(context: Context): String {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val preferences = LinphoneApplication.instance.getSharedPreferences()
            var tempId = preferences.getString(Constants.IMEI, "") ?: ""
            return if (tempId.isNotEmpty()) {
                tempId
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                tempId = telephonyManager.imei
                val preferenceEditor = preferences.edit()
                preferenceEditor.putString(Constants.IMEI, tempId)
                preferenceEditor.apply()
                tempId
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                tempId = telephonyManager.simSerialNumber ?: UUID.randomUUID().toString()
                val preferenceEditor = preferences.edit()
                preferenceEditor.putString(Constants.IMEI, tempId)
                preferenceEditor.apply()
                tempId
            } else {
                tempId = UUID.randomUUID().toString()
                val preferenceEditor = preferences.edit()
                preferenceEditor.putString(Constants.IMEI, tempId)
                preferenceEditor.apply()
                tempId
            }
        }

        fun registerFirebaseToken(request: FCMTokenRegisterRequest) {
            ApiHelper.registerFirebaseToken(request, object : ApiHelper.ApiCallbackListener<FCMTokenRegistrationResponse> {
                override fun onFailed(status: Int?) {
                }

                override fun onDataFetched(response: FCMTokenRegistrationResponse?) {
                    android.util.Log.d("Firebase:", response?.messages ?: "No Response Found")
                }
            })
        }

//        fun getMobileNumber(value: String?): String {
//            var number = value ?: "Unknown Number"
//            var temp = number.split(":")
//            var tempNumber = if (temp.size > 1) temp[1] else number
//            temp = tempNumber.split("@")
//            tempNumber = if (temp.isNotEmpty()) temp[0] else number
//            number = tempNumber
//
//            return number
//        }

        fun getMobileNumber(value: String?): String {
            var number = value ?: "Unknown Number"
            var temp = number.split(":")
            var tempNumber = if (temp.size > 1) temp[1] else number
            temp = tempNumber.split("@")
            tempNumber = if (temp.isNotEmpty()) temp[0] else number
            number = tempNumber

            if (number.contains("+88")) {
                number = number.removePrefix("+88")
            }

            return number
        }

//        fun getContactName(number: String): String {
//            var name = number
//            return if (AndroidUtil.getContactName(number).isEmpty()) {
//                name
//            } else {
//                name = AndroidUtil.getContactName(number)
//                name
//            }
//        }

        fun getContactName(number: String): String {
            var name = number
            return if (AndroidUtil.getContactName(number).isEmpty()) {
                if (number.contains("+88")) {
                    name = number.removePrefix("+88")
                    name
                } else {
                    name
                }
            } else {
                name = AndroidUtil.getContactName(number)
                name
            }
        }

        // Kotha Dialer End

        fun getString(id: Int): String {
            return coreContext.context.getString(id)
        }

        fun getStringWithPlural(id: Int, count: Int): String {
            return coreContext.context.resources.getQuantityString(id, count, count)
        }

        fun getStringWithPlural(id: Int, count: Int, value: String): String {
            return coreContext.context.resources.getQuantityString(id, count, value)
        }

        fun getDimension(id: Int): Float {
            return coreContext.context.resources.getDimension(id)
        }

        fun getInitials(displayName: String, limit: Int = 2): String {
            if (displayName.isEmpty()) return ""

            val emoji = EmojiCompat.get()
            val split = displayName.toUpperCase(Locale.getDefault()).split(" ")
            var initials = ""
            var characters = 0

            for (i in split.indices) {
                if (split[i].isNotEmpty()) {
                    if (emoji.hasEmojiGlyph(split[i])) {
                        initials += emoji.process(split[i])
                    } else {
                        initials += split[i][0]
                    }
                    characters += 1
                    if (characters >= limit) break
                }
            }
            return initials
        }

        fun pixelsToDp(pixels: Float): Float {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                pixels,
                coreContext.context.resources.displayMetrics
            )
        }

        fun bytesToDisplayableSize(bytes: Long): String {
            return formatShortFileSize(coreContext.context, bytes)
        }

        fun shareUploadedLogsUrl(activity: Activity, info: String) {
            val appName = activity.getString(R.string.app_name)
            val intent = Intent(Intent.ACTION_SEND)
            intent.putExtra(
                Intent.EXTRA_EMAIL,
                arrayOf(activity.getString(R.string.about_bugreport_email))
            )
            intent.putExtra(Intent.EXTRA_SUBJECT, "$appName Logs")
            intent.putExtra(Intent.EXTRA_TEXT, info)
            intent.type = "text/plain"

            try {
                activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.share_uploaded_logs_link)))
            } catch (ex: ActivityNotFoundException) {
                Log.e(ex)
            }
        }
    }
}
