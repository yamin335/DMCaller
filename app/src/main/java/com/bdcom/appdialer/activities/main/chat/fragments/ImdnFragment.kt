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

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bdcom.appdialer.R
import com.bdcom.appdialer.activities.main.MainActivity
import com.bdcom.appdialer.activities.main.chat.adapters.ImdnAdapter
import com.bdcom.appdialer.activities.main.chat.viewmodels.ImdnViewModel
import com.bdcom.appdialer.activities.main.chat.viewmodels.ImdnViewModelFactory
import com.bdcom.appdialer.activities.main.fragments.SecureFragment
import com.bdcom.appdialer.activities.main.viewmodels.SharedMainViewModel
import com.bdcom.appdialer.databinding.ChatRoomImdnFragmentBinding
import com.bdcom.appdialer.utils.RecyclerViewHeaderDecoration
import org.linphone.core.tools.Log

class ImdnFragment : SecureFragment<ChatRoomImdnFragmentBinding>() {
    private lateinit var viewModel: ImdnViewModel
    private lateinit var adapter: ImdnAdapter
    private lateinit var sharedViewModel: SharedMainViewModel

    override fun getLayoutId(): Int {
        return R.layout.chat_room_imdn_fragment
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this

        sharedViewModel = requireActivity().run {
            ViewModelProvider(this).get(SharedMainViewModel::class.java)
        }

        val chatRoom = sharedViewModel.selectedChatRoom.value
        if (chatRoom == null) {
            Log.e("[IMDN] Chat room is null, aborting!")
            (activity as MainActivity).showSnackBar(R.string.error)
            findNavController().navigateUp()
            return
        }

        isSecure = chatRoom.currentParams.encryptionEnabled()

        if (arguments != null) {
            val messageId = arguments?.getString("MessageId")
            val message = if (messageId != null) chatRoom.findMessage(messageId) else null
            if (message != null) {
                Log.i("[IMDN] Found message $message with id $messageId")
                viewModel = ViewModelProvider(
                    this,
                    ImdnViewModelFactory(message)
                )[ImdnViewModel::class.java]
                binding.viewModel = viewModel
            } else {
                Log.e("[IMDN] Couldn't find message with id $messageId in chat room $chatRoom")
                findNavController().popBackStack()
                return
            }
        } else {
            Log.e("[IMDN] Couldn't find message id in intent arguments")
            findNavController().popBackStack()
            return
        }

        adapter = ImdnAdapter(viewLifecycleOwner)
        binding.participantsList.adapter = adapter

        val layoutManager = LinearLayoutManager(activity)
        binding.participantsList.layoutManager = layoutManager

        // Divider between items
        val dividerItemDecoration = DividerItemDecoration(context, layoutManager.orientation)
        dividerItemDecoration.setDrawable(resources.getDrawable(R.drawable.divider, null))
        binding.participantsList.addItemDecoration(dividerItemDecoration)

        // Displays state header
        val headerItemDecoration = RecyclerViewHeaderDecoration(adapter)
        binding.participantsList.addItemDecoration(headerItemDecoration)

        viewModel.participants.observe(viewLifecycleOwner, {
            adapter.submitList(it)
        })

        binding.setBackClickListener {
            findNavController().popBackStack()
        }
    }
}
