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
package com.bdcom.appdialer.activities.main.settings.fragments

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bdcom.appdialer.R
import com.bdcom.appdialer.activities.GenericFragment
import com.bdcom.appdialer.activities.main.settings.viewmodels.CallSettingsViewModel
import com.bdcom.appdialer.compatibility.Compatibility
import com.bdcom.appdialer.databinding.SettingsCallFragmentBinding
import org.linphone.mediastream.Version

class CallSettingsFragment : GenericFragment<SettingsCallFragmentBinding>() {
    private lateinit var viewModel: CallSettingsViewModel

    override fun getLayoutId(): Int = R.layout.settings_call_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this

        viewModel = ViewModelProvider(this).get(CallSettingsViewModel::class.java)
        binding.viewModel = viewModel

        binding.setBackClickListener { findNavController().popBackStack() }
        binding.back.visibility = if (resources.getBoolean(R.bool.isTablet)) View.INVISIBLE else View.VISIBLE

        viewModel.systemWideOverlayEnabledEvent.observe(viewLifecycleOwner, {
            it.consume {
                if (!Compatibility.canDrawOverlay(requireContext())) {
                    val intent = Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:${requireContext().packageName}"))
                    startActivityForResult(intent, 0)
                }
            }
        })

        viewModel.goToAndroidNotificationSettingsEvent.observe(viewLifecycleOwner, { it.consume {
            if (Build.VERSION.SDK_INT >= Version.API26_O_80) {
                val i = Intent()
                i.action = Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
                i.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                i.putExtra(
                    Settings.EXTRA_CHANNEL_ID,
                    getString(R.string.notification_channel_service_id)
                )
                i.addCategory(Intent.CATEGORY_DEFAULT)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                startActivity(i)
            }
        } })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (!Compatibility.canDrawOverlay(requireContext())) {
            viewModel.systemWideOverlayListener.onBoolValueChanged(false)
        }
    }
}
