package com.bdcom.appdialer.fragments

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.bdcom.appdialer.R
import com.bdcom.appdialer.utils.Progress
// import com.xuchongyang.easyphone.EasyLinphone
// import com.xuchongyang.easyphone.callback.RegistrationCallback
// import com.xuchongyang.easyphone.service.LinphoneService
import android.widget.*
import com.bdcom.appdialer.models.CommonApiResponse
import com.bdcom.appdialer.network_utils.APIClient
import com.bdcom.appdialer.network_utils.APIInterface
import com.bdcom.appdialer.utils.Constants
import com.bdcom.appdialer.utils.Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.appcompat.app.AlertDialog
import com.bdcom.appdialer.LinphoneApplication
import com.bdcom.appdialer.utils.Constants.DISPLAY_NAME_KEY
import com.bdcom.appdialer.utils.Constants.LOGIN_KEY
import com.bdcom.appdialer.utils.Constants.MOBILE_NUMBER_KEY
import com.bdcom.appdialer.utils.Constants.PASSWORD_KEY
import com.bdcom.appdialer.utils.Constants.SERVER_IP_KEY
import com.bdcom.appdialer.utils.Constants.USER_NAME_KEY
import com.makeramen.roundedimageview.RoundedImageView
import java.io.*

class AccountSettingsFragment : BaseFragment() {

    private lateinit var btnSave: Button

    private lateinit var btnChangeAvatar: Button

    private lateinit var edtUsername: TextView

    private lateinit var edtPassword: EditText

    private lateinit var edtServerIP: EditText

    private lateinit var avatarImage: RoundedImageView

    private var progress: Progress? = null

    private val REQUEST_CAMERA = 0
    private val SELECT_FILE = 1

    private val TAKE_PHOTO_TAG = "Take new photo"
    private val SELECT_FROM_GALLERY_TAG = "Select from gallery"
    private var userChoosenTask: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account_settings, container, false)

        edtUsername = view.findViewById(R.id.userid)
        edtPassword = view.findViewById(R.id.edtPassword)
        edtServerIP = view.findViewById(R.id.edtServerIP)
        btnSave = view.findViewById(R.id.btnSave)
        btnChangeAvatar = view.findViewById(R.id.btn_change_avatar)
        avatarImage = view.findViewById(R.id.ivAccountHolder)

        val preferences = LinphoneApplication.instance.getSharedPreferences()

        edtUsername.setText(preferences.getString(USER_NAME_KEY, ""))
        edtPassword.setText(preferences.getString(PASSWORD_KEY, ""))
        edtServerIP.setText(preferences.getString(DISPLAY_NAME_KEY, ""))

        if (preferences.getString("profileImagePath", null) != null) {
            showAvatarImageFromPath(preferences.getString("profileImagePath", null) ?: "")
        }

        btnSave.setOnClickListener {

            val username = edtUsername.text.toString()
            val password = edtPassword.text.toString()
            val serverIP = edtServerIP.text.toString()

            if (serverIP.isNullOrEmpty()) {
                Toast.makeText(LinphoneApplication.getApplicationContext(), R.string.login_fail_message, Toast.LENGTH_SHORT).show()
               // btnSave.isClickable = true
            } else {
                callUpdateUserApi()
               // btnSave.isClickable = false
//                showProgressDialog()
//                logout()
//                addRegistrationCallback()
//                startLinphoneServiceIfNotReady()
//                EasyLinphone.setAccount(username, password, serverIP)
//                EasyLinphone.login()
            }
        }

        btnChangeAvatar.setOnClickListener {
            selectImage()
        }
        return view
    }

    private fun logout() {
        SettingsFragment.logout()
    }

    private fun onRegistrationSuccess() {
        val preferences = LinphoneApplication.instance.getSharedPreferences()
        if (preferences.getBoolean(LOGIN_KEY, false)) {
            return
        }
        dismissProgressDialog()
        LinphoneApplication.SERVER_IP = edtServerIP.text.toString()
        val preferenceEditor = preferences.edit()
        preferenceEditor.putString(USER_NAME_KEY, edtUsername.text.toString())
        preferenceEditor.putString(PASSWORD_KEY, edtPassword.text.toString())
        preferenceEditor.putString(SERVER_IP_KEY, edtServerIP.text.toString())
        preferenceEditor.putBoolean(LOGIN_KEY, true)
        preferenceEditor.apply()

        Toast.makeText(LinphoneApplication.getApplicationContext(), R.string.account_info_saved_success_message, Toast.LENGTH_SHORT).show()
    }

    private fun showProgressDialog() {
        if (progress == null) {
            progress = Progress(context)
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

    override fun onDestroy() {
        dismissProgressDialog()
        progress = null
        super.onDestroy()
      //  LinphoneService.removeRegistrationCallback()
    }

    companion object {

        fun newInstance(instance: Int): AccountSettingsFragment {
            val args = Bundle()
            args.putInt(BaseFragment.ARGS_INSTANCE, instance)
            val fragment = AccountSettingsFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private fun callUpdateUserApi() {

        val preferences = LinphoneApplication.instance.getSharedPreferences()
        val mobileNo = preferences.getString(MOBILE_NUMBER_KEY, "")
        val userSecret = preferences.getString(PASSWORD_KEY, "")
        val clientName = edtServerIP.text.toString()

        if (clientName == null || clientName.isEmpty()) {
            Toast.makeText(LinphoneApplication.getApplicationContext(), "Please Enter Name", Toast.LENGTH_SHORT).show()
            return
        }

        showProgressDialog()
        val apiService = APIClient.getClient().create(APIInterface::class.java)
        apiService.updateUserInfo(Constants.API_KEY, mobileNo, userSecret, clientName)?.enqueue(object : Callback<CommonApiResponse?> {
            override fun onResponse(
                call: Call<CommonApiResponse?>,
                response: Response<CommonApiResponse?>
            ) {
                dismissProgressDialog()
                if (response.isSuccessful && response.body() != null) {
                    val updateUserResponse = response.body()

//                    if (updateUserResponse.statuscode.equals("200OK", ignoreCase = true)) {
//                        Toast.makeText(LinphoneApplication.getApplicationContext(), "Successfully Updated", Toast.LENGTH_SHORT).show()
//
//                        val preferenceEditor = preferences.edit()
//                        preferenceEditor.putString("displayName", clientName)
//                        preferenceEditor.apply()
//                    } else {
//                        val message = updateUserResponse.message
//                        Toast.makeText(LinphoneApplication.getApplicationContext(), message, Toast.LENGTH_SHORT).show()
//
//                    }
                } else {
                    Toast.makeText(LinphoneApplication.getApplicationContext(), "Something went wrong! Please try again!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CommonApiResponse?>, t: Throwable) {
                dismissProgressDialog()
                Log.e("Update User", "Failure On API: " + t.localizedMessage)
            }
        })
    }

    // -------------------------------------------------------
    // Image functionality
    // -------------------------------------------------------

    private fun galleryIntent() {
        val i = Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(i, SELECT_FILE)
    }

    private fun cameraIntent() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra("android.intent.extras.CAMERA_FACING", 1)
        startActivityForResult(intent, REQUEST_CAMERA)
    }

    private fun selectImage() {
        val items = arrayOf<CharSequence>(TAKE_PHOTO_TAG, SELECT_FROM_GALLERY_TAG, "Cancel")
        val builder = AlertDialog.Builder(requireContext())
        // builder.setTitle("Add Photo");
        builder.setItems(items, DialogInterface.OnClickListener { dialog, item ->
            val result = Utils.checkPermission(requireContext())
            if (items[item] == TAKE_PHOTO_TAG) {
                userChoosenTask = TAKE_PHOTO_TAG
                if (result)
                    cameraIntent()
            } else if (items[item] == SELECT_FROM_GALLERY_TAG) {
                userChoosenTask = SELECT_FROM_GALLERY_TAG
                if (result)
                    galleryIntent()
            } else if (items[item] == "Cancel") {
                dialog.dismiss()
            }
        })
        builder.show()
    }

    private fun saveImagePath(path: String) {
        val preferences = LinphoneApplication.instance.getSharedPreferences()
        val preferenceEditor = preferences.edit()
        preferenceEditor.putString("profileImagePath", path)
        preferenceEditor.apply()
    }

    private fun getImagePath(): String {
        var cw = ContextWrapper(requireContext())
        var directory = cw.getDir("imageDir", Context.MODE_PRIVATE)
        var file = File(directory, System.currentTimeMillis().toString() + ".png")
        // return file.absolutePath
        return directory.absolutePath
    }

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
            avatarImage.setImageBitmap(myBitmap) // rotating bitmap
        } catch (e: Exception) {
            Log.e("Gallery", "E: " + e.localizedMessage)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE) {
                onSelectFromGalleryResult(data)
                // onCaptureImageResult(data)
            } else if (requestCode == REQUEST_CAMERA) {
                onCaptureImageResult(data)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun onCaptureImageResult(data: Intent?) {

        if (data != null) {
            try {
                val thumbnail = data.extras!!.get("data") as Bitmap

                val picturePath = saveImageToExternalStorage(thumbnail)
                showAvatarImageFromPath(picturePath)
                saveImagePath(picturePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
        }
    }

    // Method to save an image to external storage
    private fun saveImageToExternalStorage(bitmap: Bitmap): String {
        var cw = ContextWrapper(requireContext())
        var directory = cw.getDir("imageDir", Context.MODE_PRIVATE)
        var file = File(directory, System.currentTimeMillis().toString() + ".jpg")

        try {
            // Get the file output stream
            val stream: OutputStream = FileOutputStream(file)

            // Compress the bitmap
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)

            // Flush the output stream
            stream.flush()

            // Close the output stream
            stream.close()
            // toast("Image saved successful.")
        } catch (e: IOException) { // Catch the exception
            e.printStackTrace()
            // toast("Error to save image.")
        }

        // Return the saved image path to uri
        // return Uri.parse(file.absolutePath)
        return file.absolutePath
    }

    private fun onSelectFromGalleryResult(data: Intent?) {
        if (data != null) {

            val imageUri = data.data ?: return
            val inputStream: InputStream?

            try {
                inputStream = requireContext().contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                // avatarImage.setImageBitmap(bitmap)

                val picturePath = saveImageToExternalStorage(bitmap)
                showAvatarImageFromPath(picturePath)
                saveImagePath(picturePath)
            } catch (e: Exception) {
            }

            /*
            try {
                val selectedImage = data.data
                val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                val cursor = activity!!.getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null)
                cursor.moveToFirst()
                val columnIndex = cursor.getColumnIndex(filePathColumn[0])
                val picturePath = cursor.getString(columnIndex)
                cursor.close()
                showAvatarImageFromPath(picturePath)
                saveImagePath(picturePath)

            } catch (e: Exception) {
                e.printStackTrace()
            }
            */
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        try {
            when (requestCode) {
                Utils.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (userChoosenTask == TAKE_PHOTO_TAG)
                        cameraIntent()
                    else if (userChoosenTask == SELECT_FROM_GALLERY_TAG)
                        galleryIntent()
                } else {
                    // code for deny
                }
            }
        } catch (asdsa: Exception) {
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
