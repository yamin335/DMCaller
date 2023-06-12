package com.bdcom.appdialer.activities

import android.app.AlertDialog
import android.content.*
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.bdcom.appdialer.LinphoneApplication
import com.bdcom.appdialer.LinphoneApplication.Companion.coreContext
import com.bdcom.appdialer.R
import com.bdcom.appdialer.activities.call.CallActivity
import com.bdcom.appdialer.fragments.*
import com.bdcom.appdialer.models.CompanySecretRequest
import com.bdcom.appdialer.models.CompanySecretResponse
import com.bdcom.appdialer.models.CompanySecretVerifyRequest
import com.bdcom.appdialer.models.CompanySecretVerifyResponse
import com.bdcom.appdialer.network_utils.ApiHelper
import com.bdcom.appdialer.receivers.BaseReceiver
import com.bdcom.appdialer.utils.*
import com.bdcom.appdialer.utils.Constants.API_KEY
import com.bdcom.appdialer.utils.Constants.APP_BACKGROUND_KEY
import com.bdcom.appdialer.utils.Constants.APP_EXIT_KEY
import com.bdcom.appdialer.utils.Constants.COMPANY_ID_KEY
import com.bdcom.appdialer.utils.Constants.COMPANY_SECRET_KEY
import com.bdcom.appdialer.utils.Constants.MOBILE_NUMBER_KEY
import com.bdcom.appdialer.utils.Constants.PASSWORD_KEY
import com.bdcom.appdialer.utils.Constants.SERVICE_STATUS_KEY
import com.bdcom.appdialer.utils.Constants.USER_ID_KEY
import com.bdcom.appdialer.utils.Constants.USER_NAME_KEY
import com.bdcom.appdialer.utils.Event
import com.ncapdevi.fragnav.FragNavController
import com.ncapdevi.fragnav.FragNavLogger
import com.ncapdevi.fragnav.FragNavSwitchController
import com.ncapdevi.fragnav.FragNavTransactionOptions
import com.ncapdevi.fragnav.tabhistory.UniqueTabHistoryStrategy
import com.roughike.bottombar.BottomBar
import java.util.*
import org.linphone.core.*

// import com.xuchongyang.easyphone.EasyLinphone
// import com.xuchongyang.easyphone.callback.PhoneCallback
// import com.xuchongyang.easyphone.callback.RegistrationCallback
// import com.xuchongyang.easyphone.linphone.LinphoneManager
// import com.xuchongyang.easyphone.service.LinphoneService
// import org.linphone.core.LinphoneCall

class BottomTabsActivity : GenericActivity(), BaseFragment.FragmentNavigation,
        FragNavController.TransactionListener, FragNavController.RootFragmentListener, LifecycleObserver {

    private var online: Boolean = false

    override val numberOfRootFragments: Int = 5

    private val fragNavController: FragNavController = FragNavController(supportFragmentManager, R.id.fragmentContainer)

    private lateinit var bottomBar: BottomBar

    private lateinit var receiver: NetworkChangeReceiver

    private var menu: Menu? = null

    private var mListener: CoreListenerStub? = null

    private val PERMISSIONS_READ_EXTERNAL_STORAGE_DEVICE_RINGTONE = 210
    private val PERMISSIONS_READ_CONTACT = 220
    private val REQUEST_CONTACT_FOR_ADD_TO_EXISTING = 400

    private var shouldCallRefreshInFirstTime = false

    var contactsFragment: ContactsFragment? = null

    private var mProxyConfig: ProxyConfig? = null
    private var mAuthInfo: AuthInfo? = null

    private var progress: Progress? = null

    private var registrationStatusText: Int = R.string.status_not_connected
    private var registrationStatusDrawable: Int = R.drawable.led_not_registered

    private lateinit var backButton: LinearLayout

    private val listener: CoreListenerStub = object : CoreListenerStub() {
        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState,
            message: String
        ) {
            if (account == core.defaultAccount) {
                updateDefaultAccountRegistrationStatus(state)
            } else if (core.accountList.isEmpty()) {
                // Update registration status when default account is removed
                registrationStatusText = getStatusIconText(state)
                registrationStatusDrawable = getStatusIconResource(state)
            }
        }

        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State,
            message: String
        ) {
            if (state == Call.State.End || state == Call.State.Error || state == Call.State.Released) {
                AppUtils.isConference = false
                backButton.visibility = if (AppUtils.isConference) View.VISIBLE else View.GONE
            }
        }
    }

    // Fix private lateinit var mPrefs: LinphonePreferences

    // private lateinit var pushNotificationReceiver: BroadcastReceiver

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onForegroundStart() {
        val preferences = LinphoneApplication.instance.getSharedPreferences()
        val preferenceEditor = preferences.edit()
        preferenceEditor.putBoolean(APP_BACKGROUND_KEY, false)
        preferenceEditor.apply()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onForegroundStop() {
        val preferences = LinphoneApplication.instance.getSharedPreferences()
        val preferenceEditor = preferences.edit()
        preferenceEditor.putBoolean(APP_BACKGROUND_KEY, true)
        preferenceEditor.apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val preferences = LinphoneApplication.instance.getSharedPreferences()
        val isLoggedIn = preferences.getBoolean(Constants.LOGIN_KEY, false)
        if (!isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        started = true
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bottom_tabs)

        val preference = LinphoneApplication.instance.getSharedPreferences()
        var savedTone = preference.getString(Constants.RING_TONE_PATH, "")
        val defaultRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString()

        configureAccount()

        if (savedTone.isNullOrBlank()) {
            savedTone = defaultRingtone
            preference.edit().putString(Constants.RING_TONE_PATH, savedTone).apply()
            coreContext.core.ring = savedTone
        }

        backButton = findViewById(R.id.btnBackToCall)
        backButton.setOnClickListener {
            AppUtils.isConference = false
            backButton.visibility = if (AppUtils.isConference) View.VISIBLE else View.GONE
            startActivity(Intent(this, CallActivity::class.java))
        }

        Log.e("FCM_TOKEN", preferences.getString(Constants.FCM_KEY, "Not Found") ?: "Not Found")

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // prepareBroadcastReceiverForPushNotification()

        progress = Progress(this)

//        if (LinphoneService.isReady()) {
//           LinphoneManager.getCore().start()
//        }

        if (preferences.getBoolean(APP_EXIT_KEY, false)) {
//            val usernumber = preferences.getString("username", "") ?: ""
//            val password = preferences.getString("password", "") ?: ""
//            val serverIp = preferences.getString("serverIP", "") ?: ""
//            saveCreatedAccount(usernumber, password, serverIp)
//
//            Log.e("ReRegister:", "name: " + usernumber)
//
//            val preferenceEditor = preferences.edit()
//            preferenceEditor.putBoolean("isAppExit", false)
//            preferenceEditor.apply()
        } else {
//            val usernumber = preferences.getString("username", "")
//            val password = preferences.getString("password", "")
//            var serverIp = preferences.getString("serverIP", "")
//            saveCreatedAccount(usernumber, password, serverIp)
//
//            Log.e("ReRegister:", "name: " + usernumber)
//
//            val preferenceEditor = preferences.edit()
//            preferenceEditor.putBoolean("isAppExit", false)
//            preferenceEditor.apply()
        }

      //  LinphoneService.checkBackground = preferences.getBoolean("isAppInBackground", false)

        if (supportActionBar != null) {

            // supportActionBar?.subtitle = Html.fromHtml("<small>" + preferences.getString("username", "") + "</small>");
        }

        // val preferences = LinphoneApplication.instance.getSharedPreferences()
        val usernumber = preferences.getString(USER_NAME_KEY, "")
        val password = preferences.getString(PASSWORD_KEY, "")
        // val userName = preferences.getString("displayName", "")

        //  saveCreatedAccount(usernumber, password, LinphoneApplication.SERVER_IP)

        shouldCallRefreshInFirstTime = true
        receiver = NetworkChangeReceiver()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(receiver, filter)

        bottomBar = findViewById(R.id.bottomBar)

        fragNavController.apply {
            transactionListener = this@BottomTabsActivity
            rootFragmentListener = this@BottomTabsActivity
            createEager = true
            fragNavLogger = object : FragNavLogger {
                override fun error(message: String, throwable: Throwable) {
                    Log.e(TAG, message, throwable)
                }
            }

            fragmentHideStrategy = FragNavController.HIDE

            navigationStrategy = UniqueTabHistoryStrategy(object : FragNavSwitchController {
                override fun switchTab(index: Int, transactionOptions: FragNavTransactionOptions?) {
                    bottomBar.selectTabAtPosition(index)
                }
            })
        }

        val initialTabIndex = intent.getIntExtra("selectionIndex", INDEX_DIALER)
        fragNavController.initialize(initialTabIndex, savedInstanceState)
        val initial = savedInstanceState == null
        if (initial) {
            bottomBar.selectTabAtPosition(initialTabIndex)
        }

        fragNavController.executePendingTransactions()
        bottomBar.setOnTabSelectListener({ tabId ->
            when (tabId) {
                R.id.menu_contacts -> fragNavController.switchTab(INDEX_CONTACTS)
                R.id.menu_dialer -> fragNavController.switchTab(INDEX_DIALER)
                R.id.menu_call_history -> fragNavController.switchTab(INDEX_CALL_HISTORY)
                // R.id.menu_conf -> fragNavController.switchTab(INDEX_CONFERENCE)
                R.id.menu_settings -> fragNavController.switchTab(INDEX_SETTINGS)
            }
        }, initial)

        bottomBar.setOnTabReselectListener { fragNavController.clearStack() }
//        if (!LinphoneService.isReady()) run {
//            EasyLinphone.startService(this)
//            EasyLinphone.addCallback(object : RegistrationCallback() {
//                override fun registrationOk() {
//                    super.registrationOk()
//                    Log.e("BottomTabsActivity", "registrationOk: ")
//                    online = true
//                    updateMenu()
//                }
//
//                override fun registrationFailed() {
//                    super.registrationFailed()
//                    Log.e("BottomTabsActivity", "registrationFailed: ")
//                   online = false
//                    updateMenu()
//                }
//            }, null)
//        }

        // val preferences = LinphoneApplication.instance.getSharedPreferences()
        if (!preferences.getBoolean(SERVICE_STATUS_KEY, false)) {
            val preferenceEditor = preferences.edit()
            preferenceEditor.putBoolean(SERVICE_STATUS_KEY, true)
            preferenceEditor.apply()
            Log.e("Notification", "show notification")

//            var intent = Intent(this, ForegroundService::class.java)
//            intent.action = Constants.ACTION.STARTFOREGROUND_ACTION
//            startService(intent)
        }

//        mListener = object : CoreListenerStub() {
//            override fun onConfiguringStatus(core: Core, status: ConfiguringState?, message: String?) {
//                if (status == ConfiguringState.Successful) {
//
//                } else if (status == ConfiguringState.Failed) {
//                    // Toast.makeText(this@OTPActivity, "Failed to download or apply remote provisioning profile...", Toast.LENGTH_LONG).show()
//                }
//            }
//
//            override fun onRegistrationStateChanged(core: Core, proxyConfig: ProxyConfig, state: RegistrationState?, message: String) {
//                when {
//                    state == RegistrationState.Ok -> {
//                        Log.e("OTP:", "Registration OK")
//                        online = true
//                        updateMenu()
//                    }
//                    state == RegistrationState.Failed -> {
//                        Log.e("OTP:", "Registration Failed")
//                        online = false
//                        updateMenu()
//                    }
//                    state != RegistrationState.Progress -> {
//                        Log.e("OTP:", "Registration Progress")
//                        online = false
//                        updateMenu()
//                    }
//                }
//            }
//
//            override fun onCallStateChanged(core: Core, call: Call, state: Call.State?, message: String) {
//                if (state == Call.State.IncomingReceived) {
//                    Log.e("Call", "Incoming Call into listener")
//                    if (call != null) {
//                        val number = call.remoteAddress.username ?: return
// //                        if (LinphoneService.checkDoubleCall == false) {
// //                            LinphoneService.checkDoubleCall = true
// //                            // var intent =  Intent(this@BottomTabsActivity, CallActivity::class.java)
// //                            //  startActivity(intent)
// //                            CallActivity.startIncomingCall(this@BottomTabsActivity, number)
// //                        }
//                    }
//                    // startActivity(new Intent(LinphoneActivity.instance(), CallIncomingActivity.class));
//                } else if (state == Call.State.OutgoingInit || state == Call.State.OutgoingProgress) {
//                    // startActivity(new Intent(LinphoneActivity.instance(), CallOutgoingActivity.class));
//                } else if (state == Call.State.End || state == Call.State.Error || state == Call.State.Released) {
//                    // resetClassicMenuLayoutAndGoBackToCallIfStillRunning();
//                }
//
//                // int missedCalls = LinphoneManager.getLc().getMissedCallsCount();
//                // displayMissedCalls(missedCalls);
//            }
//        }

        /* Fix mListener = object : CoreListenerStub() {
            override fun onCallStateChanged(
                core: Core,
                call: Call,
                state: Call.State,
                message: String
            ) {
                if (state == Call.State.End || state == Call.State.Released) {
                    // displayMissedCalls()
                }
            }

            override fun onMessageReceived(core: Core, room: ChatRoom, message: ChatMessage) {
                // displayMissedChats()
            }

            override fun onChatRoomRead(core: Core, room: ChatRoom) {
                // displayMissedChats()
            }

            override fun onMessageReceivedUnableDecrypt(
                core: Core,
                room: ChatRoom,
                message: ChatMessage
            ) {
                // displayMissedChats()
            }

            override fun onRegistrationStateChanged(
                core: Core,
                proxyConfig: ProxyConfig,
                state: RegistrationState,
                message: String
            ) {
                // mSideMenuFragment.displayAccountsInSideMenu()
                if (state == RegistrationState.Ok) {
                    online = true
                    updateMenu()
                    // For push notifications to work on some devices,
                    // app must be in "protected mode" in battery settings...
                    // https://stackoverflow.com/questions/31638986/protected-apps-setting-on-huawei-phones-and-how-to-handle-it
                    DeviceUtils
                            .displayDialogIfDeviceHasPowerManagerThatCouldPreventPushNotifications(
                                    this@BottomTabsActivity)
                    if (resources.getBoolean(R.bool.use_phone_number_validation)) {
                        val authInfo = core.findAuthInfo(
                                proxyConfig.realm,
                                proxyConfig.identityAddress!!.username!!,
                                proxyConfig.domain)
                        if (authInfo != null &&
                                (authInfo.domain
                                        == getString(R.string.default_domain))) {
                            LinphoneManager.getInstance().isAccountWithAlias()
                        }
                    }
                    if (!Compatibility.isDoNotDisturbSettingsAccessGranted(
                                    this@BottomTabsActivity)) {
                        // displayDNDSettingsDialog()
                    }
                } else if (state == RegistrationState.Failed) {
                    online = false
                    updateMenu()
                }
            }

            override fun onLogCollectionUploadStateChanged(
                core: Core,
                state: LogCollectionUploadState,
                info: String
            ) {
                org.linphone.core.tools.Log.d(
                        "[Main Activity] Log upload state: " +
                                state.toString() +
                                ", info = " +
                                info)
                if (state == LogCollectionUploadState.Delivered) {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Logs url", info)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(
                            this@BottomTabsActivity,
                            getString(R.string.logs_url_copied_to_clipboard),
                            Toast.LENGTH_SHORT)
                            .show()
                    // shareUploadedLogsUrl(info)
                }
            }
        }

        configureAccount()
        // startLinPhoneService()
        ensureServiceIsRunning()

        val lc = LinphoneManager.getCore()
        if (lc != null) {
            Log.e("OTP", "lc not null from create")
            checkCodec()
        } */

//        val companySecret = preferences.getString(COMPANY_SECRET_KEY, "")
//
//        if (companySecret.isNullOrBlank()) {
//            val companySecretRequest = CompanySecretRequest(preferences.getString(USER_ID_KEY, "")
//                ?: "", API_KEY,
//                preferences.getString(MOBILE_NUMBER_KEY, "")
//                    ?: "", preferences.getString(COMPANY_ID_KEY, "") ?: "")
//            getCompanySecret(companySecretRequest)
//        } else {
//            val companySecretVerifyRequest = CompanySecretVerifyRequest(preferences.getString(USER_ID_KEY, "")
//                ?: "", API_KEY,
//                preferences.getString(MOBILE_NUMBER_KEY, "")
//                    ?: "", preferences.getString(COMPANY_ID_KEY, "") ?: "",
//                companySecret)
//            verifyCompanySecret(companySecretVerifyRequest)
//        }
    }

    private lateinit var accountCreator: AccountCreator
    private var proxyConfigToCheck: ProxyConfig? = null

    private fun configureAccount() {

        LinphoneApplication.coreContext.core.loadConfigFromXml(LinphoneApplication.corePreferences.defaultValuesPath)
        accountCreator = LinphoneApplication.coreContext.core.createAccountCreator(
            LinphoneApplication.corePreferences.xmlRpcServerUrl)
        accountCreator.language = Locale.getDefault().language

        val preferences = LinphoneApplication.instance.getSharedPreferences()
        accountCreator.username = preferences.getString(USER_NAME_KEY, "")
        accountCreator.domain = preferences.getString(Constants.SERVER_IP_KEY, "")
        accountCreator.password = preferences.getString(PASSWORD_KEY, "")
        accountCreator.displayName = preferences.getString(Constants.DISPLAY_NAME_KEY, "")
        accountCreator.transport = TransportType.Udp

        // LinphoneApplication.coreContext.core.addListener(coreListener)

        val proxyConfig: ProxyConfig? = accountCreator.createProxyConfig()
        // proxyConfig?.dialPrefix = "+880"
        proxyConfigToCheck = proxyConfig

//        val transports = LinphoneApplication.coreContext.core.transports
//        transports.udpPort = 15605
//        transports.tcpPort = 15605
//        transports.tlsPort = -1
//        LinphoneApplication.coreContext.core.transports = transports

        if (proxyConfig == null) {
            org.linphone.core.tools.Log.e("[Assistant] [Generic Login] Account creator couldn't create proxy config")
            // LinphoneApplication.coreContext.core.removeListener(coreListener)
            return
        }

        org.linphone.core.tools.Log.i("[Assistant] [Generic Login] Proxy config created")
        // The following is required to keep the app alive
        // and be able to receive calls while in background
        if (accountCreator.domain.orEmpty() != LinphoneApplication.corePreferences.defaultDomain) {
            org.linphone.core.tools.Log.i("[Assistant] [Generic Login] Background mode with foreground service automatically enabled")
            LinphoneApplication.corePreferences.keepServiceAlive = true
            LinphoneApplication.coreContext.notificationsManager.startForeground()
        }

        // waitForServerAnswer.value = true
        // LinphoneApplication.coreContext.core.addListener(coreListener)
        // if (!createProxyConfig()) {
        // waitForServerAnswer.value = false
        // LinphoneApplication.coreContext.core.removeListener(coreListener)
        // onErrorEvent.value = Event("Error: Failed to create account object")
        // }
//        } else {
//            val result = AccountCreator.PhoneNumberStatus.fromInt(accountCreator.setPhoneNumber(phoneNumber.value, prefix.value))
//            if (result != AccountCreator.PhoneNumberStatus.Ok) {
//                org.linphone.core.tools.Log.e("[Assistant] [Account Login] Error [$result] setting the phone number: ${phoneNumber.value} with prefix: ${prefix.value}")
//                phoneNumberError.value = result.name
//                return
//            }
//            org.linphone.core.tools.Log.i("[Assistant] [Account Login] Phone number is ${accountCreator.phoneNumber}")
//
//            val result2 = accountCreator.setUsername(accountCreator.phoneNumber)
//            if (result2 != AccountCreator.UsernameStatus.Ok) {
//                org.linphone.core.tools.Log.e("[Assistant] [Account Login] Error [${result2.name}] setting the username: ${accountCreator.phoneNumber}")
//                usernameError.value = result2.name
//                return
//            }
//            org.linphone.core.tools.Log.i("[Assistant] [Account Login] Username is ${accountCreator.username}")
//
//            waitForServerAnswer.value = true
//            val status = accountCreator.recoverAccount()
//            org.linphone.core.tools.Log.i("[Assistant] [Account Login] Recover account returned $status")
//            if (status != AccountCreator.Status.RequestOk) {
//                waitForServerAnswer.value = false
//                onErrorEvent.value = Event("Error: ${status.name}")
//            }
//        }
    }
    fun refreshRegister() {
        coreContext.core.refreshRegisters()
    }

    fun updateDefaultAccountRegistrationStatus(state: RegistrationState) {
        registrationStatusText = getStatusIconText(state)
        registrationStatusDrawable = getStatusIconResource(state)
        updateMenu()
    }

    private fun getStatusIconText(state: RegistrationState): Int {
        return when (state) {
            RegistrationState.Ok -> R.string.status_connected
            RegistrationState.Progress -> R.string.status_in_progress
            RegistrationState.Failed -> R.string.status_error
            else -> R.string.status_not_connected
        }
    }

    private fun getStatusIconResource(state: RegistrationState): Int {
        return when (state) {
            RegistrationState.Ok -> R.drawable.led_registered
            RegistrationState.Progress -> R.drawable.led_registration_in_progress
            RegistrationState.Failed -> R.drawable.led_error
            else -> R.drawable.led_error
        }
    }

//    private fun configureAccount() {
//        val core = LinphoneManager.getCore()
//        if (core != null) {
//            org.linphone.core.tools.Log.i("[Generic Connection Assistant] Reloading configuration with default")
//            reloadDefaultAccountCreatorConfig()
//        }
//
//        val preferences = LinphoneApplication.instance.getSharedPreferences()
//        val accountCreator: AccountCreator = getAccountCreator()
//        accountCreator.username = preferences.getString(USER_NAME_KEY, "")
//        accountCreator.domain = preferences.getString(SERVER_IP_KEY, "")
//        accountCreator.password = preferences.getString(PASSWORD_KEY, "")
//        accountCreator.displayName = preferences.getString(DISPLAY_NAME_KEY, "")
//        accountCreator.transport = TransportType.Udp
//        // LinphonePreferences.instance().setAccountEnabled(0, true)
//
//        // val auth = LinphonePreferences.instance().getAuthInfo(0)
//
// //        when (mTransport.getCheckedRadioButtonId()) {
// //            R.id.transport_udp -> accountCreator.transport = TransportType.Udp
// //            R.id.transport_tcp -> accountCreator.transport = TransportType.Tcp
// //            R.id.transport_tls -> accountCreator.transport = TransportType.Tls
// //        }
//        createProxyConfigAndLeaveAssistant(true)
//    }

//    fun ensureServiceIsRunning() {
//
//        val mProxyConfig = LinphoneManager.getCore().defaultProxyConfig
//        if (mProxyConfig != null) {
//            mProxyConfig.edit()
//            mProxyConfig.avpfMode = AVPFMode.Disabled
//            // mAvpfInterval.setEnabled(mProxyConfig!!.avpfEnabled())
//            mProxyConfig.done()
//        } else {
//            org.linphone.core.tools.Log.e("[Account Settings] No proxy config !")
//        }
//
//        if (!LinphoneService.isReady()) {
//            if (!LinphoneContext.isReady()) {
//                LinphoneContext(applicationContext)
//                // LinphoneContext.instance().start(false)
//                org.linphone.core.tools.Log.i("[Generic Activity] Context created & started")
//            }
//            org.linphone.core.tools.Log.i("[Generic Activity] Starting Service")
//            try {
//                startService(Intent().setClass(this, LinphoneService::class.java))
//            } catch (ise: java.lang.IllegalStateException) {
//                org.linphone.core.tools.Log.e("[Generic Activity] Couldn't start service, exception: ", ise)
//            }
//        }
//    }

//    fun getAccountCreator(): AccountCreator {
//        return LinphoneManager.getInstance().accountCreator
//    }
//
//    private fun reloadAccountCreatorConfig(path: String) {
//        val core = LinphoneManager.getCore()
//        if (core != null) {
//            core.loadConfigFromXml(path)
//            val accountCreator = getAccountCreator()
//            accountCreator.reset()
//            accountCreator.language = Locale.getDefault().language
//        }
//    }
//
//    fun reloadDefaultAccountCreatorConfig() {
//        org.linphone.core.tools.Log.i("[Assistant] Reloading configuration with default")
//        reloadAccountCreatorConfig(LinphonePreferences.instance().defaultDynamicConfigFile)
//    }
//
//    fun reloadLinphoneAccountCreatorConfig() {
//        org.linphone.core.tools.Log.i("[Assistant] Reloading configuration with specifics")
//        reloadAccountCreatorConfig(LinphonePreferences.instance().linphoneDynamicConfigFile)
//    }
//
//    fun createProxyConfigAndLeaveAssistant() {
//        createProxyConfigAndLeaveAssistant(false)
//    }
//
//    fun createProxyConfigAndLeaveAssistant(isGenericAccount: Boolean) {
//        val core = LinphoneManager.getCore()
//        val useLinphoneDefaultValues = getString(R.string.default_domain) == getAccountCreator().domain
//        if (isGenericAccount) {
//            if (useLinphoneDefaultValues) {
//                org.linphone.core.tools.Log.i(
//                        "[Assistant] Default domain found for generic connection, reloading configuration")
//                core.loadConfigFromXml(
//                        LinphonePreferences.instance().linphoneDynamicConfigFile)
//            } else {
//                org.linphone.core.tools.Log.i("[Assistant] Third party domain found, keeping default values")
//            }
//        }
//        val proxyConfig = getAccountCreator().createProxyConfig()
//        if (isGenericAccount) {
//            if (useLinphoneDefaultValues) {
//                // Restore default values
//                org.linphone.core.tools.Log.i("[Assistant] Restoring default assistant configuration")
//                core.loadConfigFromXml(
//                        LinphonePreferences.instance().defaultDynamicConfigFile)
//            } else {
//                // If this isn't a sip.linphone.org account, disable push notifications and enable
//                // service notification, otherwise incoming calls won't work (most probably)
//                if (proxyConfig != null) {
//                    proxyConfig.isPushNotificationAllowed = true
//                }
//                org.linphone.core.tools.Log.w(
//                        "[Assistant] Unknown domain used, push probably won't work, enable service mode")
//                LinphonePreferences.instance().serviceNotificationVisibility = true
//                LinphoneContext.instance().notificationManager.startForeground()
//            }
//        }
//        if (proxyConfig == null) {
//            org.linphone.core.tools.Log.e("[Assistant] Account creator couldn't create proxy config")
//        } else {
//            if (proxyConfig.dialPrefix == null) {
//                val dialPlan: DialPlan? = getDialPlanForCurrentCountry()
//                if (dialPlan != null) {
//                    proxyConfig.dialPrefix = dialPlan.countryCallingCode
//                }
//            }
//            LinphonePreferences.instance().firstLaunchSuccessful()
//            goToLinphoneActivity()
//        }
//    }

    fun getDialPlanForCurrentCountry(): DialPlan? {
        try {
            val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            val countryIso = tm.networkCountryIso
            return getDialPlanFromCountryCode(countryIso)
        } catch (e: Exception) {
            org.linphone.core.tools.Log.e("[Assistant] $e")
        }
        return null
    }

    private fun getDialPlanFromCountryCode(countryCode: String?): DialPlan? {
        if (countryCode == null || countryCode.isEmpty()) return null
        for (c in Factory.instance().dialPlans) {
            if (countryCode.equals(c.isoCountryCode, ignoreCase = true)) return c
        }
        return null
    }

//    fun goToLinphoneActivity() {
//        val needsEchoCalibration = LinphoneManager.getCore().isEchoCancellerCalibrationRequired
//        val echoCalibrationDone = LinphonePreferences.instance().isEchoCancellationCalibrationDone
//        org.linphone.core.tools.Log.i(
//                "[Assistant] Echo cancellation calibration required ? " +
//                        needsEchoCalibration +
//                        ", already done ? " +
//                        echoCalibrationDone)
//        val intent: Intent
//        if (needsEchoCalibration && !echoCalibrationDone) {
//            intent = Intent(this, EchoCancellerCalibrationAssistantActivity::class.java)
//        } else {
//            /*boolean openH264 = LinphonePreferences.instance().isOpenH264CodecDownloadEnabled();
//            boolean codecFound =
//                    LinphoneManager.getInstance().getOpenH264DownloadHelper().isCodecFound();
//            boolean abiSupported =
//                    Version.getCpuAbis().contains("armeabi-v7a")
//                            && !Version.getCpuAbis().contains("x86");
//            boolean androidVersionOk = Version.sdkStrictlyBelow(Build.VERSION_CODES.M);
//
//            if (openH264 && abiSupported && androidVersionOk && !codecFound) {
//                intent = new Intent(this, OpenH264DownloadAssistantActivity.class);
//            } else {*/
//            intent = Intent(this, DialerActivity::class.java)
//            intent.addFlags(
//                    Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
//            // }
//        }
//        // startActivity(intent)
//        bottomBar.selectTabAtPosition(1)
//    }

    private fun getCompanySecret(request: CompanySecretRequest) {
        progress.showSafely()
        ApiHelper.getCompanySecret(request, object : ApiHelper.ApiCallbackListener<CompanySecretResponse> {
            override fun onFailed(status: Int?) {
                progress.dismissSafely()
            }

            override fun onDataFetched(response: CompanySecretResponse?) {
                response?.company_key?.let {
                    val preferences = LinphoneApplication.instance.getSharedPreferences()
                    val preferenceEditor = preferences.edit()
                    preferenceEditor.putString(COMPANY_SECRET_KEY, it)
                    preferenceEditor.apply()
                }
                progress.dismissSafely()
            }
        })
    }

//    private fun getCompanySecret(request: CompanySecretRequest) {
//        progress.showSafely()
//        ApiHelper.getCompanySecret(request, object : ApiHelper.ApiCallbackListener<CompanySecretResponse> {
//            override fun onFailed(status: Int?) {
//                progress.dismissSafely()
//            }
//
//            override fun onDataFetched(response: CompanySecretResponse?) {
//                response?.company_key?.let {
//                    val preferences = LinphoneApplication.instance.getSharedPreferences()
//                    val preferenceEditor = preferences.edit()
//                    preferenceEditor.putString(COMPANY_SECRET_KEY, it)
//                    preferenceEditor.apply()
//
//                    val companySecretVerifyRequest = CompanySecretVerifyRequest(preferences.getString(USER_ID_KEY, "")
//                            ?: "", API_KEY,
//                            preferences.getString(MOBILE_NUMBER_KEY, "")
//                                    ?: "", preferences.getString(COMPANY_ID_KEY, "") ?: "",
//                            preferences.getString(COMPANY_SECRET_KEY, "") ?: "")
//                    verifyCompanySecret(companySecretVerifyRequest)
//                }
//                progress.dismissSafely()
//            }
//        })
//    }

    fun verifyCompanySecret(request: CompanySecretVerifyRequest) {
        progress.showSafely()
        ApiHelper.verifyCompanySecret(request, object : ApiHelper.ApiCallbackListener<CompanySecretVerifyResponse> {
            override fun onFailed(status: Int?) {
                progress.dismissSafely()
                // Need to fix whether it is verified or not
            }

            override fun onDataFetched(response: CompanySecretVerifyResponse?) {
                if (response?.messages == "YES") {
                } else {
                    // Need to fix whether it is verified or not
                }
                progress.dismissSafely()
            }
        })
    }

//    private fun startLinPhoneService() {
//        val core = LinphoneManager.getCore() ?: return
//
//        // Create a proxy config if there is none
//        if (mProxyConfig == null) {
//            // Ensure the default configuration is loaded first
//            val defaultConfig = LinphonePreferences.instance().defaultDynamicConfigFile
//            core.loadConfigFromXml(defaultConfig)
//            mProxyConfig = core.createProxyConfig()
//            val preferences = LinphoneApplication.instance.getSharedPreferences()
//            mAuthInfo = Factory.instance().createAuthInfo(preferences.getString(USER_NAME_KEY, "")
//                    ?: "",
//                    preferences.getString(MOBILE_NUMBER_KEY, "")
//                            ?: "", preferences.getString(PASSWORD_KEY, ""),
//                    null, null, preferences.getString(SERVER_IP_KEY, ""))
//            Log.e("Username:", mAuthInfo?.username ?: "N/A")
//            if (mProxyConfig != null) {
//
//                mProxyConfig!!.edit()
//                var server = preferences.getString(SERVER_IP_KEY, "")
//                val serverAddr = Factory.instance().createAddress(server!!)
//                mProxyConfig!!.serverAddr = server
//                if (serverAddr != null) {
//                    try {
//                        serverAddr.transport = TransportType.fromInt(0)
//                        server = serverAddr.asString()
//                        mProxyConfig!!.setRoute(server)
//                    } catch (nfe: NumberFormatException) {
//                        org.linphone.core.tools.Log.e(nfe)
//                    }
//                }
//                mProxyConfig!!.done()
//
//                mProxyConfig!!.edit()
//                var identity = mProxyConfig!!.identityAddress
//                if (identity != null) {
//                    identity.domain = preferences.getString(SERVER_IP_KEY, "")
//                } else {
//                    var server = preferences.getString(SERVER_IP_KEY, "")
//                    identity = Factory.instance().createAddress(server!!)
//
//                    if (identity != null) {
//                        identity.domain = preferences.getString(SERVER_IP_KEY, "")
//                    }
//                }
//                mProxyConfig!!.identityAddress = identity
//                mProxyConfig!!.done()
//
//                mProxyConfig!!.edit()
//                var identityUser = mProxyConfig!!.identityAddress
//                if (identityUser != null) {
//                    identityUser.username = preferences.getString(USER_NAME_KEY, "")
//                } else {
//
//                    var server = preferences.getString(SERVER_IP_KEY, "")
//                    identityUser = Factory.instance().createAddress(server!!)
//
//                    if (identityUser != null) {
//                        identityUser.username = preferences.getString(USER_NAME_KEY, "")
//                    }
//                }
//
//                mProxyConfig!!.identityAddress = identityUser
//                mProxyConfig!!.done()
//
//                core.addAuthInfo(mAuthInfo!!)
//                core.addProxyConfig(mProxyConfig!!)
//
//                core.enableEchoCancellation(true)
//
//                core.enableIpv6(false)
//                core.enableKeepAlive(true)
//
//                if (LinphoneContext.isReady()) {
//                    Log.i("FirebaseMessaging", "[Push Notification] Starting context")
//                    LinphoneContext(applicationContext)
//                    // LinphoneContext.instance().start(false)
//                    core.ensureRegistered()
//                    core.addListener(mListener)
//                } else {
//                    Log.i("[Push Notification]", "Notifying Core")
//                    if (LinphoneManager.getInstance() != null) {
//                        val core = LinphoneManager.getCore()
//                        core?.ensureRegistered()
//                        core?.start()
//                    }
//                }
//            } else {
//                org.linphone.core.tools.Log.e("[Account Settings] No proxy config !")
//            }
//        }
//    }

    fun checkCodec() {

        /* Fix LinphonePreferences.instance().setEchoCancellation(true)

        val lc = LinphoneManager.getCore()

        if (lc != null) {
            for (pt in lc.audioPayloadTypes) {
                if (pt.mimeType.equals("PCMU")) {
                    Log.e("Mime", "name: " + pt.mimeType + "; enabled")
                    pt.enable(true)
                } else if (pt.mimeType.equals("PCMA")) {
                    Log.e("Mime", "name: " + pt.mimeType + "; enabled")
                    pt.enable(true)
                } else if (pt.mimeType.equals("G729")) {
                    Log.e("Mime", "name: " + pt.mimeType + "; enabled")
                    pt.enable(true)
                } else {
                    Log.e("Mime", "name: " + pt.mimeType + "; disabled")
                    pt.enable(false)
                }
            }
        } */
    }

    fun saveCreatedAccount(userid: String, password: String, domain: String) {

        // username = LinphoneUtils.getDisplayableUsernameFromAddress(username);
        // domain = LinphoneUtils.getDisplayableUsernameFromAddress(domain);

        // String identity = "sip:" + username + "@" + domain;
        // address = Factory.instance().createAddress(identity);

        Log.e("OTP", "yes 1")
        val preferences = LinphoneApplication.instance.getSharedPreferences()
//        val builder = LinphonePreferences.AccountBuilder(LinphoneManager.getCore())
//                .setUsername(preferences.getString("username", ""))
//                .setDomain(preferences.getString("serverIP", ""))
//                .setUserid(preferences.getString("username", ""))
//                .setDisplayName(preferences.getString("displayName", ""))
//                .setPassword(preferences.getString("password", ""))
// //        "extension": "1000101",
// //        "secret": "w(p+RR8U",
// //        "server_ip": "119.40.81.104",
//        Log.e("OTP", "yes 2")
//
//        builder.setTransport(TransportType.Udp)
        Log.e("OTP", "yes 3")

//        try {
//            Log.e("OTP", "yes 4")
//            builder.saveNewAccount()
//            //            if (!newAccount) {
//            //                displayRegistrationInProgressDialog();
//            //            }
//            //            accountCreated = true;
//        } catch (e: CoreException) {
//            Log.e("OTP", "yes 5")
//            org.linphone.mediastream.Log.e(e)
//            Log.e("OTP", "reg problem")
//        }

        Log.e("OTP", "yes 6")
    }

    private fun addRegistrationCallback() {
/*
        val registrationCallback = object : RegistrationCallback() {
            override fun registrationNone() {
                Log.d(TAG, "registrationNone")
            }

            override fun registrationProgress() {
                Log.d(TAG, "registrationProgress")
            }

            override fun registrationOk() {
                Log.d(TAG, "BottomTabsActivity registrationOk")
                online = true
                updateMenu()
            }

            override fun registrationCleared() {
                Log.d(TAG, "BottomTabsActivity registrationFailed")
                online = false
                updateMenu()
            }

            override fun registrationFailed() {
                Log.d(TAG, "BottomTabsActivity registrationFailed")
                online = false
                updateMenu()
            }
        } as RegistrationCallback

        EasyLinphone.addCallback(registrationCallback, null as PhoneCallback?)

        */
    }

    override fun onResume() {
        super.onResume()
        backButton.visibility = if (AppUtils.isConference) View.VISIBLE else View.GONE
        val core = coreContext.core
        core.addListener(listener)

        val preferences = LinphoneApplication.instance.getSharedPreferences()
        val companySecretRequest = CompanySecretRequest(preferences.getString(USER_ID_KEY, "")
            ?: "", API_KEY,
            preferences.getString(MOBILE_NUMBER_KEY, "")
                ?: "", preferences.getString(COMPANY_ID_KEY, "") ?: "")
        getCompanySecret(companySecretRequest)

        refreshRegister()

        var state: RegistrationState = RegistrationState.None
        val defaultAccount = core.defaultAccount
        if (defaultAccount != null) {
            state = defaultAccount.state
        }

        updateDefaultAccountRegistrationStatus(state)

//        val account = coreContext.core.defaultAccount
//        val params = account?.params?.clone()
//        params?.useInternationalPrefixForCallsAndChats = true
//        params?.internationalPrefix = "880"
//        account?.params = params!!

        val path = "file:///android_asset/toy_mono.wav"

        // LinphoneApplication.coreContext.core.ring = path
        // refreshRegister()

        /* register new push message receiver
        by doing this, the activity will
        be notified each time a new message arrives*/
//        LocalBroadcastManager.getInstance(this).registerReceiver(pushNotificationReceiver,
//                IntentFilter(PUSH_NOTIFICATION_RECEIVER))

        /* Fix mPrefs = LinphonePreferences.instance()
        mPrefs.incTimeout = 60

        LinphoneContext.instance()
                .notificationManager
                .removeForegroundServiceNotificationIfPossible()

        val preferences = LinphoneApplication.instance.getSharedPreferences()

        var preferenceEditor = preferences.edit()
        preferenceEditor.putBoolean("isAppInBackground", false)
        preferenceEditor.apply()

//        val lc = LinphoneManager.getCore()
//        if (lc != null) {
//            Log.e("OTP", "lc not null")
//            lc.addListener(mListener)
//        } else {
//            Log.e("OTP", "lc null")
//        }

        val core = LinphoneManager.getCore()
        core?.addListener(mListener)

        if (LinphoneService.isReady()) {
            Log.e("OTP", "yes... service ready")
        }

        if (LinphoneManager.getCore().calls.size > 0) {
            val call = LinphoneManager.getCore().calls[0]
            val onCallStateChanged = call.state

            if (onCallStateChanged == Call.State.IncomingReceived) {
                Log.e("Call", "Incoming Call into onResume")
                val number = call.remoteAddress.username ?: return
//                if(!LinphoneService.checkBackOrFore) {
                CallActivity.startIncomingCall(this, number)
//                }
                // startActivity(new Intent(this, CallIncomingActivity.class));
            } else if (onCallStateChanged == Call.State.OutgoingInit || onCallStateChanged == Call.State.OutgoingProgress || onCallStateChanged == Call.State.OutgoingRinging) {
                // startActivity(new Intent(this, CallOutgoingActivity.class));
            } else {
                // startIncallActivity(call);
            }
        }

        if (shouldCallRefreshInFirstTime) {
            shouldCallRefreshInFirstTime = false

            Handler().postDelayed({
                /* Fix if (LinphoneService.isReady()) run {
                    LinphoneManager.getCore().refreshRegisters()
                }

                val lc2 = LinphoneManager.getCore()
                if (lc2 != null) {
                    Log.e("OTP", "lc not null222")
                    lc2.addListener(mListener)
                } */
            }, 2000)
        } */
    }

//    protected override fun onNewIntent(intent: Intent) {
//        super.onNewIntent(intent)
//        if (LinphoneManager.getLc().calls.size > 0) {
//            // If a call is ringing, start incomingcallactivity
//            val incoming = ArrayList<Call.State>()
//            incoming.add(Call.State.IncomingReceived)
//            if (LinphoneUtils.getCallsInState(LinphoneManager.getLc(), incoming).size > 0) {
//                Log.e("Call", "Incoming Call into onNewIntent")
// //                if (CallActivity.isInstanciated()) {
// //                    // CallActivity.instance().startIncomingCallActivity();
// //                } else {
// //                    //startActivity(new Intent(this, CallIncomingActivity.class));
// //                }
//            }
//        }
//    }

    override fun onPause() {
        // Fix val lc = LinphoneManager.getCore()
        // Fix lc?.removeListener(mListener)
        coreContext.core.removeListener(listener)
        super.onPause()
        // LinphoneService.removeRegistrationCallback()
        // LocalBroadcastManager.getInstance(this).unregisterReceiver(pushNotificationReceiver)
        AppUtils.isConference = false
        backButton.visibility = if (AppUtils.isConference) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        // LinphoneService.removeRegistrationCallback()
        unregisterReceiver(receiver)
        started = false
        progress = null
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)

//        val keepAliveIntent = Intent(this, KeepAliveReceiver::class.java)
//        val keepAlivePendingIntent = PendingIntent.getBroadcast(this, 0, keepAliveIntent, PendingIntent.FLAG_ONE_SHOT)
//        val alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//        Compatibility.scheduleAlarm(alarmManager, AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 2000, keepAlivePendingIntent)
    }

    override fun onBackPressed() {
        if (fragNavController.isRootFragment) {
            fragNavController.clearStack()
            // super.onBackPressed()
            exit()
        } else if (fragNavController.popFragment().not()) {
            // super.onBackPressed()
            exit()
        }
    }

    private fun exit() {
        val builder = AlertDialog.Builder(this@BottomTabsActivity)

        // Set the alert dialog title
        builder.setTitle("EXIT")
        builder.setCancelable(false)
        builder.setMessage("Are you sure?")
        builder.setPositiveButton("Yes") { dialog, which ->

//            val intent = Intent()
//            intent.putExtra("Exit", true)
//            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        builder.setNegativeButton("No") { dialog, which ->
            // Toast.makeText(applicationContext,"You are not agree.",Toast.LENGTH_SHORT).show()
        }

        val b = builder.create()
        b.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        fragNavController.onSaveInstanceState(outState)
    }

    override fun pushFragment(fragment: Fragment) {
        fragNavController.pushFragment(fragment)
    }

    override fun popFragment() {
        fragNavController.popFragment()
    }

    override fun onTabTransaction(fragment: Fragment?, index: Int) {
        // If we have a backstack, show the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(fragNavController.isRootFragment.not())
    }

    override fun onFragmentTransaction(
        fragment: Fragment?,
        transactionType: FragNavController.TransactionType
    ) {
        // do fragmentty stuff. Maybe change title, I'm not going to tell you how to live your life
        // If we have a backstack, show the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(fragNavController.isRootFragment.not())
    }

    override fun getRootFragment(index: Int): Fragment {
        contactsFragment = ContactsFragment.newInstance(0)
        when (index) {
            INDEX_CONTACTS -> return contactsFragment!!
            INDEX_DIALER -> return DialpadFragment.newInstance(0)
            INDEX_CALL_HISTORY -> return DialCallHistoryFragment.newInstance(0)
            INDEX_CONFERENCE -> return ConferenceFragment.newInstance(0)
            INDEX_SETTINGS -> return SettingsFragment.newInstance(0)
        }
        throw IllegalStateException("Need to send an index that we know")
    }

    private inner class NetworkChangeReceiver : BaseReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            // EasyLinphone.addCallback(registrationCallback, null)
        }
    }

    private fun updateMenu() {
        if (menu == null) {
            return
        }
        val menuItem = menu!!.findItem(R.id.menuNetworkStatus)
        val rootView = menuItem.actionView as LinearLayout
        val ivStatus = rootView.findViewById(R.id.ivStatus) as ImageView
        val tvStatus = rootView.findViewById(R.id.tvStatus) as TextView
        ivStatus.setImageResource(registrationStatusDrawable)
        tvStatus.setText(registrationStatusText)
//        if (online) {
            // ivStatus.setImageResource(R.drawable.ic_connection_found)
//            tvStatus.setText(R.string.connection_on)
//        } else {
//            ivStatus.setImageResource(R.drawable.ic_connection_not_found)
//            tvStatus.setText(R.string.connection_off)
//        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.connection_status, menu)
        updateMenu()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> fragNavController.popFragment()
            R.id.menuNetworkStatus -> refreshRegister()
        }
        return false
    }

    companion object {

        var isAppWentToBg = false

        var navigatedFromPushNotification = false

        private val TAG2 = "EasyLinphone"

        var started = false
        private val TAG = BottomTabsActivity::class.java.simpleName
        private const val INDEX_CONTACTS = FragNavController.TAB1
        private const val INDEX_DIALER = FragNavController.TAB2
        const val INDEX_CALL_HISTORY = FragNavController.TAB3
        private const val INDEX_CONFERENCE = FragNavController.TAB4
        private const val INDEX_SETTINGS = FragNavController.TAB5

        fun start(context: Context) {
            start(context, INDEX_DIALER)
        }

        fun start(context: Context, index: Int) {
            if (started) {
                return
            }
            started = true
            val intent = Intent(context, BottomTabsActivity::class.java)
            intent.putExtra("selectionIndex", index)
            context.startActivity(intent)
        }
    }

//
//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
//        if (permissions.size <= 0) {
//            return
//        }
//
//        if (requestCode == PERMISSIONS_READ_EXTERNAL_STORAGE_DEVICE_RINGTONE) {
//            if (permissions[0].compareTo(Manifest.permission.READ_EXTERNAL_STORAGE) != 0) {
//
//            }
//            val enableRingtone = grantResults[0] == PackageManager.PERMISSION_GRANTED
//            LinphonePreferences.instance().enableDeviceRingtone(enableRingtone)
//            LinphoneManager.getInstance().enableDeviceRingtone(enableRingtone)
//
//            checkAndRequestReadContactPermission()
//        }
//
//        if (requestCode == PERMISSIONS_READ_CONTACT) {
//            if (!grantResults.isEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                contactsFragment!!.reloadContacts()
//            } else {
//                // Not granted
//            }
//
//        }
//
//
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//    }

    // This will be needed for geeting the uri data from recycler call history adapter
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == REQUEST_CONTACT_FOR_ADD_TO_EXISTING) {

            val contactUri = data!!.data
            Log.d("", "contactUri data : " + contactUri.toString())

            val idx: Int

            // var n = intent.extras.getString("number")
//            var c = data.getStringExtra("number")
//            Log.d("====> NNN", c)

//            var e: String = data.getStringExtra("number")
//            Log.d("","e="+e)

            // var phoneNum: String = edtPhoneNo.text.toString()
//            val extras = intent.getExtras()
//            //Log.d("==> Extras :", extras.getString("number"))
//
//            if (extras != null) {
//                var number: Parcelable = extras.getParcelable("number")
//                Log.d("=====> N EXTRAS :", number.toString())
//
//                //val number = data.getStringExtra("number")
//
//
//                val cursor = contentResolver.query(contactUri, null, null, null, null);
//                if (cursor!!.moveToFirst()) {
//
//                    idx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
//                    val _id = cursor.getString(idx).toLong()
//
//                    //Log.d("ID : =======>", _id)
//
//                    val editIntent = Intent(Intent.ACTION_EDIT)
//                    editIntent.setData(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, _id))
//                    editIntent.putExtra("finishActivityOnSaveCompleted", true)
//                    editIntent.putExtra(ContactsContract.Intents.Insert.PHONE, number)
//                    editIntent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_WORK)
//                    startActivity(editIntent)
//
//                }
//
//            }else{
//                Toast.makeText(applicationContext, "Failed to get extras", Toast.LENGTH_SHORT).show()
//            }
//
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStart() {
        // Log.d(TAG, "onStart isAppWentToBg $isAppWentToBg")
//
//        applicationWillEnterForeground()

        super.onStart()
        isAppWentToBg = false

//        Toast.makeText(applicationContext, isAppWentToBg.toString(),
//                Toast.LENGTH_SHORT).show()

//        if (!LinphoneService.checkIncomingForBatch) {
//            ShortcutBadger.removeCount(applicationContext)
//            LinphoneManager.getCore()!!.resetMissedCallsCount()
//        }
    }

    override fun onStop() {
        super.onStop()
        isAppWentToBg = true

        val preferences = LinphoneApplication.instance.getSharedPreferences()

        val preferenceEditor = preferences.edit()
        preferenceEditor.putBoolean(APP_BACKGROUND_KEY, true)
        preferenceEditor.apply()

//        Toast.makeText(applicationContext, isAppWentToBg.toString(),
//                Toast.LENGTH_SHORT).show()
//        Log.d(TAG, "onStop ")
//        applicationdidenterbackground()
    }

//    private fun prepareBroadcastReceiverForPushNotification() {
//        pushNotificationReceiver = object : BroadcastReceiver() {
//            override fun onReceive(context: Context, intent: Intent) {
//                if (intent.action != null && intent.action == PUSH_NOTIFICATION_RECEIVER) {
//                    // new push notification is received
//                    val message = intent.getStringExtra("notification")
//                    if (message == null || message.isEmpty()) return
//
//                    CoroutineScope(Dispatchers.Main.immediate).launch {
//                        val phoneNo = "01648301499"
//
//                        LinphoneApplication.instance.getDatabaseHelper().insertCallHistory(phoneNo, "", 1)
//
//                        //val context = LinphoneContext.instance().applicationContext
//                        CallActivity.start(this@BottomTabsActivity, "Test Call", phoneNo, incoming = true)
//                    }
//
//                    try {
// //                        val jsonObject: JsonObject = JsonParser.parseString(message).getAsJsonObject()
// //                        val title = if (jsonObject["title"].isJsonNull) "" else jsonObject["title"].asString
// //                        val body = if (jsonObject["body"].isJsonNull) "" else jsonObject["body"].asString
// //                        val imageUrl = if (jsonObject["thumbnail"].isJsonNull) "" else jsonObject["thumbnail"].asString
// //                        val time = if (jsonObject["time"].isJsonNull) 0 else jsonObject["time"].asLong
// //                        val data = if (jsonObject["data"].isJsonNull) JsonObject() else jsonObject["data"].asJsonObject
// //                        var action_type: String? = ""
// //                        var action_type_id: String? = ""
// //                        var action_type_link: String? = ""
// //                        if (data.size() > 0) {
// //                            action_type = if (data.has("action_type") && !data["action_type"].isJsonNull) data["action_type"].asString else ""
// //                            action_type_link = if (data.has("action_type_link") && !data["action_type"].isJsonNull) data["action_type_link"].asString else ""
// //                            action_type_id = if (data.has("action_type_id") && !data["action_type"].isJsonNull) data["action_type_id"].asString else ""
// //                        }
// //                        val resultIntent = Intent("kotha_dialer_push_notification")
// //                        val bundle = Bundle()
// //                        bundle.putString("title", title)
// //                        bundle.putString("content", body)
// //                        bundle.putString("action_type", action_type)
// //                        bundle.putString("action_type_link", action_type_link)
// //                        bundle.putString("action_type_id", action_type_id)
// //                        bundle.putString("image", imageUrl)
// //                        resultIntent.putExtras(bundle)
// //                        resultIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
// //                        val requestCode: Int = AtomicNumberGenerator.getUniqueNumber()
// //                        val resultPendingIntent = PendingIntent.getActivity(applicationContext, requestCode, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)
// //                        if (imageUrl != null && imageUrl.isNotEmpty()) {
// //                            NotificationUtils.showBigImageNotification(applicationContext, title, body, time, imageUrl, resultPendingIntent)
// //                        } else {
// //                            NotificationUtils.showBigTextNotification(applicationContext, title, body, time, resultPendingIntent)
// //                        }
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                    }
//                }
//            }
//        }
//
//        /* register new push message receiver
//        by doing this, the activity will
//        be notified each time a new message arrives*/LocalBroadcastManager.getInstance(this).registerReceiver(pushNotificationReceiver,
//                IntentFilter(PUSH_NOTIFICATION_RECEIVER))
//    }
}
