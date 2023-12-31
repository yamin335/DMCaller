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
package com.bdcom.appdialer.activities.main.contact.fragments

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bdcom.appdialer.LinphoneApplication.Companion.coreContext
import com.bdcom.appdialer.R
import com.bdcom.appdialer.activities.GenericFragment
import com.bdcom.appdialer.activities.main.*
import com.bdcom.appdialer.activities.main.contact.viewmodels.ContactViewModel
import com.bdcom.appdialer.activities.main.contact.viewmodels.ContactViewModelFactory
import com.bdcom.appdialer.activities.main.viewmodels.DialogViewModel
import com.bdcom.appdialer.activities.main.viewmodels.SharedMainViewModel
import com.bdcom.appdialer.activities.navigateToChatRoom
import com.bdcom.appdialer.activities.navigateToContactEditor
import com.bdcom.appdialer.activities.navigateToDialer
import com.bdcom.appdialer.databinding.ContactDetailFragmentBinding
import com.bdcom.appdialer.utils.DialogUtils
import org.linphone.core.tools.Log

class DetailContactFragment : GenericFragment<ContactDetailFragmentBinding>() {
    private lateinit var viewModel: ContactViewModel
    private lateinit var sharedViewModel: SharedMainViewModel

    override fun getLayoutId(): Int = R.layout.contact_detail_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this

        sharedViewModel = requireActivity().run {
            ViewModelProvider(this).get(SharedMainViewModel::class.java)
        }

        val id = arguments?.getString("id")
        arguments?.clear()
        if (id != null) {
            Log.i("[Contact] Found contact id parameter in arguments: $id")
            sharedViewModel.selectedContact.value = coreContext.contactsManager.findContactById(id)
        }

        val contact = sharedViewModel.selectedContact.value
        if (contact == null) {
            Log.e("[Contact] Contact is null, aborting!")
            (activity as MainActivity).showSnackBar(R.string.error)
            findNavController().navigateUp()
            return
        }

        viewModel = ViewModelProvider(
            this,
            ContactViewModelFactory(contact)
        )[ContactViewModel::class.java]
        binding.viewModel = viewModel

        viewModel.sendSmsToEvent.observe(viewLifecycleOwner, {
            it.consume { number ->
                sendSms(number)
            }
        })

        viewModel.startCallToEvent.observe(viewLifecycleOwner, {
            it.consume { address ->
                if (coreContext.core.callsNb > 0) {
                    Log.i("[Contact] Starting dialer with pre-filled URI ${address.asStringUriOnly()}, is transfer? ${sharedViewModel.pendingCallTransfer}")
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

        binding.setBackClickListener {
            findNavController().popBackStack()
        }
        binding.back.visibility = if (resources.getBoolean(R.bool.isTablet)) View.INVISIBLE else View.VISIBLE

        binding.setEditClickListener {
            navigateToContactEditor()
        }

        binding.setDeleteClickListener {
            confirmContactRemoval()
        }

        viewModel.onErrorEvent.observe(viewLifecycleOwner, {
            it.consume { messageResourceId ->
                (activity as MainActivity).showSnackBar(messageResourceId)
            }
        })
    }

    private fun confirmContactRemoval() {
        val dialogViewModel = DialogViewModel(getString(R.string.contact_delete_one_dialog))
        val dialog: Dialog = DialogUtils.getDialog(requireContext(), dialogViewModel)

        dialogViewModel.showCancelButton {
            dialog.dismiss()
        }

        dialogViewModel.showDeleteButton({
            viewModel.deleteContact()
            dialog.dismiss()
            findNavController().navigateUp()
        }, getString(R.string.dialog_delete))

        dialog.show()
    }

    private fun sendSms(number: String) {
        val smsIntent = Intent(Intent.ACTION_SENDTO)
        smsIntent.putExtra("address", number)
        smsIntent.data = Uri.parse("smsto:$number")
        val text = getString(R.string.contact_send_sms_invite_text).format(getString(R.string.contact_send_sms_invite_download_link))
        smsIntent.putExtra("sms_body", text)
        startActivity(smsIntent)
    }
}
