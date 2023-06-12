package com.bdcom.appdialer.fragments

import android.app.Activity
import android.app.AlertDialog
import android.app.Instrumentation
import android.content.ContentUris
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import com.bdcom.appdialer.LinphoneApplication
import com.bdcom.appdialer.R
import com.bdcom.appdialer.models.CallHistory
import com.bdcom.appdialer.utils.AppUtils

class DialpadFragment : BaseFragment(), View.OnClickListener, View.OnLongClickListener {

    private lateinit var edtPhoneNo: EditText
    private lateinit var btnADDphoneNo: LinearLayout
    private lateinit var bottomContactForm: FrameLayout
    private lateinit var ll: LinearLayout
    private lateinit var cancelBtn: TextView
    private lateinit var addToContacts: TextView
    private lateinit var addToExisting: TextView

    private val REQUEST_CONTACT = 201
    private lateinit var slide_up: Animation
    // private lateinit var slide_down: Animation
    val preferences = LinphoneApplication.instance.getSharedPreferences()
    private val list: ArrayList<CallHistory> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        setHasOptionsMenu(true)
        val view = inflater.inflate(R.layout.fragment_dialpad, container, false)

        // slide_down = AnimationUtils.loadAnimation(context, R.anim.slide_down)
        slide_up = AnimationUtils.loadAnimation(context, R.anim.slide_up)

        edtPhoneNo = view.findViewById(R.id.editText)
        btnADDphoneNo = view.findViewById(R.id.btnAdd_layout)
        bottomContactForm = view.findViewById(R.id.bottom_contact_form)
        cancelBtn = view.findViewById(R.id.cancel_btn)
        addToContacts = view.findViewById(R.id.addTocontacts)
        addToExisting = view.findViewById(R.id.addToexisting)
        ll = view.findViewById(R.id.ll)
        btnADDphoneNo.setOnClickListener(this)
        cancelBtn.setOnClickListener(this)
        addToContacts.setOnClickListener(this)
        addToExisting.setOnClickListener(this)

        edtPhoneNo = view.findViewById(R.id.editText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            edtPhoneNo.showSoftInputOnFocus = false
        }

        edtPhoneNo.requestFocus()

        // askPermissions()

        val btnAsterisk = view.findViewById<Button>(R.id.btnAterisk)
        btnAsterisk.setOnClickListener(this)
        btnAsterisk.setOnLongClickListener(this)

        val btnHash = view.findViewById<Button>(R.id.btnHash)
        btnHash.setOnClickListener(this)

        val btnZero = view.findViewById<Button>(R.id.btnZero)
        btnZero.setOnClickListener(this)

        val btnOne = view.findViewById<Button>(R.id.btnOne)
        btnOne.setOnClickListener(this)

        val btnTwo = view.findViewById<Button>(R.id.btnTwo)
        btnTwo.setOnClickListener(this)

        val btnThree = view.findViewById<Button>(R.id.btnThree)
        btnThree.setOnClickListener(this)

        val btnFour = view.findViewById<Button>(R.id.btnFour)
        btnFour.setOnClickListener(this)

        val btnFive = view.findViewById<Button>(R.id.btnFive)
        btnFive.setOnClickListener(this)

        val btnSix = view.findViewById<Button>(R.id.btnSix)
        btnSix.setOnClickListener(this)

        val btnSeven = view.findViewById<Button>(R.id.btnSeven)
        btnSeven.setOnClickListener(this)

        val btnEight = view.findViewById<Button>(R.id.btnEight)
        btnEight.setOnClickListener(this)

        val btnNine = view.findViewById<Button>(R.id.btnNine)
        btnNine.setOnClickListener(this)

        val btnDelete = view.findViewById<Button>(R.id.btnDelete)
        btnDelete.setOnClickListener(this)
        btnDelete.setOnLongClickListener(this)

        val btnCall = view.findViewById<ImageButton>(R.id.btnCall)
        btnCall.setOnClickListener(this)

//        edtPhoneNo.addTextChangedListener(object : TextWatcher{
//            override fun afterTextChanged(s: Editable?) {

//            }
//
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

//            }
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

//            }
//
//        })

        return view
    }

    companion object {

        fun newInstance(instance: Int): DialpadFragment {
            val args = Bundle()
            args.putInt(BaseFragment.ARGS_INSTANCE, instance)
            val fragment = DialpadFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onClick(view: View) {
        var phoneNo: String = edtPhoneNo.text.toString()

        when (view.id) {
            R.id.btnAterisk -> {
                phoneNo += "+"
                pressAKey(KeyEvent.KEYCODE_PLUS)
            }
            R.id.btnHash -> {
                phoneNo += "#"
                pressAKey(KeyEvent.KEYCODE_POUND)
            }
            R.id.btnZero -> {
                phoneNo += "0"
                pressAKey(KeyEvent.KEYCODE_0)
            }
            R.id.btnOne -> {
                phoneNo += "1"
                pressAKey(KeyEvent.KEYCODE_1)
            }
            R.id.btnTwo -> {
                phoneNo += "2"
                pressAKey(KeyEvent.KEYCODE_2)
            }
            R.id.btnThree -> {
                phoneNo += "3"
                pressAKey(KeyEvent.KEYCODE_3)
            }
            R.id.btnFour -> {
                phoneNo += "4"
                pressAKey(KeyEvent.KEYCODE_4)
            }
            R.id.btnFive -> {
                phoneNo += "5"
                pressAKey(KeyEvent.KEYCODE_5)
            }
            R.id.btnSix -> {
                phoneNo += "6"
                pressAKey(KeyEvent.KEYCODE_6)
            }
            R.id.btnSeven -> {
                phoneNo += "7"
                pressAKey(KeyEvent.KEYCODE_7)
            }
            R.id.btnEight -> {
                phoneNo += "8"
                pressAKey(KeyEvent.KEYCODE_8)
            }
            R.id.btnNine -> {
                phoneNo += "9"
                pressAKey(KeyEvent.KEYCODE_9)
            }
            R.id.btnDelete -> {
//                if (phoneNo.isNotBlank()) {
//                    phoneNo = phoneNo.subSequence(0, phoneNo.length - 1).toString()
//                }
//                edtPhoneNo.setText(phoneNo)
                pressAKey(KeyEvent.KEYCODE_DEL)
            }

            R.id.addToexisting -> {

                val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                startActivityForResult(intent, REQUEST_CONTACT)
                bottomContactForm.visibility = View.INVISIBLE
            }

            R.id.addTocontacts -> {
                bottomContactForm.visibility = View.INVISIBLE

                val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                    type = ContactsContract.RawContacts.CONTENT_TYPE

                    putExtra(ContactsContract.Intents.Insert.PHONE, phoneNo)
                    // putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_WORK)
                }
                startActivity(intent)
            }

            R.id.btnAdd_layout -> {
                if (!phoneNo.isNullOrBlank()) {
                    // slideUp(ll)
                    bottomContactForm.visibility = View.VISIBLE
                    ll.startAnimation(slide_up)
                } else {
                    Toast.makeText(LinphoneApplication.getApplicationContext(), R.string.dialpad_warning_message, Toast.LENGTH_SHORT).show()
                }
            }

            R.id.cancel_btn -> {
                // slideDown(bottomContactForm)
                bottomContactForm.visibility = View.INVISIBLE
            }

            R.id.btnCall -> if (phoneNo.isBlank()) {

                val number: String? = LinphoneApplication.instance.getDatabaseHelper().getLastCall()
                var filteredCallList: ArrayList<CallHistory> = ArrayList()
                if (list.isNotEmpty() && filteredCallList.isNotEmpty()) {
                    list.clear()
                    filteredCallList.clear()
                }
                LinphoneApplication.instance.getDatabaseHelper().readCallHistory(null, list)

                for (item in list) {
                    if (item.callStatus == 1) {
                        filteredCallList.add(item)
                    }
                }

//                if (number.isNullOrEmpty()) {
//                    Toast.makeText(LinphoneApplication.getApplicationContext(), R.string.dialpad_warning_message, Toast.LENGTH_SHORT).show()
//                } else {
//                    edtPhoneNo.setText(number)
//                }

                if (filteredCallList.isEmpty()) {
                    Toast.makeText(LinphoneApplication.getApplicationContext(), R.string.dialpad_warning_message, Toast.LENGTH_SHORT).show()
                } else {
                    edtPhoneNo.setText(filteredCallList.first().contactNumber)
                }
            } else if (phoneNo == preferences.getString("username", "")) {

                val builder = AlertDialog.Builder(context)

                // Set the alert dialog title
                builder.setTitle("Notice")
                builder.setCancelable(false)
                builder.setMessage("You cant call your own number")
                builder.setPositiveButton("Ok") { dialog, which ->
                }

//                    builder.setNegativeButton("No"){dialog,which ->
//                        //Toast.makeText(applicationContext,"You are not agree.",Toast.LENGTH_SHORT).show()
//                    }

                val b = builder.create()
                b.show()
            } else {

                LinphoneApplication.instance.getDatabaseHelper().insertCallHistory(phoneNo, "", 1)

                AppUtils.directCall(phoneNo)

                /* Fix val context = LinphoneContext.instance().applicationContext
                CallActivity.start(context!!, "", phoneNo) */

//                val intent = Intent(context, CallActivity::class)
//                // This flag is required to start an Activity from a Service context
//                // This flag is required to start an Activity from a Service context
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                context.startActivity(intent)
//                val intent = Intent(activity, Call2Activity::class.java)
//                intent.putExtra("number", phoneNo)
//                startActivity(intent)

              //  LinphoneManager.getInstance().newOutgoingCall("01710441906", "Mamun")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CONTACT && resultCode == Activity.RESULT_OK) {
            val contactUri = data!!.data ?: return
            Log.d("URI data : ", contactUri.toString())

            val idx: Int
            val phoneNum: String = edtPhoneNo.text.toString()

            val cursor = requireContext().contentResolver.query(contactUri, null, null, null, null)
            if (cursor!!.moveToFirst()) {

                idx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val _id = cursor.getString(idx).toLong()

                // Log.d("ID : =======>", _id)

                val editIntent = Intent(Intent.ACTION_EDIT)
                editIntent.setData(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, _id))
                editIntent.putExtra("finishActivityOnSaveCompleted", true)
                editIntent.putExtra(ContactsContract.Intents.Insert.PHONE, phoneNum)
                editIntent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_WORK)
                startActivity(editIntent)
            }
        }
    }

    private fun pressAKey(key: Int) {
        Thread {
            val instrumentation = Instrumentation()
            instrumentation.sendKeyDownUpSync(key)
        }.start()
    }

    private fun setCursorPosition() {
        edtPhoneNo.setSelection(edtPhoneNo.text.lastIndex + 1)
    }

    override fun onLongClick(view: View?): Boolean {

        var phoneNo: String = edtPhoneNo.text.toString()

        when (view?.id) {
            R.id.btnAterisk -> {
                phoneNo += "*"
//                edtPhoneNo.setText(phoneNo)
                pressAKey(KeyEvent.KEYCODE_STAR)
            }
            R.id.btnDelete -> {
                phoneNo
                edtPhoneNo.setText("")
            }
        }

        return true
    }
}
