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
package com.bdcom.appdialer.activities.main.chat.fragments

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bdcom.appdialer.LinphoneApplication
import com.bdcom.appdialer.R
import com.bdcom.appdialer.activities.main.MainActivity
import com.bdcom.appdialer.activities.main.chat.adapters.ChatRoomCreationContactsAdapter
import com.bdcom.appdialer.activities.main.chat.viewmodels.ChatRoomCreationViewModel
import com.bdcom.appdialer.activities.main.fragments.SecureFragment
import com.bdcom.appdialer.activities.main.viewmodels.SharedMainViewModel
import com.bdcom.appdialer.activities.navigateToChatRoom
import com.bdcom.appdialer.activities.navigateToGroupInfo
import com.bdcom.appdialer.databinding.ChatRoomCreationFragmentBinding
import com.bdcom.appdialer.utils.PermissionHelper
import org.linphone.core.tools.Log

class ChatRoomCreationFragment : SecureFragment<ChatRoomCreationFragmentBinding>() {
    private lateinit var viewModel: ChatRoomCreationViewModel
    private lateinit var sharedViewModel: SharedMainViewModel
    private lateinit var adapter: ChatRoomCreationContactsAdapter

    override fun getLayoutId(): Int = R.layout.chat_room_creation_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this

        sharedViewModel = requireActivity().run {
            ViewModelProvider(this).get(SharedMainViewModel::class.java)
        }

        val createGroup = arguments?.getBoolean("createGroup") ?: false

        viewModel = ViewModelProvider(this).get(ChatRoomCreationViewModel::class.java)
        viewModel.createGroupChat.value = createGroup

        viewModel.isEncrypted.value = sharedViewModel.createEncryptedChatRoom

        binding.viewModel = viewModel

        adapter = ChatRoomCreationContactsAdapter(viewLifecycleOwner)
        adapter.groupChatEnabled = viewModel.createGroupChat.value == true
        adapter.updateSecurity(viewModel.isEncrypted.value == true)
        binding.contactsList.adapter = adapter

        val layoutManager = LinearLayoutManager(activity)
        binding.contactsList.layoutManager = layoutManager

        // Divider between items
        val dividerItemDecoration = DividerItemDecoration(context, layoutManager.orientation)
        dividerItemDecoration.setDrawable(resources.getDrawable(R.drawable.divider, null))
        binding.contactsList.addItemDecoration(dividerItemDecoration)

        binding.setBackClickListener {
            findNavController().popBackStack()
        }
        binding.back.visibility = if (resources.getBoolean(R.bool.isTablet)) View.INVISIBLE else View.VISIBLE

        binding.setAllContactsToggleClickListener {
            viewModel.sipContactsSelected.value = false
        }

        binding.setSipContactsToggleClickListener {
            viewModel.sipContactsSelected.value = true
        }

        viewModel.contactsList.observe(viewLifecycleOwner, {
            adapter.submitList(it)
        })

        viewModel.isEncrypted.observe(viewLifecycleOwner, {
            adapter.updateSecurity(it)
        })

        viewModel.sipContactsSelected.observe(viewLifecycleOwner, {
            viewModel.updateContactsList()
        })

        viewModel.selectedAddresses.observe(viewLifecycleOwner, {
            adapter.updateSelectedAddresses(it)
        })

        viewModel.chatRoomCreatedEvent.observe(viewLifecycleOwner, {
            it.consume { chatRoom ->
                sharedViewModel.selectedChatRoom.value = chatRoom
                navigateToChatRoom()
            }
        })

        viewModel.filter.observe(viewLifecycleOwner, {
            viewModel.applyFilter()
        })

        adapter.selectedContact.observe(viewLifecycleOwner, {
            it.consume { searchResult ->
                if (createGroup) {
                    viewModel.toggleSelectionForSearchResult(searchResult)
                } else {
                    viewModel.createOneToOneChat(searchResult)
                }
            }
        })

        addParticipantsFromSharedViewModel()

        // Next button is only used to go to group chat info fragment
        binding.setNextClickListener {
            sharedViewModel.createEncryptedChatRoom = viewModel.isEncrypted.value == true
            sharedViewModel.chatRoomParticipants.value = viewModel.selectedAddresses.value
            navigateToGroupInfo()
        }

        viewModel.onErrorEvent.observe(viewLifecycleOwner, {
            it.consume { messageResourceId ->
                (activity as MainActivity).showSnackBar(messageResourceId)
            }
        })

        if (!PermissionHelper.get().hasReadContactsPermission()) {
            Log.i("[Chat Room Creation] Asking for READ_CONTACTS permission")
            requestPermissions(arrayOf(android.Manifest.permission.READ_CONTACTS), 0)
        }
    }

    private fun addParticipantsFromSharedViewModel() {
        val participants = sharedViewModel.chatRoomParticipants.value
        if (participants != null && participants.size > 0) {
            viewModel.selectedAddresses.value = participants
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 0) {
            val granted = grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                Log.i("[Chat Room Creation] READ_CONTACTS permission granted")
                com.bdcom.appdialer.LinphoneApplication.coreContext.contactsManager.onReadContactsPermissionGranted()
                com.bdcom.appdialer.LinphoneApplication.coreContext.contactsManager.fetchContactsAsync()
            } else {
                Log.w("[Chat Room Creation] READ_CONTACTS permission denied")
            }
        }
    }
}
