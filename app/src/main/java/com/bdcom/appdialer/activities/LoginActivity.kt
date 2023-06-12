package com.bdcom.appdialer.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bdcom.appdialer.LinphoneApplication.Companion.instance
import com.bdcom.appdialer.R
import com.bdcom.appdialer.models.FCMTokenRegisterRequest
import com.bdcom.appdialer.models.LoginDataResponse
import com.bdcom.appdialer.models.LoginRequest
import com.bdcom.appdialer.network_utils.ApiHelper
import com.bdcom.appdialer.utils.AppUtils
import com.bdcom.appdialer.utils.Constants.API_KEY
import com.bdcom.appdialer.utils.Constants.COMPANY_ID_KEY
import com.bdcom.appdialer.utils.Constants.DISPLAY_NAME_KEY
import com.bdcom.appdialer.utils.Constants.FCM_KEY
import com.bdcom.appdialer.utils.Constants.LAT_KEY
import com.bdcom.appdialer.utils.Constants.LOGIN_KEY
import com.bdcom.appdialer.utils.Constants.LONG_KEY
import com.bdcom.appdialer.utils.Constants.MOBILE_NUMBER_KEY
import com.bdcom.appdialer.utils.Constants.PASSWORD_KEY
import com.bdcom.appdialer.utils.Constants.PLATFORM
import com.bdcom.appdialer.utils.Constants.SERVER_IP_KEY
import com.bdcom.appdialer.utils.Constants.USER_ID_KEY
import com.bdcom.appdialer.utils.Constants.USER_NAME_KEY
import com.bdcom.appdialer.utils.Progress
import com.bdcom.appdialer.utils.showSafely
import com.google.android.gms.location.*
import java.util.*
import org.linphone.core.*

class LoginActivity : AppCompatActivity(), View.OnFocusChangeListener {

    private lateinit var txtCompany: EditText

    private lateinit var txtUser: EditText
    private lateinit var txtPassword: EditText
    private lateinit var txtDomain: EditText

    private lateinit var btnLogin: Button
    private lateinit var btnRegistration: Button

    private var progress: Progress? = null

    private lateinit var company: String
    private lateinit var mobileno: String
    private lateinit var password: String
    private var PERMISSION_COARSE_LOCATION = 254

    private var location: Location? = null

    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var isLocationPermissionGranted: Boolean = false
    private var isPhoneStatePermissionGranted: Boolean = false
    private var locationManager: LocationManager? = null

    private var mProxyConfig: ProxyConfig? = null
    private var mAuthInfo: AuthInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // this.supportActionBar!!.hide()

//        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
//            if (!task.isSuccessful) {
//                Log.w("FirebaseID Error:", "Fetching FCM registration token failed", task.exception)
//                return@OnCompleteListener
//            }
//
//            // Get new FCM registration token
//            val token = task.result
//
//            Log.e("FirebaseID  ", token, task.exception)
//
//            // Log and toast
//            Toast.makeText(baseContext, token, Toast.LENGTH_SHORT).show()
//        })

        progress = Progress(this)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val fullScreen = WindowManager.LayoutParams.FLAG_FULLSCREEN
        window.setFlags(fullScreen, fullScreen)

//        try {
//            var intent = Intent(this, ForegroundService::class.java)
//            intent.action = Constants.ACTION.STOPFOREGROUND_ACTION
//            startService(intent)
//        } catch (e: Exception) {
//
//        }

//        AppMain.instance.getSharedPreferences().edit().clear().commit()

//        Log.d("OTP", "onCreate: we are in LoginActivity")
//
//        if (LinphoneService.isReady()) {
//            Log.e("OTP", "Service ready")
//          //  onServiceReady()
//        } else {
//            Log.e("OTP", "Service not ready")
//            val intent = Intent(Intent.ACTION_MAIN)
//            intent.setClass(this, LinphoneService::class.java)
//            this.startService(intent)
//            // start linphone as background
//            startService(Intent(ACTION_MAIN).setClass(this, LinphoneService::class.java!!))
//            if (LinphoneService.isReady()) {
//
//            }
//           ServiceWaitThread().start()
//            //mServiceThread.start()
//        }

        val preferences = instance.getSharedPreferences()
        if (preferences.getBoolean(LOGIN_KEY, false)) {
            Log.e("Login", "Found login")
            startBottomTabActivity()
            return
        } else {
            try {
                // LinphoneManager.getCore().clearProxyConfig()
                // LinphoneManager.getCore().refreshRegisters()
            } catch (e: Exception) {
            }
            Log.e("Login", "Found not login")
        }

//        if (!LinphoneService.isReady()) run {
//            EasyLinphone.startService(AppMain.getApplicationContext())
//            EasyLinphone.addCallback(object : RegistrationCallback() {
//                override fun registrationOk() {
//                    super.registrationOk()
//                    Log.e("LoginActivity", "registrationOk: ")
//                    startBottomTabActivity()
//                }
//
//                override fun registrationFailed() {
//                    super.registrationFailed()
//                    Log.e("LoginActivity", "registrationFailed: ")
//                }
//            }, null)
//        } else {
//            startBottomTabActivity()
//        }

        setContentView(R.layout.activity_login)

        // txtUserid = findViewById(R.id.userid)
        txtCompany = findViewById(R.id.company)

        txtUser = findViewById(R.id.user)
        txtPassword = findViewById(R.id.password)
        txtDomain = findViewById(R.id.domain)

        btnLogin = findViewById(R.id.btnLogin)
        btnRegistration = findViewById(R.id.btn_registration)

        // txtMobileno.setCompoundDrawables(TextDrawable(txtMobileno, "+88"), null, null, null);

        // txtUserid.onFocusChangeListener = this
        txtCompany.onFocusChangeListener = this

        txtUser.onFocusChangeListener = this
        txtPassword.onFocusChangeListener = this
        txtDomain.onFocusChangeListener = this

        btnRegistration.setOnClickListener {
            startActivity(Intent(this, RegistrationActivity::class.java))
        }

        btnLogin.setOnClickListener {

            when {
//                txtCompany.text.isEmpty() -> {
//                    Toast.makeText(applicationContext, "Company id is not found", Toast.LENGTH_SHORT).show()
//                }
                txtUser.text.isEmpty() -> {
                    Toast.makeText(applicationContext, "User number not found!", Toast.LENGTH_SHORT).show()
                }
//                txtUser.text.length < 11 -> {
//                    Toast.makeText(applicationContext, "Phone number must contain 11 characters", Toast.LENGTH_SHORT).show()
//                }
                txtPassword.text.isEmpty() -> {
                    Toast.makeText(applicationContext, "Password not found!", Toast.LENGTH_SHORT).show()
                }
                txtUser.text.isEmpty() -> {
                    Toast.makeText(applicationContext, "Domain not found!", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // Log.d("===> Number: ", phoneNo)
                    when {
                        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), REQUEST_READ_PHONE_STATE)
                            }
                        }
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_COARSE_LOCATION)
                            }
                        }
                        else -> {
                            requestForLocation()
                        }
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_READ_PHONE_STATE) {
            isPhoneStatePermissionGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else if (requestCode == PERMISSION_COARSE_LOCATION) {
            isLocationPermissionGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        }

        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), REQUEST_READ_PHONE_STATE)
                }
            }
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_COARSE_LOCATION)
                }
            }
            isLocationPermissionGranted && isPhoneStatePermissionGranted -> {
                requestForLocation()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestForLocation() {
        val request = LocationRequest.create()
        request.interval = 1000
        request.fastestInterval = 500

        mFusedLocationClient?.requestLocationUpdates(request, object : LocationCallback() {
            override fun onLocationAvailability(locationAvailability: LocationAvailability?) {
                super.onLocationAvailability(locationAvailability)
                if (locationAvailability != null && locationAvailability.isLocationAvailable) {
                    try {
                        location = mFusedLocationClient?.lastLocation?.result
                        mFusedLocationClient?.removeLocationUpdates(this)
                        callRegistrationApi()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    checkLocationEnableStatus()
                }
            }

            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                location = locationResult?.lastLocation
                mFusedLocationClient!!.removeLocationUpdates(this)
                callRegistrationApi()
            }
        }, Looper.myLooper())
    }

    private fun checkLocationEnableStatus() {
        locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager?
        var gps_enabled = false
        var network_enabled = false

        if (locationManager == null) {
            Toast.makeText(this, "Location is not available yet. Please try again.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            gps_enabled = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        try {
            network_enabled = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        if (!gps_enabled && !network_enabled) {
            showDialog()
        } else {
            getLastLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        mFusedLocationClient!!.lastLocation.addOnSuccessListener { location: Location? ->
            if (location == null) {
                requestForLocation()
            } else {
                this.location = location
                callRegistrationApi()
            }
        }
    }

    private fun showDialog() {
        try {
            val dialogBuilder = AlertDialog.Builder(this)
            dialogBuilder.setCancelable(false)
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val dialogView = inflater.inflate(R.layout.layout_location_dialog, null)
            dialogBuilder.setView(dialogView)

            val alertDialog = dialogBuilder.create()
            alertDialog.show()

            val dialogMessageTextView = dialogView.findViewById<View>(R.id.dialog_message_textview) as TextView

            dialogMessageTextView.text = "Gps is not enable. Press ok to enable GPS."

            val dialogDissmissBtn = dialogView.findViewById<View>(R.id.dialog_dismiss_btn) as Button
            val dialogOkBtn = dialogView.findViewById<View>(R.id.dialog_ok_btn) as Button

            dialogDissmissBtn.setOnClickListener {
                alertDialog.dismiss()
            }

            dialogOkBtn.setOnClickListener {
                try {
                    alertDialog.dismiss()
                    val callGPSSettingIntent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(callGPSSettingIntent)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    fun callRegistrationApi() {
        if (location == null) {
            Toast.makeText(this@LoginActivity, "Enable GPS and give location permission then try again.", Toast.LENGTH_LONG).show()
            return
        }

        val deviceId = AppUtils.getDeviceUniqueID(this)
        progress.showSafely()
        val preferences = instance.getSharedPreferences()
        val preferenceEditor = preferences.edit()
        preferenceEditor.putString(LAT_KEY, location?.latitude?.toString() ?: "")
        preferenceEditor.putString(LONG_KEY, location?.longitude?.toString() ?: "")
        preferenceEditor.apply()

//        val registerRequest = LoginRequest(
//            apikey = API_KEY, userid = USER_ID, company = txtCompany.text.toString(), mobileno = txtMobileno.text.toString(), password = txtPassword.text.toString(), email = "", imei = deviceId,
//            lat = location?.latitude?.toString() ?: "", long = location?.longitude?.toString()
//                ?: "", platform = PLATFORM
//        )

        doLogIn(txtUser.text.toString(), txtPassword.text.toString(), txtDomain.text.toString())

//        val registerRequest = LoginRequest(
//            apikey = API_KEY, userid = USER_ID, company = "", mobileno = txtUser.text.toString(), password = txtPassword.text.toString(), email = "", imei = deviceId,
//            lat = location?.latitude?.toString() ?: "", long = location?.longitude?.toString()
//                ?: "", platform = PLATFORM
//        )
//        registerUser(registerRequest)
    }

    private fun registerUser(registerRequest: LoginRequest) {
        ApiHelper.login(registerRequest,
                object : ApiHelper.ApiCallbackListener<LoginDataResponse> {
                    override fun onFailed(status: Int?) {
                        progress?.dismiss()
                    }

                    override fun onDataFetched(response: LoginDataResponse?) {
                        progress?.dismiss()
                        // startOtpActivity(response, deviceId, iccid)
                        if (response?.secret != null && response.extension != null) {
                            onLoginSuccess(response, registerRequest.userid, registerRequest.mobileno)

                            // finish()
                        } else {
                            Toast.makeText(applicationContext, "Invalid Login", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
    }

    //    private final void onLoginSuccess() {
    //        hideProgress();
    //        SharedPreferences preferences = AppMain.Companion.getInstance().getSharedPreferences();
    //        if (preferences.getBoolean("loggedIn", false)) {
    //            return;
    //        }
    //
    //        SharedPreferences.Editor preferenceEditor = preferences.edit();
    //        preferenceEditor.putString("username", mUserName);
    //        preferenceEditor.putString("password", mPassword);
    //        preferenceEditor.putString("serverIP", AppMain.Companion.getSERVER_IP());
    //        preferenceEditor.putString("displayName", mDisplayName);
    //        preferenceEditor.putString("mobileNumber", mMobileNumber);
    //        preferenceEditor.putBoolean("loggedIn", true);
    //        preferenceEditor.apply();
    //
    //        Log.d("====> Server IP :", AppMain.Companion.getSERVER_IP());
    //
    //        BottomTabsActivity.Companion.start(OTPActivity.this);
    //        finishActivity();
    //
    //
    //        saveCreatedAccount(mUserName, mPassword, AppMain.Companion.getSERVER_IP());
    //
    // //        EasyLinphone.setAccount(mUserName, mPassword, AppMain.Companion.getSERVER_IP());
    // //        EasyLinphone.login();
    //
    // //        Intent intent = new Intent(OTPActivity.this, BottomTabsActivity.class);
    // //        startActivity(intent);
    //
    // //        BottomTabsActivity.Companion.start(this);
    // //        finishActivity();
    //    }

    private fun doLogIn(user: String, password: String, domain: String) {
        val preferences = instance.getSharedPreferences()
        val preferenceEditor = preferences.edit()
        preferenceEditor.putString(USER_NAME_KEY, user)
        preferenceEditor.putString(PASSWORD_KEY, password)
        preferenceEditor.putString(SERVER_IP_KEY, domain)
        preferenceEditor.putString(DISPLAY_NAME_KEY, "")
        preferenceEditor.putString(MOBILE_NUMBER_KEY, "")
        preferenceEditor.putString(USER_ID_KEY, "")

        preferenceEditor.putString(COMPANY_ID_KEY, "")
        preferenceEditor.putBoolean(LOGIN_KEY, true)
        preferenceEditor.apply()

        val fcmTokenRegisterRequest = FCMTokenRegisterRequest(preferences.getString(USER_ID_KEY, "")
            ?: "", API_KEY,
            preferences.getString(MOBILE_NUMBER_KEY, "")
                ?: "", preferences.getString(COMPANY_ID_KEY, "") ?: "",
            preferences.getString(FCM_KEY, "") ?: "", AppUtils.getDeviceUniqueID(this),
            preferences.getString(LAT_KEY, "") ?: "",
            preferences.getString(LONG_KEY, "") ?: "", PLATFORM)
        AppUtils.registerFirebaseToken(fcmTokenRegisterRequest)
        startBottomTabActivity()
    }

    private fun onLoginSuccess(
        serveData: LoginDataResponse,
        userID: String,
        mobileNo: String?
    ) {
        // hideProgress()
        val preferences = instance.getSharedPreferences()
        val preferenceEditor = preferences.edit()
        preferenceEditor.putString(USER_NAME_KEY, serveData.extension)
        preferenceEditor.putString(PASSWORD_KEY, serveData.secret)
        preferenceEditor.putString(SERVER_IP_KEY, serveData.server_ip)
        preferenceEditor.putString(DISPLAY_NAME_KEY, serveData.name)
        preferenceEditor.putString(MOBILE_NUMBER_KEY, mobileNo)
        preferenceEditor.putString(USER_ID_KEY, userID)

        preferenceEditor.putString(COMPANY_ID_KEY, serveData.company_uniqueid)
        preferenceEditor.putBoolean(LOGIN_KEY, true)
        preferenceEditor.apply()

        val fcmTokenRegisterRequest = FCMTokenRegisterRequest(preferences.getString(USER_ID_KEY, "")
            ?: "", API_KEY,
            preferences.getString(MOBILE_NUMBER_KEY, "")
                ?: "", preferences.getString(COMPANY_ID_KEY, "") ?: "",
            preferences.getString(FCM_KEY, "") ?: "", AppUtils.getDeviceUniqueID(this),
            preferences.getString(LAT_KEY, "") ?: "",
            preferences.getString(LONG_KEY, "") ?: "", PLATFORM)
        AppUtils.registerFirebaseToken(fcmTokenRegisterRequest)
        startBottomTabActivity()
       // configureAccount()

        // Need to fix LinphonePreferences.instance().firstLaunchSuccessful()

        // startActivity(Intent(this, BottomTabsActivity::class.java))
        // finishActivity()
    }

    private val coreListener = object : CoreListenerStub() {
        override fun onRegistrationStateChanged(
            core: Core,
            cfg: ProxyConfig,
            state: RegistrationState,
            message: String
        ) {
//            if (cfg == proxyConfigToCheck) {
//                org.linphone.core.tools.Log.i("[Assistant] [Account Login] Registration state is $state: $message")
//                if (state == RegistrationState.Ok) {
//                    // waitForServerAnswer.value = false
//                    // leaveAssistantEvent.value = Event(true)
//                    core.removeListener(this)
//                    val intent = Intent()
//                    intent.setClass(this@LoginActivity, BottomTabsActivity::class.java)
//
//                    // Propagate current intent action, type and data
//                    if (getIntent() != null) {
//                        val extras = getIntent().extras
//                        if (extras != null) intent.putExtras(extras)
//                    }
//                    intent.action = getIntent().action
//                    intent.type = getIntent().type
//                    intent.data = getIntent().data
//                    startActivity(intent)
//                    finishActivity()
//                } else if (state == RegistrationState.Failed) {
//                    // waitForServerAnswer.value = false
//                    // invalidCredentialsEvent.value = Event(true)
//                    core.removeListener(this)
//                    val intent = Intent()
//                    intent.setClass(this@LoginActivity, BottomTabsActivity::class.java)
//
//                    // Propagate current intent action, type and data
//                    if (getIntent() != null) {
//                        val extras = getIntent().extras
//                        if (extras != null) intent.putExtras(extras)
//                    }
//                    intent.action = getIntent().action
//                    intent.type = getIntent().type
//                    intent.data = getIntent().data
//                    startActivity(intent)
//                    finishActivity()
//                }
//            }
        }
    }

    private fun startBottomTabActivity() {
        // BottomTabsActivity.start(this)
        val intent = Intent()
        intent.setClass(this@LoginActivity, BottomTabsActivity::class.java)

        // Propagate current intent action, type and data
        if (getIntent() != null) {
            val extras = getIntent().extras
            if (extras != null) intent.putExtras(extras)
        }
        intent.action = getIntent().action
        intent.type = getIntent().type
        intent.data = getIntent().data
        startActivity(intent)
        finishActivity()
    }

    private fun finishActivity() {
        this.finish()
    }

    override fun onFocusChange(view: View, hasFocus: Boolean) {
        if (!hasFocus) {
            hideKeyboard(this)
        }
    }

    override fun onDestroy() {
        progress?.dismiss()
        progress = null
        super.onDestroy()
      //  LinphoneService.removeRegistrationCallback()
    }

    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        // Find the currently focused view, so we can grab the correct window token from it.
        var view = activity.currentFocus
        // If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    companion object {
        private const val REQUEST_READ_PHONE_STATE = 555
    }
}
