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
import androidx.activity.addCallback
import androidx.navigation.fragment.findNavController
import com.bdcom.appdialer.LinphoneApplication.Companion.corePreferences
import com.bdcom.appdialer.R
import com.bdcom.appdialer.activities.GenericFragment
import com.bdcom.appdialer.databinding.AssistantTopBarFragmentBinding

class TopBarFragment : GenericFragment<AssistantTopBarFragmentBinding>() {
    override fun getLayoutId(): Int = R.layout.assistant_top_bar_fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            goBack()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this

        binding.setBackClickListener {
            goBack()
        }
    }

    private fun goBack() {
        if (!findNavController().popBackStack()) {
            requireActivity().finish()
            if (corePreferences.enableAnimations) {
                requireActivity().overridePendingTransition(
                    R.anim.enter_left,
                    R.anim.exit_right
                )
            }
        }
    }
}
