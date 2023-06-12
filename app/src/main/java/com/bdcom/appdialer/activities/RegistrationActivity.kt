package com.bdcom.appdialer.activities

// import com.xuchongyang.easyphone.EasyLinphone
// import com.xuchongyang.easyphone.callback.RegistrationCallback
// import com.xuchongyang.easyphone.service.LinphoneService
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.bdcom.appdialer.LinphoneApplication.Companion.instance
import com.bdcom.appdialer.R
import com.bdcom.appdialer.models.*
import com.bdcom.appdialer.network_utils.ApiHelper
import com.bdcom.appdialer.utils.AndroidUtil
import com.bdcom.appdialer.utils.AppUtils
import com.bdcom.appdialer.utils.Constants.API_KEY
import com.bdcom.appdialer.utils.Constants.DISPLAY_NAME_KEY
import com.bdcom.appdialer.utils.Constants.FCM_KEY
import com.bdcom.appdialer.utils.Constants.LOGIN_KEY
import com.bdcom.appdialer.utils.Constants.MOBILE_NUMBER_KEY
import com.bdcom.appdialer.utils.Constants.PASSWORD_KEY
import com.bdcom.appdialer.utils.Constants.PLATFORM
import com.bdcom.appdialer.utils.Constants.SERVER_IP_KEY
import com.bdcom.appdialer.utils.Constants.USER_ID
import com.bdcom.appdialer.utils.Constants.USER_NAME_KEY
import com.bdcom.appdialer.utils.Progress
import com.google.android.gms.location.*
import java.util.*

class RegistrationActivity : AppCompatActivity(), View.OnFocusChangeListener {

    // private lateinit var txtUserid: EditText

    private lateinit var txtName: EditText

    private lateinit var txtCompany: EditText

    private lateinit var txtMobileno: EditText

    private lateinit var txtPassword: EditText

    private lateinit var txtRetypePassword: EditText
    private lateinit var txtEmail: EditText
    private lateinit var btnRegistration: Button

    private var progress: Progress? = null

    private var PERMISSION_COARSE_LOCATION = 254

    private var location: Location? = null

    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var isLocationPermissionGranted: Boolean = false
    private var isPhoneStatePermissionGranted: Boolean = false
    private var locationManager: LocationManager? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // this.supportActionBar!!.hide()

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContentView(R.layout.activity_registration)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Get a support ActionBar corresponding to this toolbar
        val ab: ActionBar? = supportActionBar
        // Enable the Up button
        ab?.setDisplayHomeAsUpEnabled(true)
        ab?.setDisplayShowHomeEnabled(true)

       // txtUserid = findViewById(R.id.userid)
        txtName = findViewById(R.id.name)
        txtCompany = findViewById(R.id.company)
        txtMobileno = findViewById(R.id.mobileno)
        txtPassword = findViewById(R.id.password)
        txtRetypePassword = findViewById(R.id.retypepassword)
        txtEmail = findViewById(R.id.email)
        // btnLogin = findViewById(R.id.btnLogin)
        btnRegistration = findViewById(R.id.btnLogin)

       // txtMobileno.setCompoundDrawables(TextDrawable(txtMobileno, "+88"), null, null, null);

        // txtUserid.onFocusChangeListener = this
        txtCompany.onFocusChangeListener = this
        txtMobileno.onFocusChangeListener = this
        txtPassword.onFocusChangeListener = this

        btnRegistration.setOnClickListener {

//            if (txtUserid.text.isNullOrEmpty()) {
//                Toast.makeText(applicationContext, R.string.login_fail_message, Toast.LENGTH_SHORT).show()
//
//            }else
//
            when {
                txtName.text.isEmpty() -> {
                    Toast.makeText(applicationContext, "Name not found", Toast.LENGTH_SHORT).show()
                }
                txtCompany.text.isEmpty() -> {
                    Toast.makeText(applicationContext, "Company id not found", Toast.LENGTH_SHORT).show()
                }
                txtMobileno.text.isEmpty() -> {
                    Toast.makeText(applicationContext, "Phone number is not fount", Toast.LENGTH_SHORT).show()
                }
                txtMobileno.text.length < 11 -> {
                    Toast.makeText(applicationContext, "Phone number must contain 11 characters", Toast.LENGTH_SHORT).show()
                }
                txtPassword.text.isEmpty() -> {
                    Toast.makeText(applicationContext, "Please enter password", Toast.LENGTH_SHORT).show()
                }
                txtRetypePassword.text.isEmpty() -> {
                    Toast.makeText(applicationContext, "Please re-type password", Toast.LENGTH_SHORT).show()
                }
                txtPassword.text.toString() != txtRetypePassword.text.toString() -> {
                    Toast.makeText(applicationContext, "Both password does not match", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    when {
                        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED -> {
                            requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), REQUEST_READ_PHONE_STATE)
                        }
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED -> {
                            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_COARSE_LOCATION)
                        }
                        else -> {
                            requestForLocation()
                            // txtMobileno.getText().toString()
                        }
                    }
                }
            }
        }

//        btnRegistration.setOnClickListener {
//            val intent = Intent(this, RegistrationActivity::class.java)
//            startActivity(intent)
//        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
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
                // txtMobileno.getText().toString()
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
            Toast.makeText(this@RegistrationActivity, "Location is not available yet. Please try again.", Toast.LENGTH_LONG).show()
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

//    private fun callRegistrationApi(mobileNumber: String) {
//        showProgressDialog()
//        val apiService = APIClient.getClient().create(APIInterface::class.java)
//        apiService.userLogin(Constants.API_KEY, mobileNumber).enqueue(object : Callback<RegistrationResponse> {
//            override fun onResponse(call: Call<RegistrationResponse>, response: Response<RegistrationResponse>) {
//                dismissProgressDialog()
//                if (response.isSuccessful && response.body() != null) {
//                    val registrationResponse = response.body()
//
//                    if (registrationResponse.statuscode.equals("200OK", ignoreCase = true)) {
//                        val intent = Intent(this@LoginActivity, OTPActivity::class.java)
//                        intent.putExtra(Constants.MOBILE_NUMBER_KEY, mobileNumber)
//                        startActivity(intent)
//                        finishActivity()
//                    } else {
//                        val message = registrationResponse.message
//                        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
//
//                    }
//                } else {
//                    Toast.makeText(applicationContext, "Something went wrong! Please try again!", Toast.LENGTH_SHORT).show()
//                }
//            }
//
//            override fun onFailure(call: Call<RegistrationResponse>, t: Throwable) {
//                dismissProgressDialog()
//                Log.e("ApI Failure ===>", "Failure On API: " + t.localizedMessage)
//            }
//        })
//    }

    @SuppressLint("MissingPermission")
    fun callRegistrationApi() {
        if (location == null) {
            Toast.makeText(this, "Enable GPS and give location permission then try again.", Toast.LENGTH_LONG).show()
            return
        }

        val deviceId = AppUtils.getDeviceUniqueID(this)

        // var msisdn: String = e

        // if (iccid != null && !iccid.isEmpty() && iccid != "null") {
        showProgressDialog()
            val registerRequest = RegisterRequest(
                    apikey = API_KEY, name = txtName.text.toString(), userid = USER_ID, company = txtCompany.text.toString(),
                    mobileno = txtMobileno.text.toString(), password = txtPassword.text.toString(), email = txtEmail.text.toString(), imei = deviceId,
                    lat = location?.latitude?.toString() ?: "", long = location?.longitude?.toString() ?: "", platform = PLATFORM
            )
            registerUser(registerRequest)
//        } else {
//            Toast.makeText(applicationContext, "No sim card found", Toast.LENGTH_SHORT).show()
//            iccid = "12344"
//            dismissProgressDialog()
//            return
//        }
    }

    private fun registerUser(registerRequest: RegisterRequest) {
        ApiHelper.registerUser(registerRequest,
                object : ApiHelper.ApiCallbackListener<RegistrationDataResponse> {
                    override fun onFailed(status: Int?) {
                        dismissProgressDialog()
                        Toast.makeText(applicationContext, "Registration not successful!", Toast.LENGTH_SHORT).show()
                    }

                    override fun onDataFetched(response: RegistrationDataResponse?) {
                        dismissProgressDialog()
                        // startOtpActivity(response, deviceId, iccid)
                        // onLoginSuccess(response)

                        val preferences = instance.getSharedPreferences()
                        val fcmTokenRegisterRequest = FCMTokenRegisterRequest(USER_ID, API_KEY,
                                registerRequest.mobileno, registerRequest.company,
                                preferences.getString(FCM_KEY, "") ?: "",
                                AppUtils.getDeviceUniqueID(this@RegistrationActivity),
                                registerRequest.lat,
                                registerRequest.long, PLATFORM)
                        AppUtils.registerFirebaseToken(fcmTokenRegisterRequest)

                        Toast.makeText(applicationContext, "Registration Successful!", Toast.LENGTH_SHORT).show()
                        finish()
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
    private fun onLoginSuccess(serveData: LoginDataResponse) {
        // hideProgress()
        val preferences = instance.getSharedPreferences()
        if (preferences.getBoolean(LOGIN_KEY, false)) {
           // return
        }
        val preferenceEditor = preferences.edit()
        preferenceEditor.putString(USER_NAME_KEY, serveData.extension)
        preferenceEditor.putString(PASSWORD_KEY, serveData.secret)
        preferenceEditor.putString(SERVER_IP_KEY, serveData.server_ip)
        preferenceEditor.putString(DISPLAY_NAME_KEY, serveData.name)
        preferenceEditor.putString(MOBILE_NUMBER_KEY, serveData.extension)
        preferenceEditor.putBoolean(LOGIN_KEY, true)
        preferenceEditor.apply()
        startActivity(Intent(this, BottomTabsActivity::class.java))
        finishActivity()
        saveCreatedAccount(serveData.extension, serveData.secret, serveData.server_ip)

//        EasyLinphone.setAccount(mUserName, mPassword, AppMain.Companion.getSERVER_IP());
//        EasyLinphone.login();

//        Intent intent = new Intent(OTPActivity.this, BottomTabsActivity.class);
//        startActivity(intent);
//        BottomTabsActivity.Companion.start(this);
//        finishActivity();
    }

    fun saveCreatedAccount(userid: String?, password: String?, domain: String?) {

        // username = LinphoneUtils.getDisplayableUsernameFromAddress(username);
        // domain = LinphoneUtils.getDisplayableUsernameFromAddress(domain);

        // String identity = "sip:" + username + "@" + domain;
        // address = Factory.instance().createAddress(identity);

        // Need_to_fix_start
//        Log.e("OTP", "yes 1")
//        val builder = AccountBuilder(LinphoneManager.getLc())
//                .setUsername(userid)
//                .setDomain(domain)
//                .setUserid(userid)
//                .setDisplayName(userid)
//                .setPassword(password)
//        Log.e("OTP", "yes 2")
//        builder.setTransport(TransportType.Udp)
//        Log.e("OTP", "yes 3")
//        try {
//            Log.e("OTP", "yes 4")
//            builder.saveNewAccount()
//            //            if (!newAccount) {
// //                displayRegistrationInProgressDialog();
// //            }
// //            accountCreated = true;
//        } catch (e: CoreException) {
//            Log.e("OTP", "yes 5")
//            org.linphone.mediastream.Log.e(e)
//            Log.e("OTP", "reg problem")
//        }
//        Log.e("OTP", "yes 6")
        // Need_to_fix_end
    }
    private fun startOtpActivity(
        registerSuccessData: RegisterSuccessData,
        deviceId: String,
        iccid: String
    ) {
//        val intent = Intent(this@LoginActivity, OTPActivity::class.java)
//        intent.putExtra(Constants.OTP, registerSuccessData.otp)
//        intent.putExtra(Constants.EXPIRATION_DATE, registerSuccessData.expiration_date)
//        intent.putExtra(Constants.SECRET, registerSuccessData.secret)
//        //intent.putExtra(Constants.MSISDN, phoneNo)
//        intent.putExtra("deviceid", deviceId)
//        intent.putExtra("iccid", iccid)
//        startActivity(intent)
    }

    private fun finishActivity() {
        this.finish()
    }

    private fun startBottomTabActivity() {
        BottomTabsActivity.start(this)
        finishActivity()
    }

    override fun onFocusChange(view: View, hasFocus: Boolean) {
        if (!hasFocus) {
            AndroidUtil.hideKeyboard(this)
        }
    }

    override fun onDestroy() {
        dismissProgressDialog()
        progress = null
        super.onDestroy()
      //  LinphoneService.removeRegistrationCallback()
    }

    private fun showProgressDialog() {
        if (progress == null) {
            progress = Progress(this)
        }
        progress!!.show()
    }

    private fun dismissProgressDialog() {
        if (progress != null) {
            try {
                progress!!.dismiss()
            } catch (e: Exception) {
            }
        }
    }

    companion object {
        private const val REQUEST_READ_PHONE_STATE = 555
    }
}
