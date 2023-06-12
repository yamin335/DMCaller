package com.bdcom.appdialer.models

 import android.net.Uri
import com.bdcom.appdialer.utils.AndroidUtil

 class PhoneContact {

    var id: Int? = 0

    var name: String? = null

    var number: String? = null

    var image: Uri? = null

    val color: Int = AndroidUtil.getRandomColor()

    var showDeleteIcon = false
 }
