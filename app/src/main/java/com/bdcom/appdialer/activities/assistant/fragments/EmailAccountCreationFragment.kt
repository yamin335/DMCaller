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
import com.bdcom.appdialer.R
import com.bdcom.appdialer.activities.GenericFragment
import com.bdcom.appdialer.activities.assistant.AssistantActivity
import com.bdcom.appdialer.activities.assistant.viewmodels.EmailAccountCreationViewModel
import com.bdcom.appdialer.activities.assistant.viewmodels.EmailAccountCreationViewModelFactory
import com.bdcom.appdialer.activities.assistant.viewmodels.SharedAssistantViewModel
import com.bdcom.appdialer.activities.navigateToEmailAccountValidation
import com.bdcom.appdialer.databinding.AssistantEmailAccountCreationFragmentBinding

class EmailAccountCreationFragment : GenericFragment<AssistantEmailAccountCreationFragmentBinding>() {
    private lateinit var sharedViewModel: SharedAssistantViewModel
    private lateinit var viewModel: EmailAccountCreationViewModel

    override fun getLayoutId(): Int = R.layout.assistant_email_account_creation_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this

        sharedViewModel = requireActivity().run {
            ViewModelProvider(this).get(SharedAssistantViewModel::class.java)
        }

        viewModel = ViewModelProvider(this, EmailAccountCreationViewModelFactory(sharedViewModel.getAccountCreator())).get(EmailAccountCreationViewModel::class.java)
        binding.viewModel = viewModel

        viewModel.goToEmailValidationEvent.observe(viewLifecycleOwner, {
            it.consume {
                navigateToEmailAccountValidation()
            }
        })

        viewModel.onErrorEvent.observe(viewLifecycleOwner, {
            it.consume { message ->
                (requireActivity() as AssistantActivity).showSnackBar(message)
            }
        })
    }
}
