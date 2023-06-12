package com.bdcom.appdialer.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import java.io.IOException
import java.util.*

object Constants {
    const val BASE_URL = "http://cdr.kotha.com.bd/iptkotha/apps/api/"
    const val API_ROOT_URL = "http://cdr.kotha.com.bd"
    const val API_KEY = "4f2f7dbed722e5f1a26e4d3488a36584"
    const val Server_IP = "119.40.81.104"

    // ?userid=rtchubs&apikey=4f2f7dbed722e5f1a26e4d3488a36584&mobile_no=01710441906&name=Mamun&company=1000&password=rtchubs&email=mamun&imei_number=12345&latitude=23.9&longitude=24.9&platform=ios
    // key
    const val CLIENT_NAME_KEY = "clientName"
    const val FROM_REGISTRATION_KEY = "from_registration"
    const val OTP = "OTP"
    const val SECRET = "secret"
    const val EXPIRATION_DATE = "expiration"
    const val MSISDN = "MSISDN"
    const val PUSH_NOTIFICATION_RECEIVER = "push_notification_broadcast_receiver"
    const val START_FOREGROUND_ACTION = "start_foreground_service"
    const val STOP_FOREGROUND_ACTION = "stop_foreground_service"
    const val OUTGOING_CALL_TYPE = 1
    const val INCOMING_CALL_TYPE = 2
    const val MISSED_CALL_TYPE = 3
    const val USER_ID = "rtchubs"

    const val RING_TONE_PATH = "selected_ringtone_path"

    // Incoming Call Notification Data Keys
    const val DATA_KEY_NOTIFICATION_ID = "notification_id"
    const val DATA_KEY_INCOMING = "incoming"
    const val DATA_KEY_INCOMING_CALL = "action_call_decline"
    const val DATA_KEY_MOBILE = "mobile"
    const val DATA_KEY_EXT = "extension"
    const val DATA_KEY_ID = "uniqueid"
    const val DATA_KEY_DATE = "calldate"
    const val DATA_KEY_CALL_TYPE = "calltype"
    const val DATA_KEY_SRC = "src"
    const val DATA_KEY_DEVICE_TYPE = "deviceType"
    const val CALL_RESPONSE_ACTION_KEY = "action_call_response"
    const val CALL_RECEIVE_ACTION = "action_call_receive"
    const val CALL_decline_ACTION = "action_call_decline"
    const val TEXT_INCOMING_CALL = "Incoming Call"

    // User Data Keys
    const val FCM_KEY = "Firebase_Token_Key"
    const val PLATFORM = "android"
    const val LAT_KEY = "Last_Location_Latitude"
    const val LONG_KEY = "Last_Location_Longitude"
    const val MOBILE_NUMBER_KEY = "mobileNumber"
    const val USER_ID_KEY = "User_ID"
    const val USER_NAME_KEY = "username"
    const val IMEI = "imei"
    const val PASSWORD_KEY = "password"
    const val SERVER_IP_KEY = "serverIP"
    const val DISPLAY_NAME_KEY = "displayName"
    const val COMPANY_ID_KEY = "Company_Unique_ID"
    const val LOGIN_KEY = "loggedIn"
    const val SERVICE_STATUS_KEY = "isServiceStarted"
    const val APP_EXIT_KEY = "isAppExit"
    const val COMPANY_SECRET_KEY = "Company_Secret_Number"
    const val APP_BACKGROUND_KEY = "isAppInBackground"
    fun getCountry(lat: Double, lon: Double, context: Context?): String? {
        val geocoder: Geocoder
        var addresses: List<Address> = ArrayList()
        geocoder = Geocoder(context, Locale.getDefault())
        try {
            addresses = geocoder.getFromLocation(
                lat, lon,
                1
            ) // Here 1 represent max location result to returned, by documents it
            // recommended 1 to 5
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("Country:", e.localizedMessage)
        }
        return if (!addresses.isEmpty()) {
            val address = addresses[0].getAddressLine(0)
            Log.e("Country:", address)
            address
        } else {
            Log.e("Country:", "nai")
            null
        }
    }

    interface ACTION {
        companion object {
            const val MAIN_ACTION = "bdcom.truiton.foregroundservice.action.main"
            const val PREV_ACTION = "bdcom.truiton.foregroundservice.action.prev"
            const val PLAY_ACTION = "bdcom.truiton.foregroundservice.action.play"
            const val NEXT_ACTION = "bdcom.truiton.foregroundservice.action.next"
            const val STARTFOREGROUND_ACTION =
                "bdcom.truiton.foregroundservice.action.startforeground"
            const val STOPFOREGROUND_ACTION =
                "bdcom.truiton.foregroundservice.action.stopforeground"
        }
    }

    interface NOTIFICATION_ID {
        companion object {
            const val FOREGROUND_SERVICE = 101
        }
    }
}
