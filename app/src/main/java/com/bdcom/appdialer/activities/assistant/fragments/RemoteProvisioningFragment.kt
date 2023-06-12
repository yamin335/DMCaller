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
package com.bdcom.appdialer.activities.assistant.fragments

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.bdcom.appdialer.LinphoneApplication.Companion.coreContext
import com.bdcom.appdialer.R
import com.bdcom.appdialer.activities.GenericFragment
import com.bdcom.appdialer.activities.assistant.AssistantActivity
import com.bdcom.appdialer.activities.assistant.viewmodels.RemoteProvisioningViewModel
import com.bdcom.appdialer.activities.assistant.viewmodels.SharedAssistantViewModel
import com.bdcom.appdialer.activities.navigateToEchoCancellerCalibration
import com.bdcom.appdialer.activities.navigateToQrCode
import com.bdcom.appdialer.databinding.AssistantRemoteProvisioningFragmentBinding

class RemoteProvisioningFragment : GenericFragment<AssistantRemoteProvisioningFragmentBinding>() {
    private lateinit var sharedViewModel: SharedAssistantViewModel
    private lateinit var viewModel: RemoteProvisioningViewModel

    override fun getLayoutId(): Int = R.layout.assistant_remote_provisioning_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this

        sharedViewModel = requireActivity().run {
            ViewModelProvider(this).get(SharedAssistantViewModel::class.java)
        }

        viewModel = ViewModelProvider(this).get(RemoteProvisioningViewModel::class.java)
        binding.viewModel = viewModel

        binding.setQrCodeClickListener {
            navigateToQrCode()
        }

        viewModel.fetchSuccessfulEvent.observe(viewLifecycleOwner, {
            it.consume { success ->
                if (success) {
                    if (coreContext.core.isEchoCancellerCalibrationRequired) {
                        navigateToEchoCancellerCalibration()
                    } else {
                        requireActivity().finish()
                    }
                } else {
                    val activity = requireActivity() as AssistantActivity
                    activity.showSnackBar(R.string.assistant_remote_provisioning_failure)
                }
            }
        })

        viewModel.urlToFetch.value = sharedViewModel.remoteProvisioningUrl.value ?: coreContext.core.provisioningUri
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedViewModel.remoteProvisioningUrl.value = null
    }
}
