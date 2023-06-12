package com.bdcom.appdialer.fragments

// import com.xuchongyang.easyphone.EasyLinphone
// import com.xuchongyang.easyphone.service.LinphoneService
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.bdcom.appdialer.BuildConfig
import com.bdcom.appdialer.LinphoneApplication
import com.bdcom.appdialer.R
import com.bdcom.appdialer.activities.LoginActivity
import com.bdcom.appdialer.adapters.SettingsViewAdapter
import com.bdcom.appdialer.dialog_fragment.RingtoneSelectionBottomSheetDialog
import com.bdcom.appdialer.models.RingtoneDataModel
import com.bdcom.appdialer.models.current_balance.CurrentBalanceResponse
import com.bdcom.appdialer.network_utils.APIClient
import com.bdcom.appdialer.network_utils.APIInterface
import com.bdcom.appdialer.utils.Constants
import com.bdcom.appdialer.utils.Constants.APP_EXIT_KEY
import com.bdcom.appdialer.utils.Constants.SERVICE_STATUS_KEY
import com.makeramen.roundedimageview.RoundedImageView
// import com.sslcommerz.library.payment.Classes.PayUsingSSLCommerz
// import com.sslcommerz.library.payment.Listener.OnPaymentResultListener
// import com.sslcommerz.library.payment.Util.ConstantData.CurrencyType
// import com.sslcommerz.library.payment.Util.ConstantData.ErrorKeys
// import com.sslcommerz.library.payment.Util.ConstantData.SdkCategory
// import com.sslcommerz.library.payment.Util.ConstantData.SdkType
// import com.sslcommerz.library.payment.Util.JsonModel.TransactionInfo
// import com.sslcommerz.library.payment.Util.Model.MandatoryFieldModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SettingsFragment : BaseFragment() {

    var settingsViewAdapter: SettingsViewAdapter? = null
    private lateinit var image: RoundedImageView
    private lateinit var ringtoneSelectionBottomSheetDialog: RingtoneSelectionBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        val name = view.findViewById(R.id.avatar_name) as TextView
        val number = view.findViewById(R.id.avatar_number) as TextView

        image = view.findViewById(R.id.avatar_img)

        ringtoneSelectionBottomSheetDialog = RingtoneSelectionBottomSheetDialog(object : RingtoneSelectionBottomSheetDialog.SocialMediaClickListener {
            override fun onRingtoneSelected(ringTone: RingtoneDataModel) {
            }
        })

        val preferences = LinphoneApplication.instance.getSharedPreferences()
        val usernumber = preferences.getString(Constants.USER_NAME_KEY, "")
        val userName = preferences.getString(Constants.DISPLAY_NAME_KEY, "")
        name.setText(userName)
        number.setText(usernumber)

        val listView = view.findViewById(R.id.settings) as ListView
        val accountSettings = getString(R.string.account_settings)
        val ringtoneSettings = getString(R.string.ringtone_settings)
        val contactUs = getString(R.string.contact_us)
        val tellToFriend = getString(R.string.tell_to_friend)
        val aboutVersion = "${getString(R.string.app_name)} V-${BuildConfig.VERSION_NAME}"
        // val balance = getString(R.string.balance)
        val logout = getString(R.string.log_out)
        val exit = getString(R.string.exit_app)

        val list = arrayListOf(getString(R.string.account_settings), ringtoneSettings, getString(R.string.contact_us),
                getString(R.string.tell_to_friend), aboutVersion, getString(R.string.log_out), getString(R.string.exit_app))

        settingsViewAdapter = SettingsViewAdapter(requireContext(), list)
        val viewedFiles = settingsViewAdapter
        listView.adapter = viewedFiles

        listView.setOnItemClickListener { adapterView, row, i, l ->

            when (list[i]) {
                accountSettings -> {
                    mFragmentNavigation.pushFragment(AccountSettingsFragment.newInstance(0))
                }
                ringtoneSettings -> {
                    mFragmentNavigation.pushFragment(RingtoneSettingsFragment.newInstance(0))
                    // ringtoneSelectionBottomSheetDialog.show(childFragmentManager, "#RingtoneSelectionBottomSheetDialog")
                }
                contactUs -> {
                    web_page_open("https://www.bdcom.com/pages/view/corporate-office")
                }
                tellToFriend -> {
                    shareWithFriends()
                }
                aboutVersion -> {
//                    startActivity(Intent(context, VersionActivity::class.java))
                    // Need_to_fix mFragmentNavigation.pushFragment(VersionFragment.newInstance(0))
                }
//                balance -> {
//                    // logout task
// //                    CallService.stop(LinphoneApplication.getApplicationContext())
// //                    logout()
// //                    startActivity(Intent(context, LoginActivity::class.java))
// //                    activity!!.finish()
//
//                    // startActivity(Intent(context, PaymentActivity::class.java))
//                    // callPayment()
//                }

                logout -> {
                    // logout task
                    // Need_to_fix CallService.stop(LinphoneApplication.getApplicationContext())
                    logout()
                    startActivity(Intent(context, LoginActivity::class.java))
                    requireActivity().finish()
                }

                exit -> {

                    // Need_to_fix LinphoneManager.getCore().clearProxyConfig()
                    // Need_to_fix LinphoneManager.getCore().refreshRegisters()
                    // Need_to_fix LinphoneService.instance().stopForegroundTest()
                    LinphoneApplication.coreContext.stop()
                    val preferences = LinphoneApplication.instance.getSharedPreferences()
                    val preferenceEditor = preferences.edit()
                    preferenceEditor.putBoolean(APP_EXIT_KEY, true)
                    preferenceEditor.apply()
                    requireActivity().finish()
                }
            }
        }

        // callBalanceApi()

        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        // callBalanceApi()
    }

    override fun onResume() {
        super.onResume()
        Log.e("Fragment", "Resumed called")
        // callBalanceApi()

// add by monir for settings page avatar image update
        val preferences = LinphoneApplication.instance.getSharedPreferences()
        if (preferences.getString("profileImagePath", null) != null) {
            showAvatarImageFromPath(preferences.getString("profileImagePath", null) ?: "")
        }
    }

    fun web_page_open(urls: String) { // for more than one url
        val uris = Uri.parse(urls)
        val intent = Intent(Intent.ACTION_VIEW, uris)
        // intent.data = Uri.parse("http://www.stackoverflow.com")

        // Always use string resources for UI text. This says something like "Share this photo with"
        val title = "Open Link With"
        // Create and start the chooser
        val chooser = Intent.createChooser(intent, title)

        // Try to invoke the intent.
        try {
            startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            // Define what your app should do if no activity can handle the intent.
            Toast.makeText(requireContext(), "No browser found to open the link!", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareWithFriends() {
        val msg = "My Application Link: http://www.google.com"
        val msgHtml = String.format("<p>My Application Link: " + "<a href=\"%s\"> link</a>!</p>", "http://www.google.com")
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Download Kotha Dialer Application")
        sharingIntent.putExtra(Intent.EXTRA_TEXT, msg)
        sharingIntent.putExtra(Intent.EXTRA_HTML_TEXT, msgHtml)
        startActivity(Intent.createChooser(sharingIntent, "Share Application Link"))
    }

//    private fun callPayment() {
//        val mandatoryFieldModel = MandatoryFieldModel("testbox", "qwerty", "10", "1012", CurrencyType.BDT, SdkType.TESTBOX, SdkCategory.BANK_LIST)
//
//        PayUsingSSLCommerz.getInstance().setData(context, mandatoryFieldModel, null, null, null, object :
//            OnPaymentResultListener {
//            override fun transactionSuccess(transactionInfo: TransactionInfo) {
//                if (transactionInfo.riskLevel == "0") {
//                    Log.d(TAG, "Transaction Successfully completed")
//                } else {
//                    Log.d(TAG, "Transaction in risk.")
//                }
//            }
//
//            override fun transactionFail(transactionInfo: TransactionInfo) {
//                Log.d(TAG, "Transaction Fail")
//            }
//
//            override fun error(i: Int) {
//                when (i) {
//                    ErrorKeys.USER_INPUT_ERROR -> Log.e(TAG, "User Input Error")
//                    ErrorKeys.INTERNET_CONNECTION_ERROR -> Log.e(TAG, "INTERNET_CONNECTION_ERROR")
//                    ErrorKeys.DATA_PARSING_ERROR -> Log.e(TAG, "DATA_PARSING_ERROR")
//                    ErrorKeys.CANCEL_TRANSACTION_ERROR -> Log.e(TAG, "CANCEL_TRANSACTION_ERROR")
//                    ErrorKeys.SERVER_ERROR -> Log.e(TAG, "SERVER_ERROR")
//                    ErrorKeys.NETWORK_ERROR -> Log.e(TAG, "NETWORK_ERROR")
//                }
//            }
//        })
//    }

    private fun callBalanceApi() {
        // showProgress()

        Log.e("Balance", "Balance API Called")

        val preferences = LinphoneApplication.instance.getSharedPreferences()
        val userId = preferences.getString(Constants.USER_NAME_KEY, "")
        val userSecret = preferences.getString(Constants.PASSWORD_KEY, "")

        val apiService = APIClient.getClient().create(APIInterface::class.java)

        apiService.getCurrentBalance(Constants.API_KEY, userId, userSecret)?.enqueue(object : Callback<CurrentBalanceResponse?> {
            override fun onResponse(
                call: Call<CurrentBalanceResponse?>,
                response: Response<CurrentBalanceResponse?>
            ) {
                // hideProgress()
                if (response.isSuccessful && response.body() != null) {
                    val registrationResponse = response.body()

//                    if (registrationResponse.statuscode.equals("200OK", ignoreCase = true)) {
//                        val data = registrationResponse.data
//                        val amount = data.currentBalance
//                        settingsViewAdapter?.setAmount(amount)
//
//                        Log.e("Balance", "Amount: " + amount)
//
//                    } else {
//                        val message = registrationResponse.message
//                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
//
//                    }
                } else {
                    Toast.makeText(context, "Something went wrong! Please try again!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CurrentBalanceResponse?>, t: Throwable) {
                // hideProgress()
                // Log.e(TAG, "Failure On API: " + t.localizedMessage)
            }
        })
    }

    companion object {

        private val TAG = "SettingsFragment"

        fun logout() {
            // EasyLinphone.logout()
            val preferences = LinphoneApplication.instance.getSharedPreferences()
            val preferenceEditor = preferences.edit()
            preferenceEditor.putBoolean(Constants.LOGIN_KEY, false)
            preferenceEditor.putBoolean(SERVICE_STATUS_KEY, false)
            preferenceEditor.putString(Constants.USER_NAME_KEY, "")
            preferenceEditor.putString(Constants.PASSWORD_KEY, "")
            preferenceEditor.putString(Constants.SERVER_IP_KEY, "")
            preferenceEditor.putString(Constants.DISPLAY_NAME_KEY, "")
            preferenceEditor.putString(Constants.MOBILE_NUMBER_KEY, "")
            preferenceEditor.apply()

            // LinphoneApplication.coreContext.core.removeAccount(LinphoneApplication.coreContext.core.accountList.first())
            LinphoneApplication.coreContext.stop()

            // Need_to_fix LinphoneService.instance().stopForegroundTest()
        }

        fun newInstance(instance: Int): SettingsFragment {
            val args = Bundle()
            args.putInt(ARGS_INSTANCE, instance)
            val fragment = SettingsFragment()
            fragment.arguments = args
            return fragment
        }
    }

    // add by monir for settings page avatar image update
    private fun showAvatarImageFromPath(path: String) {

        Log.e("Gallery", "Path: " + path)
        try {
            var myBitmap = BitmapFactory.decodeFile(path)
            val exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
            Log.d("EXIF", "Exif: $orientation")
            val matrix = Matrix()
            if (orientation == 6) {
                matrix.postRotate(90f)
            } else if (orientation == 3) {
                matrix.postRotate(180f)
            } else if (orientation == 8) {
                matrix.postRotate(270f)
            }
            myBitmap = Bitmap.createBitmap(myBitmap, 0, 0, myBitmap.getWidth(), myBitmap.getHeight(), matrix, true)
            image.setImageBitmap(myBitmap) // rotating bitmap
        } catch (e: Exception) {
            Log.e("Gallery", "E: " + e.localizedMessage)
        }
    }
}
