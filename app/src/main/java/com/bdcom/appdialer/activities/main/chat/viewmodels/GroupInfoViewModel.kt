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
import com.bdcom.appdialer.LinphoneApplication.Companion.coreContext
import com.bdcom.appdialer.R
import com.bdcom.appdialer.activities.main.chat.GroupChatRoomMember
import com.bdcom.appdialer.activities.main.viewmodels.ErrorReportingViewModel
import com.bdcom.appdialer.utils.Event
import org.linphone.core.*
import org.linphone.core.tools.Log

class GroupInfoViewModelFactory(private val chatRoom: ChatRoom?) :
    ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return GroupInfoViewModel(chatRoom) as T
    }
}

class GroupInfoViewModel(val chatRoom: ChatRoom?) : ErrorReportingViewModel() {
    val createdChatRoomEvent = MutableLiveData<Event<ChatRoom>>()

    val subject = MutableLiveData<String>()

    val participants = MutableLiveData<ArrayList<GroupChatRoomMember>>()

    val isEncrypted = MutableLiveData<Boolean>()

    val isMeAdmin = MutableLiveData<Boolean>()

    val canLeaveGroup = MutableLiveData<Boolean>()

    val waitForChatRoomCreation = MutableLiveData<Boolean>()

    val meAdminChangedEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    private val listener = object : ChatRoomListenerStub() {
        override fun onStateChanged(chatRoom: ChatRoom, state: ChatRoom.State) {
            if (state == ChatRoom.State.Created) {
                waitForChatRoomCreation.value = false
                createdChatRoomEvent.value = Event(chatRoom) // To trigger going to the chat room
            } else if (state == ChatRoom.State.CreationFailed) {
                Log.e("[Chat Room Group Info] Group chat room creation has failed !")
                waitForChatRoomCreation.value = false
                onErrorEvent.value = Event(com.bdcom.appdialer.R.string.chat_room_creation_failed_snack)
            }
        }

        override fun onSubjectChanged(chatRoom: ChatRoom, eventLog: EventLog) {
            subject.value = chatRoom.subject
        }

        override fun onParticipantAdded(chatRoom: ChatRoom, eventLog: EventLog) {
            updateParticipants()
        }

        override fun onParticipantRemoved(chatRoom: ChatRoom, eventLog: EventLog) {
            updateParticipants()
        }

        override fun onParticipantAdminStatusChanged(chatRoom: ChatRoom, eventLog: EventLog) {
            val admin = chatRoom.me?.isAdmin ?: false
            if (admin != isMeAdmin.value) {
                isMeAdmin.value = admin
                meAdminChangedEvent.value = Event(admin)
            }
            updateParticipants()
        }
    }

    init {
        subject.value = chatRoom?.subject
        isMeAdmin.value = chatRoom == null || (chatRoom.me?.isAdmin == true && !chatRoom.hasBeenLeft())
        canLeaveGroup.value = chatRoom != null && !chatRoom.hasBeenLeft()
        isEncrypted.value = chatRoom?.hasCapability(ChatRoomCapabilities.Encrypted.toInt())

        if (chatRoom != null) updateParticipants()

        chatRoom?.addListener(listener)
        waitForChatRoomCreation.value = false
    }

    override fun onCleared() {
        chatRoom?.removeListener(listener)

        super.onCleared()
    }

    fun createChatRoom() {
        waitForChatRoomCreation.value = true
        val params: ChatRoomParams = coreContext.core.createDefaultChatRoomParams()
        params.enableEncryption(isEncrypted.value == true)
        params.enableGroup(true)
        params.subject = subject.value

        val addresses = arrayOfNulls<Address>(participants.value.orEmpty().size)
        var index = 0
        for (participant in participants.value.orEmpty()) {
            addresses[index] = participant.address
            Log.i("[Chat Room Group Info] Participant ${participant.address.asStringUriOnly()} will be added to group")
            index += 1
        }

        val chatRoom: ChatRoom? = coreContext.core.createChatRoom(params, coreContext.core.defaultAccount?.params?.identityAddress, addresses)
        chatRoom?.addListener(listener)
        if (chatRoom == null) {
            Log.e("[Chat Room Group Info] Couldn't create chat room!")
            waitForChatRoomCreation.value = false
            onErrorEvent.value = Event(R.string.chat_room_creation_failed_snack)
        }
    }

    fun updateRoom() {
        if (chatRoom != null) {
            // Subject
            val newSubject = subject.value.orEmpty()
            if (newSubject.isNotEmpty() && newSubject != chatRoom.subject) {
                Log.i("[Chat Room Group Info] Subject changed to $newSubject")
                chatRoom.subject = newSubject
            }

            // Removed participants
            val participantsToRemove = arrayListOf<Participant>()
            for (participant in chatRoom.participants) {
                val member = participants.value.orEmpty().find { member ->
                    participant.address.weakEqual(member.address)
                }
                if (member == null) {
                    Log.w("[Chat Room Group Info] Participant ${participant.address.asStringUriOnly()} will be removed from group")
                    participantsToRemove.add(participant)
                }
            }
            val toRemove = arrayOfNulls<Participant>(participantsToRemove.size)
            participantsToRemove.toArray(toRemove)
            chatRoom.removeParticipants(toRemove)

            // Added participants & new admins
            val participantsToAdd = arrayListOf<Address>()
            for (member in participants.value.orEmpty()) {
                val participant = chatRoom.participants.find { participant ->
                    participant.address.weakEqual(member.address)
                }
                if (participant != null) {
                    // Participant found, check if admin status needs to be updated
                    if (member.isAdmin != participant.isAdmin) {
                        if (chatRoom.me?.isAdmin == true) {
                            Log.i("[Chat Room Group Info] Participant ${member.address.asStringUriOnly()} will be admin? ${member.isAdmin}")
                            chatRoom.setParticipantAdminStatus(participant, member.isAdmin)
                        }
                    }
                } else {
                    Log.i("[Chat Room Group Info] Participant ${member.address.asStringUriOnly()} will be added to group")
                    participantsToAdd.add(member.address)
                }
            }
            val toAdd = arrayOfNulls<Address>(participantsToAdd.size)
            participantsToAdd.toArray(toAdd)
            chatRoom.addParticipants(toAdd)

            // Go back to chat room
            createdChatRoomEvent.value = Event(chatRoom)
        }
    }

    fun leaveGroup() {
        if (chatRoom != null) {
            Log.w("[Chat Room Group Info] Leaving group")
            chatRoom.leave()
            createdChatRoomEvent.value = Event(chatRoom)
        }
    }

    fun removeParticipant(participant: GroupChatRoomMember) {
        val list = arrayListOf<GroupChatRoomMember>()
        list.addAll(participants.value.orEmpty())
        list.remove(participant)
        participants.value = list
    }

    private fun updateParticipants() {
        val list = arrayListOf<GroupChatRoomMember>()

        if (chatRoom != null) {
            for (participant in chatRoom.participants) {
                list.add(GroupChatRoomMember(participant.address, participant.isAdmin, participant.securityLevel, canBeSetAdmin = true))
            }
        }

        participants.value = list
    }
}
