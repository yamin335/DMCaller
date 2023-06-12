package com.bdcom.appdialer.utils

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.ConnectivityManager
import android.net.Uri
import android.provider.ContactsContract
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.bdcom.appdialer.LinphoneApplication
import com.bdcom.appdialer.models.PhoneContact
import java.text.SimpleDateFormat
import java.util.*

class AndroidUtil {

    companion object {

        fun getRandomColor(): Int {
            return ColorGenerator.MATERIAL.randomColor
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

        fun getCurrentTime(): String {
            val date = Calendar.getInstance().time
            val dateFormat = SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.US)
            return dateFormat.format(date)
        }

        private fun contactExists(number: String, name: String, list: ArrayList<PhoneContact>): Boolean {
            for (i in list.indices) {
                val contact = list[i]
                val itemName = contact.name!!.trim()
                val itemNumber = contact.number!!.trim()
                if (number.trim() == itemNumber || name.trim() == itemName) {
                    return true
                }
            }
            return false
        }

        private fun contactExists(number: String, list: ArrayList<PhoneContact>): Boolean {
            for (i in list.indices) {
                val contact = list[i]
                val itemNumber = contact.number!!.trim()
                if (number.trim() == itemNumber) {
                    return true
                }
            }
            return false
        }

        fun readContacts(list: ArrayList<PhoneContact>, query: String, showAll: Boolean) {
            try {
                Log.i("Read contact", " Reading contact with query $query")
                val context = LinphoneApplication.getApplicationContext()
                val cursor: Cursor = if (query.isNotEmpty()) {
                    val filter = "%$query%"
                    context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + "  LIKE ? OR " +
                                    ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE ?",
                            arrayOf(filter, filter),
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")
                } else {
                    context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null, null, null,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")
                } ?: return

                while (cursor.moveToNext()) {
                    val phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    val id = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
//                    val image = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI))
                    val name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    if (showAll) {
                        if (contactExists(phoneNumber, list).not()) {
                            val contact = PhoneContact()
                            contact.name = name
                            contact.number = phoneNumber
                            contact.id = id
                            // contact.image = getPhotoUri(id)
                            list.add(contact)
                        }
                    } else {
                        if (contactExists(phoneNumber, name, list).not()) {
                            val contact = PhoneContact()
                            contact.name = name
                            contact.number = phoneNumber
                            contact.id = id
                            // contact.image = getPhotoUri(id)
                            list.add(contact)
                        }
                    }
                }
                cursor.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        public fun getPhotoUri(id: Int): Uri? {
            try {
                val cur = LinphoneApplication.getApplicationContext().getContentResolver().query(
                        ContactsContract.Data.CONTENT_URI, null,
                        ContactsContract.Data.CONTACT_ID + "=" + id + " AND " +
                                ContactsContract.Data.MIMETYPE + "='" +
                                ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'", null, null)
                if (cur != null) {
                    if (!cur!!.moveToFirst()) {
                        return null // no photo
                    }
                } else {
                    return null // error in cursor process
                }
                cur.close()
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }

            val person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, java.lang.Long.parseLong(id.toString()))
            return Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)
        }

        fun readContactsByName(list: ArrayList<PhoneContact>, query: String) {
            try {
                val context = LinphoneApplication.getApplicationContext()
                val cursor = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + "=? ",
                        arrayOf(query),
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC") ?: return

                while (cursor.moveToNext()) {
                    val phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    if (contactExists(phoneNumber, list).not()) {
                        val name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                        val contact = PhoneContact()
                        contact.name = name
                        contact.number = phoneNumber
                        list.add(contact)
                    }
                }
                cursor.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getContactName(phoneNumber: String): String {
            val context = LinphoneApplication.getApplicationContext()
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            var contactName = ""
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    contactName = cursor.getString(0)
                }
                cursor.close()
            }
            return contactName
        }

        fun formatCallDuration(duration: Int): String {
            val d = Date(duration * 1000L)
            val df = SimpleDateFormat("HH:mm:ss", Locale.US) // HH for 0-23
            df.timeZone = (TimeZone.getTimeZone("GMT"))
            return df.format(d)
        }

        fun isValidPhoneNumber(phoneNumber: String): Boolean {
            return !TextUtils.isEmpty(phoneNumber) && Patterns.PHONE.matcher(phoneNumber).matches()
        }

        fun isOnline(): Boolean {
            val context = LinphoneApplication.getApplicationContext()
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val netInfo = cm.activeNetworkInfo
            // should check null because in airplane mode it will be null
            return netInfo != null && netInfo.isConnected
        }
    }
}
