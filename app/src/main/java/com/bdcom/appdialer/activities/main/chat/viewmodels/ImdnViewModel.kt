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
package com.bdcom.appdialer.activities.main.chat.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bdcom.appdialer.activities.main.chat.data.ChatMessageData
import org.linphone.core.ChatMessage
import org.linphone.core.ChatMessageListenerStub
import org.linphone.core.ParticipantImdnState

class ImdnViewModelFactory(private val chatMessage: ChatMessage) :
    ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ImdnViewModel(chatMessage) as T
    }
}

class ImdnViewModel(private val chatMessage: ChatMessage) : ViewModel() {
    val participants = MutableLiveData<ArrayList<ParticipantImdnState>>()

    val chatMessageViewModel = ChatMessageData(chatMessage)

    private val listener = object : ChatMessageListenerStub() {
        override fun onParticipantImdnStateChanged(
            message: ChatMessage,
            state: ParticipantImdnState
        ) {
            updateParticipantsLists()
        }
    }

    init {
        chatMessage.addListener(listener)
        updateParticipantsLists()
    }

    override fun onCleared() {
        chatMessage.removeListener(listener)
        super.onCleared()
    }

    private fun updateParticipantsLists() {
        val list = arrayListOf<ParticipantImdnState>()
        list.addAll(chatMessage.getParticipantsByImdnState(ChatMessage.State.Displayed))
        list.addAll(chatMessage.getParticipantsByImdnState(ChatMessage.State.DeliveredToUser))
        list.addAll(chatMessage.getParticipantsByImdnState(ChatMessage.State.Delivered))
        list.addAll(chatMessage.getParticipantsByImdnState(ChatMessage.State.NotDelivered))
        participants.value = list
    }
}
