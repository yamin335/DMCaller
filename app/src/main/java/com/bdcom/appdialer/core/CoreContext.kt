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
package com.bdcom.appdialer.core

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Base64
import android.util.Pair
import android.view.*
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.lifecycle.MutableLiveData
import com.bdcom.appdialer.LinphoneApplication
import com.bdcom.appdialer.LinphoneApplication.Companion.corePreferences
import com.bdcom.appdialer.R
import com.bdcom.appdialer.activities.call.CallActivity
import com.bdcom.appdialer.activities.call.IncomingCallActivity
import com.bdcom.appdialer.activities.call.OutgoingCallActivity
import com.bdcom.appdialer.compatibility.Compatibility
import com.bdcom.appdialer.contact.Contact
import com.bdcom.appdialer.contact.ContactsManager
import com.bdcom.appdialer.notifications.NotificationsManager
import com.bdcom.appdialer.utils.*
import com.bdcom.appdialer.utils.Constants.COMPANY_SECRET_KEY
import com.bdcom.appdialer.utils.Constants.IMEI
import com.bdcom.appdialer.utils.Constants.LAT_KEY
import com.bdcom.appdialer.utils.Constants.LONG_KEY
import com.bdcom.appdialer.utils.Event
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.File
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.math.abs
import kotlinx.coroutines.*
import org.linphone.core.*
import org.linphone.core.tools.Log
import org.linphone.mediastream.Version

class CoreContext(val context: Context, coreConfig: Config) {
    var stopped = false
    val core: Core
    val handler: Handler = Handler(Looper.getMainLooper())

    var screenWidth: Float = 0f
    var screenHeight: Float = 0f

    val appVersion: String by lazy {
        val appVersion = BuildConfig.VERSION_NAME
        val appBranch = context.getString(R.string.linphone_app_branch)
        val appBuildType = BuildConfig.BUILD_TYPE
        "$appVersion ($appBranch, $appBuildType)"
    }

    val sdkVersion: String by lazy {
        val sdkVersion = context.getString(R.string.linphone_sdk_version)
        val sdkBranch = context.getString(R.string.linphone_sdk_branch)
        val sdkBuildType = BuildConfig.BUILD_TYPE
        "$sdkVersion ($sdkBranch, $sdkBuildType)"
    }

    val contactsManager: ContactsManager by lazy {
        ContactsManager(context)
    }
    val notificationsManager: NotificationsManager by lazy {
        NotificationsManager(context)
    }

    val callErrorMessageResourceId: MutableLiveData<Event<Int>> by lazy {
        MutableLiveData<Event<Int>>()
    }

    private val loggingService = Factory.instance().loggingService

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var gsmCallActive = false
    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            gsmCallActive = when (state) {
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    Log.i("[Context] Phone state is off hook")
                    true
                }
                TelephonyManager.CALL_STATE_RINGING -> {
                    Log.i("[Context] Phone state is ringing")
                    true
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    Log.i("[Context] Phone state is idle")
                    false
                }
                else -> {
                    Log.w("[Context] Phone state is unexpected: $state")
                    false
                }
            }
        }
    }

    private var overlayX = 0f
    private var overlayY = 0f
    private var callOverlay: View? = null
    private var previousCallState = Call.State.Idle
    private var isIncoming = false
    private var isCallConnected = false

    private val listener: CoreListenerStub = object : CoreListenerStub() {
        override fun onGlobalStateChanged(core: Core, state: GlobalState, message: String) {
            Log.i("[Context] Global state changed [$state]")
            if (state == GlobalState.On) {
                contactsManager.fetchContactsAsync()
            }
        }

        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState?,
            message: String
        ) {
            Log.i("[Context] Account [${account.params.identityAddress?.asStringUriOnly()}] registration state changed [$state]")
            if (state == RegistrationState.Ok && account == core.defaultAccount) {
                notificationsManager.stopForegroundNotificationIfPossible()
            }
        }

        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State,
            message: String
        ) {
            Log.i("[Context] Call state changed [$state]")
            if (state == Call.State.IncomingReceived || state == Call.State.IncomingEarlyMedia) {
                isIncoming = true

                if (gsmCallActive) {
                    Log.w("[Context] Refusing the call with reason busy because a GSM call is active")
                    call.decline(Reason.Busy)
                    return
                }

                // Starting SDK 24 (Android 7.0) we rely on the fullscreen intent of the call incoming notification
                if (Version.sdkStrictlyBelow(Version.API24_NOUGAT_70)) {
                    onIncomingReceived()
                }

                if (corePreferences.autoAnswerEnabled) {
                    val autoAnswerDelay = corePreferences.autoAnswerDelay
                    if (autoAnswerDelay == 0) {
                        Log.w("[Context] Auto answering call immediately")
                        answerCall(call)
                    } else {
                        Log.i("[Context] Scheduling auto answering in $autoAnswerDelay milliseconds")
                        val mainThreadHandler = Handler(Looper.getMainLooper())
                        mainThreadHandler.postDelayed({
                            Log.w("[Context] Auto answering call")
                            answerCall(call)
                        }, autoAnswerDelay.toLong())
                    }
                }
            } else if (state == Call.State.OutgoingInit) {
                onOutgoingStarted()
            } else if (state == Call.State.OutgoingProgress) {
                if (core.callsNb == 1 && corePreferences.routeAudioToBluetoothIfAvailable) {
                    AudioRouteUtils.routeAudioToBluetooth(call)
                }
            } else if (state == Call.State.Connected) {
                isCallConnected = true
                onCallStarted()
                if (isIncoming) {
                    val address = LinphoneUtils.getDisplayableAddress(call.remoteAddress)

                    LinphoneApplication.instance.getDatabaseHelper().insertCallHistory(AppUtils.getMobileNumber(address), "",
                        Constants.INCOMING_CALL_TYPE
                    )
                }
            } else if (state == Call.State.StreamsRunning) {
                // Do not automatically route audio to bluetooth after first call
                if (core.callsNb == 1) {
                    // Only try to route bluetooth / headphone / headset when the call is in StreamsRunning for the first time
                    if (previousCallState == Call.State.Connected) {
                        Log.i("[Context] First call going into StreamsRunning state for the first time, trying to route audio to headset or bluetooth if available")
                        if (AudioRouteUtils.isHeadsetAudioRouteAvailable()) {
                            AudioRouteUtils.routeAudioToHeadset(call)
                        } else if (corePreferences.routeAudioToBluetoothIfAvailable && AudioRouteUtils.isBluetoothAudioRouteAvailable()) {
                            AudioRouteUtils.routeAudioToBluetooth(call)
                        }
                    }
                }

                if (corePreferences.routeAudioToSpeakerWhenVideoIsEnabled && call.currentParams.videoEnabled()) {
                    // Do not turn speaker on when video is enabled if headset or bluetooth is used
                    if (!AudioRouteUtils.isHeadsetAudioRouteAvailable() && !AudioRouteUtils.isBluetoothAudioRouteCurrentlyUsed(
                            call
                        )) {
                        Log.i("[Context] Video enabled and no wired headset not bluetooth in use, routing audio to speaker")
                        AudioRouteUtils.routeAudioToSpeaker(call)
                    }
                }
            } else if (state == Call.State.End || state == Call.State.Error || state == Call.State.Released) {
                if (state == Call.State.End) {
                    val address = LinphoneUtils.getDisplayableAddress(call.remoteAddress)
                    if (!isCallConnected && isIncoming) {
                        LinphoneApplication.instance.getDatabaseHelper().insertCallHistory(AppUtils.getMobileNumber(address), "",
                            Constants.MISSED_CALL_TYPE)
                    } else {
                        LinphoneApplication.instance.getDatabaseHelper().updateLastCall(
                            AppUtils.getMobileNumber(address),
                            call.duration.toString(),
                            if (isIncoming) Constants.INCOMING_CALL_TYPE else Constants.OUTGOING_CALL_TYPE
                        )
                    }
                    isCallConnected = false
                    isIncoming = false
                }

                if (core.callsNb == 0) {
                    removeCallOverlay()
                }

                if (state == Call.State.Error) {
                    Log.w("[Context] Call error reason is ${call.errorInfo.reason}")
                    val id = when (call.errorInfo.reason) {
                        Reason.Busy -> R.string.call_error_user_busy
                        Reason.IOError -> R.string.call_error_io_error
                        Reason.NotAcceptable -> R.string.call_error_incompatible_media_params
                        Reason.NotFound -> R.string.call_error_user_not_found
                        else -> R.string.call_error_unknown
                    }
                    callErrorMessageResourceId.value = Event(id)
                } else if (state == Call.State.End &&
                        call.dir == Call.Dir.Outgoing &&
                        call.errorInfo.reason == Reason.Declined
                ) {
                    Log.i("[Context] Call has been declined")
                    val id = R.string.call_error_declined
                    callErrorMessageResourceId.value = Event(id)
                }
            }

            previousCallState = state
        }

        override fun onMessageReceived(core: Core, chatRoom: ChatRoom, message: ChatMessage) {
            if (core.maxSizeForAutoDownloadIncomingFiles != -1) {
                var hasFile = false
                for (content in message.contents) {
                    if (content.isFile) {
                        hasFile = true
                        break
                    }
                }
                if (hasFile) {
                    exportFilesInMessageToMediaStore(message)
                }
            }
        }
    }

    private val loggingServiceListener = object : LoggingServiceListenerStub() {
        override fun onLogMessageWritten(
            logService: LoggingService,
            domain: String,
            level: LogLevel,
            message: String
        ) {
            when (level) {
                LogLevel.Error -> android.util.Log.e(domain, message)
                LogLevel.Warning -> android.util.Log.w(domain, message)
                LogLevel.Message -> android.util.Log.i(domain, message)
                LogLevel.Fatal -> android.util.Log.wtf(domain, message)
                else -> android.util.Log.d(domain, message)
            }
            FirebaseCrashlytics.getInstance().log("[$domain] [${level.name}] $message")
        }
    }

    init {
        if (context.resources.getBoolean(R.bool.crashlytics_enabled)) {
            loggingService.addListener(loggingServiceListener)
            Log.i("[Context] Crashlytics enabled, register logging service listener")
        }

        core = Factory.instance().createCoreWithConfig(coreConfig, context)

        stopped = false
        Log.i("[Context] Ready")
    }

    fun start(isPush: Boolean = false) {
        Log.i("[Context] Starting")
//        val transports = core.transports
//        transports.udpPort = 15605
//        transports.tcpPort = 15605
//        transports.tlsPort = -1
//        core.transports = transports
        notificationsManager.onCoreReady()

        core.addListener(listener)

        if (isPush) {
            Log.i("[Context] Push received, assume in background")
            core.enterBackground()
        }

        core.start()

        configureCore()

        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        Log.i("[Context] Registering phone state listener")
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)

        EmojiCompat.init(BundledEmojiCompatConfig(context))
    }

    fun stop() {
        Log.i("[Context] Stopping")
        coroutineScope.cancel()

        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        Log.i("[Context] Unregistering phone state listener")
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)

        notificationsManager.destroy()
        contactsManager.destroy()

        core.stop()
        core.removeListener(listener)
        stopped = true
        loggingService.removeListener(loggingServiceListener)
    }

    private fun configureCore() {
        Log.i("[Context] Configuring Core")

        core.staticPicture = corePreferences.staticPicturePath

        // Migration code
        if (core.config.getBool("app", "incoming_call_vibration", true)) {
            // core.isVibrationOnIncomingCallEnabled = true
            core.config.setBool("app", "incoming_call_vibration", false)
        }

        initUserCertificates()

        computeUserAgent()

        for (account in core.accountList) {
            if (account.params.identityAddress?.domain == corePreferences.defaultDomain) {
                // Ensure conference URI is set on sip.linphone.org proxy configs
                if (account.params.conferenceFactoryUri == null) {
                    val params = account.params.clone()
                    val uri = corePreferences.conferenceServerUri
                    Log.i("[Context] Setting conference factory on proxy config ${params.identityAddress?.asString()} to default value: $uri")
                    params.conferenceFactoryUri = uri
                    account.params = params
                }

                // Ensure LIME server URL is set if at least one sip.linphone.org proxy
                if (core.limeX3DhAvailable()) {
                    var url: String? = core.limeX3DhServerUrl
                    if (url == null || url.isEmpty()) {
                        url = corePreferences.limeX3dhServerUrl
                        Log.i("[Context] Setting LIME X3Dh server url to default value: $url")
                        core.limeX3DhServerUrl = url
                    }
                }
            }
        }

        Log.i("[Context] Core configured")
    }

    private fun computeUserAgent() {
        val deviceName: String = corePreferences.deviceName
        val appName: String = context.resources.getString(R.string.app_name)
        val androidVersion = BuildConfig.VERSION_NAME
        val preferences: SharedPreferences = context.getSharedPreferences("KothaDialer", Context.MODE_PRIVATE)
        val companySecret = preferences.getString(COMPANY_SECRET_KEY, "")
        val location_lat = preferences.getString(LAT_KEY, "")
        val location_long = preferences.getString(LONG_KEY, "")
        val userAgent = "$appName/$androidVersion ($deviceName), Company_key: $companySecret, Lat: $location_lat, Long: $location_long"
        val sdkVersion = context.getString(R.string.linphone_sdk_version)
        val sdkBranch = context.getString(R.string.linphone_sdk_branch)
        val sdkUserAgent = "$sdkVersion ($sdkBranch)"
        core.setUserAgent(userAgent, sdkUserAgent)
        val imei = preferences.getString(IMEI, "")

        core.defaultProxyConfig?.setCustomHeader("imei:", imei)
        core.defaultProxyConfig?.setCustomHeader("Company_key:", companySecret)
        core.defaultProxyConfig?.setCustomHeader("Lat:", location_lat)
        core.defaultProxyConfig?.setCustomHeader("Long:", location_long)
    }

    private fun initUserCertificates() {
        val userCertsPath = corePreferences.userCertificatesPath
        val f = File(userCertsPath)
        if (!f.exists()) {
            if (!f.mkdir()) {
                Log.e("[Context] $userCertsPath can't be created.")
            }
        }
        core.userCertificatesPath = userCertsPath
    }

    /* Call related functions */

    fun answerCallVideoUpdateRequest(call: Call, accept: Boolean) {
        val params = core.createCallParams(call)

        if (accept) {
            params?.enableVideo(true)
            core.enableVideoCapture(true)
            core.enableVideoDisplay(true)
        } else {
            params?.enableVideo(false)
        }

        call.acceptUpdate(params)
    }

    fun answerCall(call: Call) {
        Log.i("[Context] Answering call $call")
        val params = core.createCallParams(call)
        params?.recordFile = LinphoneUtils.getRecordingFilePathForAddress(call.remoteAddress)
        if (LinphoneUtils.checkIfNetworkHasLowBandwidth(context)) {
            Log.w("[Context] Enabling low bandwidth mode!")
            params?.enableLowBandwidth(true)
        }
        call.acceptWithParams(params)
    }

    fun declineCall(call: Call) {
        val voiceMailUri = corePreferences.voiceMailUri
        if (voiceMailUri != null && corePreferences.redirectDeclinedCallToVoiceMail) {
            val voiceMailAddress = core.interpretUrl(voiceMailUri)
            if (voiceMailAddress != null) {
                Log.i("[Context] Redirecting call $call to voice mail URI: $voiceMailUri")
                call.redirectTo(voiceMailAddress)
            }
        } else {
            Log.i("[Context] Declining call $call")
            call.decline(Reason.Declined)
        }
    }

    fun terminateCall(call: Call) {
        Log.i("[Context] Terminating call $call")
        call.terminate()
    }

    fun transferCallTo(addressToCall: String) {
        val currentCall = core.currentCall ?: core.calls.first()
        if (currentCall == null) {
            Log.e("[Context] Couldn't find a call to transfer")
        } else {
            val address = core.interpretUrl(addressToCall)
            if (address != null) {
                Log.i("[Context] Transferring current call to $addressToCall")
                currentCall.transferTo(address)
            }
        }
    }

    fun startCall(to: String) {
        var stringAddress = to
        if (android.util.Patterns.PHONE.matcher(to).matches()) {
            val contact: Contact? = contactsManager.findContactByPhoneNumber(to)
            val alias = contact?.getContactForPhoneNumberOrAddress(to)
            if (alias != null) {
                Log.i("[Context] Found matching alias $alias for phone number $to, using it")
                stringAddress = alias
            }
        }

        val address: Address? = core.interpretUrl(stringAddress)
        if (address == null) {
            Log.e("[Context] Failed to parse $stringAddress, abort outgoing call")
            callErrorMessageResourceId.value = Event(R.string.call_error_network_unreachable)
            return
        }

        startCall(address)
    }

    fun startCall(address: Address, forceZRTP: Boolean = false) {
        if (!core.isNetworkReachable) {
            Log.e("[Context] Network unreachable, abort outgoing call")
            callErrorMessageResourceId.value = Event(R.string.call_error_network_unreachable)
            return
        }

        val params = core.createCallParams(null)
        if (forceZRTP) {
            params?.mediaEncryption = MediaEncryption.ZRTP
        }
        if (LinphoneUtils.checkIfNetworkHasLowBandwidth(context)) {
            Log.w("[Context] Enabling low bandwidth mode!")
            params?.enableLowBandwidth(true)
        }
        params?.recordFile = LinphoneUtils.getRecordingFilePathForAddress(address)

        val deviceName: String = corePreferences.deviceName
        val appName: String = context.resources.getString(R.string.app_name)
        val androidVersion = BuildConfig.VERSION_NAME
        val preferences: SharedPreferences = context.getSharedPreferences("KothaDialer", Context.MODE_PRIVATE)
        val companySecret = preferences.getString(COMPANY_SECRET_KEY, "")
        val imei = preferences.getString(IMEI, "")

        val location_lat = preferences.getString(LAT_KEY, "")
        val location_long = preferences.getString(LONG_KEY, "")

        params?.addCustomHeader("imei:", imei)
        params?.addCustomHeader("Company_key:", companySecret)
        params?.addCustomHeader("Lat:", location_lat)
        params?.addCustomHeader("Long:", location_long)

        val call = if (params != null) {
            core.inviteAddressWithParams(address, params)
        } else {
            core.inviteAddress(address)
        }
        Log.i("[Context] Starting call $call")
    }

    fun switchCamera() {
        val currentDevice = core.videoDevice
        Log.i("[Context] Current camera device is $currentDevice")

        for (camera in core.videoDevicesList) {
            if (camera != currentDevice && camera != "StaticImage: Static picture") {
                Log.i("[Context] New camera device will be $camera")
                core.videoDevice = camera
                break
            }
        }

        val conference = core.conference
//        if (conference == null || !conference.isIn) {
//            val call = core.currentCall
//            if (call == null) {
//                Log.w("[Context] Switching camera while not in call")
//                return
//            }
//            call.update(null)
//        }
    }

    fun showSwitchCameraButton(): Boolean {
        return core.videoDevicesList.size > 2 // Count StaticImage camera
    }

    fun isVideoCallOrConferenceActive(): Boolean {
//        val conference = core.conference
//        return if (conference != null && conference.isIn) {
//            conference.currentParams.isVideoEnabled
//        } else {
//            core.currentCall?.currentParams?.videoEnabled() ?: false
//        }
        return false // new added by yamin
    }

    fun createCallOverlay() {
        if (!corePreferences.showCallOverlay || !corePreferences.systemWideCallOverlay || callOverlay != null) {
            return
        }

        if (overlayY == 0f) overlayY = AppUtils.pixelsToDp(40f)
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params: WindowManager.LayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            Compatibility.getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.x = overlayX.toInt()
        params.y = overlayY.toInt()
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        val overlay = LayoutInflater.from(context).inflate(R.layout.call_overlay, null)

        var initX = overlayX
        var initY = overlayY
        overlay.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = params.x - event.rawX
                    initY = params.y - event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    val x = (event.rawX + initX).toInt()
                    val y = (event.rawY + initY).toInt()

                    params.x = x
                    params.y = y
                    windowManager.updateViewLayout(overlay, params)
                }
                MotionEvent.ACTION_UP -> {
                    if (abs(overlayX - params.x) < 5 && abs(overlayY - params.y) < 5) {
                        view.performClick()
                    }
                    overlayX = params.x.toFloat()
                    overlayY = params.y.toFloat()
                }
                else -> return@setOnTouchListener false
            }
            true
        }
        overlay.setOnClickListener {
            Log.i("[Context] Overlay clicked, go back to call view")
            onCallStarted()
        }

        callOverlay = overlay
        windowManager.addView(overlay, params)
    }

    fun removeCallOverlay() {
        if (callOverlay != null) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.removeView(callOverlay)
            callOverlay = null
        }
    }

    /* Coroutine related */

    fun exportFilesInMessageToMediaStore(message: ChatMessage) {
        if (message.isEphemeral) {
            Log.w("[Context] Do not make ephemeral file(s) public")
            return
        }
        if (corePreferences.vfsEnabled) {
            Log.w("[Context] Do not make received file(s) public when VFS is enabled")
            return
        }
        if (!corePreferences.makePublicMediaFilesDownloaded) {
            Log.w("[Context] Making received files public setting disabled")
            return
        }

        if (Version.sdkAboveOrEqual(Version.API29_ANDROID_10) || PermissionHelper.get().hasWriteExternalStorage()) {
            for (content in message.contents) {
                if (content.isFile && content.filePath != null && content.userData == null) {
                    addContentToMediaStore(content)
                }
            }
        } else {
            Log.e("[Context] Can't make file public, app doesn't have WRITE_EXTERNAL_STORAGE permission")
        }
    }

    fun addContentToMediaStore(content: Content) {
        coroutineScope.launch {
            when (content.type) {
                "image" -> {
                    if (Compatibility.addImageToMediaStore(context, content)) {
                        Log.i("[Context] Adding image ${content.name} to Media Store terminated")
                    } else {
                        Log.e("[Context] Something went wrong while copying file to Media Store...")
                    }
                }
                "video" -> {
                    if (Compatibility.addVideoToMediaStore(context, content)) {
                        Log.i("[Context] Adding video ${content.name} to Media Store terminated")
                    } else {
                        Log.e("[Context] Something went wrong while copying file to Media Store...")
                    }
                }
                "audio" -> {
                    if (Compatibility.addAudioToMediaStore(context, content)) {
                        Log.i("[Context] Adding audio ${content.name} to Media Store terminated")
                    } else {
                        Log.e("[Context] Something went wrong while copying file to Media Store...")
                    }
                }
                else -> {
                    Log.w("[Context] File ${content.name} isn't either an image, an audio file or a video, can't add it to the Media Store")
                }
            }
        }
    }

    fun checkIfForegroundServiceNotificationCanBeRemovedAfterDelay(delayInMs: Long) {
        coroutineScope.launch {
            withContext(Dispatchers.Default) {
                delay(delayInMs)
                withContext(Dispatchers.Main) {
                    if (core.defaultAccount != null && core.defaultAccount?.state == RegistrationState.Ok) {
                        Log.i("[Context] Default account is registered, cancel foreground service notification if possible")
                        notificationsManager.stopForegroundNotificationIfPossible()
                    }
                }
            }
        }
    }

    /* Start call related activities */

    private fun onIncomingReceived() {
        if (corePreferences.preventInterfaceFromShowingUp) {
            Log.w("[Context] We were asked to not show the incoming call screen")
            return
        }

        Log.i("[Context] Starting IncomingCallActivity")
        val intent = Intent(context, IncomingCallActivity::class.java)
        // This flag is required to start an Activity from a Service context
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun onOutgoingStarted() {
        if (corePreferences.preventInterfaceFromShowingUp) {
            Log.w("[Context] We were asked to not show the outgoing call screen")
            return
        }

        Log.i("[Context] Starting OutgoingCallActivity")
        val intent = Intent(context, OutgoingCallActivity::class.java)
        // This flag is required to start an Activity from a Service context
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun onCallStarted() {
        if (corePreferences.preventInterfaceFromShowingUp) {
            Log.w("[Context] We were asked to not show the call screen")
            return
        }

        Log.i("[Context] Starting CallActivity")
        val intent = Intent(context, CallActivity::class.java)
        // This flag is required to start an Activity from a Service context
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        context.startActivity(intent)
    }

    /* VFS */

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val ALIAS = "vfs"
        private const val LINPHONE_VFS_ENCRYPTION_AES256GCM128_SHA256 = 2
        private const val VFS_IV = "vfsiv"
        private const val VFS_KEY = "vfskey"

        @Throws(java.lang.Exception::class)
        private fun generateSecretKey() {
            val keyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            keyGenerator.generateKey()
        }

        @Throws(java.lang.Exception::class)
        private fun getSecretKey(): SecretKey? {
            val ks = KeyStore.getInstance(ANDROID_KEY_STORE)
            ks.load(null)
            val entry = ks.getEntry(ALIAS, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }

        @Throws(java.lang.Exception::class)
        fun generateToken(): String {
            return sha512(UUID.randomUUID().toString())
        }

        @Throws(java.lang.Exception::class)
        private fun encryptData(textToEncrypt: String): Pair<ByteArray, ByteArray> {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            return Pair<ByteArray, ByteArray>(
                iv,
                cipher.doFinal(textToEncrypt.toByteArray(StandardCharsets.UTF_8))
            )
        }

        @Throws(java.lang.Exception::class)
        private fun decryptData(encrypted: String?, encryptionIv: ByteArray): String {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, encryptionIv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val encryptedData = Base64.decode(encrypted, Base64.DEFAULT)
            return String(cipher.doFinal(encryptedData), StandardCharsets.UTF_8)
        }

        @Throws(java.lang.Exception::class)
        fun encryptToken(string_to_encrypt: String): Pair<String?, String?> {
            val encryptedData = encryptData(string_to_encrypt)
            return Pair<String?, String?>(
                Base64.encodeToString(encryptedData.first, Base64.DEFAULT),
                Base64.encodeToString(encryptedData.second, Base64.DEFAULT)
            )
        }

        @Throws(java.lang.Exception::class)
        fun sha512(input: String): String {
            val md = MessageDigest.getInstance("SHA-512")
            val messageDigest = md.digest(input.toByteArray())
            val no = BigInteger(1, messageDigest)
            var hashtext = no.toString(16)
            while (hashtext.length < 32) {
                hashtext = "0$hashtext"
            }
            return hashtext
        }

        @Throws(java.lang.Exception::class)
        fun getVfsKey(sharedPreferences: SharedPreferences): String {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
            keyStore.load(null)
            return decryptData(
                sharedPreferences.getString(VFS_KEY, null),
                Base64.decode(sharedPreferences.getString(VFS_IV, null), Base64.DEFAULT)
            )
        }

        fun activateVFS() {
            try {
                Log.i("[Context] Activating VFS")

                if (corePreferences.encryptedSharedPreferences.getString(VFS_IV, null) == null) {
                    generateSecretKey()
                    encryptToken(generateToken()).let { data ->
                        corePreferences.encryptedSharedPreferences
                            .edit()
                            .putString(VFS_IV, data.first)
                            .putString(VFS_KEY, data.second)
                            .commit()
                    }
                }
                Factory.instance().setVfsEncryption(
                    LINPHONE_VFS_ENCRYPTION_AES256GCM128_SHA256,
                    getVfsKey(corePreferences.encryptedSharedPreferences).toByteArray().copyOfRange(0, 32),
                    32
                )

                Log.i("[Context] VFS activated")
            } catch (e: Exception) {
                Log.f("[Context] Unable to activate VFS encryption: $e")
            }
        }
    }
}
