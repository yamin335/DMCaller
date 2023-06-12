package com.bdcom.appdialer.utils

import android.app.Activity
import android.content.Intent
import com.bdcom.appdialer.activities.BottomTabsActivity

fun Progress?.showSafely() {
    this?.show()
}

fun Progress?.dismissSafely() {
    this?.dismiss()
}

fun Activity.finishOpeningBottomTabActivity() {
    val intent = Intent(this, BottomTabsActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    startActivity(intent)
    finish()
}
