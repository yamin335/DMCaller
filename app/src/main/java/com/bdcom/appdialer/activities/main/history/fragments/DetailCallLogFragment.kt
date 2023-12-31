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
package com.bdcom.appdialer.activities.main.history.fragments

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bdcom.appdialer.LinphoneApplication.Companion.coreContext
import com.bdcom.appdialer.R
import com.bdcom.appdialer.activities.*
import com.bdcom.appdialer.activities.main.*
import com.bdcom.appdialer.activities.main.history.viewmodels.CallLogViewModel
import com.bdcom.appdialer.activities.main.history.viewmodels.CallLogViewModelFactory
import com.bdcom.appdialer.activities.main.viewmodels.SharedMainViewModel
import com.bdcom.appdialer.activities.navigateToContact
import com.bdcom.appdialer.activities.navigateToContacts
import com.bdcom.appdialer.activities.navigateToFriend
import com.bdcom.appdialer.contact.NativeContact
import com.bdcom.appdialer.databinding.HistoryDetailFragmentBinding
import org.linphone.core.tools.Log

class DetailCallLogFragment : GenericFragment<HistoryDetailFragmentBinding>() {
    private lateinit var viewModel: CallLogViewModel
    private lateinit var sharedViewModel: SharedMainViewModel

    override fun getLayoutId(): Int = R.layout.history_detail_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this

        sharedViewModel = requireActivity().run {
            ViewModelProvider(this).get(SharedMainViewModel::class.java)
        }

        val callLogGroup = sharedViewModel.selectedCallLogGroup.value
        if (callLogGroup == null) {
            Log.e("[History] Call log group is null, aborting!")
            (activity as MainActivity).showSnackBar(R.string.error)
            findNavController().navigateUp()
            return
        }

        viewModel = ViewModelProvider(
            this,
            CallLogViewModelFactory(callLogGroup.lastCallLog)
        )[CallLogViewModel::class.java]
        binding.viewModel = viewModel

        viewModel.relatedCallLogs.value = callLogGroup.callLogs

        binding.setBackClickListener {
            findNavController().popBackStack()
        }
        binding.back.visibility = if (resources.getBoolean(R.bool.isTablet)) View.INVISIBLE else View.VISIBLE

        binding.setNewContactClickListener {
            val copy = viewModel.callLog.remoteAddress.clone()
            copy.clean()
            Log.i("[History] Creating contact with SIP URI: ${copy.asStringUriOnly()}")
            navigateToContacts(viewModel.callLog.remoteAddress.asStringUriOnly())
        }

        binding.setContactClickListener {
            val contact = viewModel.contact.value as? NativeContact
            if (contact != null) {
                Log.i("[History] Displaying contact $contact")
                navigateToContact(contact)
            } else {
                val copy = viewModel.callLog.remoteAddress.clone()
                copy.clean()
                Log.i("[History] Displaying friend with address ${copy.asStringUriOnly()}")
                navigateToFriend(copy)
            }
        }

        viewModel.startCallEvent.observe(viewLifecycleOwner, {
            it.consume { address ->
                if (coreContext.core.callsNb > 0) {
                    Log.i("[History] Starting dialer with pre-filled URI ${address.asStringUriOnly()}, is transfer? ${sharedViewModel.pendingCallTransfer}")
                    val args = Bundle()
                    args.putString("URI", address.asStringUriOnly())
                    args.putBoolean("Transfer", sharedViewModel.pendingCallTransfer)
                    args.putBoolean("SkipAutoCallStart", true) // If auto start call setting is enabled, ignore it
                    navigateToDialer(args)
                } else {
                    coreContext.startCall(address)
                }
            }
        })

        viewModel.chatRoomCreatedEvent.observe(viewLifecycleOwner, {
            it.consume { chatRoom ->
                val args = Bundle()
                args.putString("LocalSipUri", chatRoom.localAddress.asStringUriOnly())
                args.putString("RemoteSipUri", chatRoom.peerAddress.asStringUriOnly())
                navigateToChatRoom(args)
            }
        })

        viewModel.onErrorEvent.observe(viewLifecycleOwner, {
            it.consume { messageResourceId ->
                (activity as MainActivity).showSnackBar(messageResourceId)
            }
        })
    }
}
