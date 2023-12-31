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
package com.bdcom.appdialer.contact

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.databinding.DataBindingUtil
import com.bdcom.appdialer.LinphoneApplication
import com.bdcom.appdialer.R
import com.bdcom.appdialer.databinding.ContactAvatarBigBinding
import com.bdcom.appdialer.utils.AppUtils

class BigContactAvatarView : LinearLayout {
    lateinit var binding: ContactAvatarBigBinding

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    fun init(context: Context) {
        binding = DataBindingUtil.inflate(
            LayoutInflater.from(context), R.layout.contact_avatar_big, this, true
        )
    }

    fun setViewModel(viewModel: ContactDataInterface?) {
        if (viewModel == null) {
            binding.root.visibility = View.GONE
            return
        }
        binding.root.visibility = View.VISIBLE

        val contact: Contact? = viewModel.contact.value
        val initials = if (contact != null) {
            AppUtils.getInitials(contact.fullName ?: contact.firstName + " " + contact.lastName)
        } else {
            AppUtils.getInitials(viewModel.displayName)
        }

        binding.initials = initials
        binding.generatedAvatarVisibility = initials.isNotEmpty() && initials != "+"
        binding.imagePath = contact?.getContactPictureUri()
        binding.borderVisibility = com.bdcom.appdialer.LinphoneApplication.corePreferences.showBorderOnBigContactAvatar
    }
}
