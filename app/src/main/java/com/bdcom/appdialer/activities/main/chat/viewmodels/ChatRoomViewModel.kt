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
import com.bdcom.appdialer.contact.Contact
import com.bdcom.appdialer.contact.ContactDataInterface
import com.bdcom.appdialer.contact.ContactsUpdatedListenerStub
import com.bdcom.appdialer.utils.AppUtils
import com.bdcom.appdialer.utils.LinphoneUtils
import com.bdcom.appdialer.utils.TimestampUtils
import org.linphone.core.*
import org.linphone.core.tools.Log

class ChatRoomViewModelFactory(private val chatRoom: ChatRoom) :
    ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ChatRoomViewModel(chatRoom) as T
    }
}

class ChatRoomViewModel(val chatRoom: ChatRoom) : ViewModel(), ContactDataInterface {
    override val contact = MutableLiveData<Contact>()

    override val displayName: String
        get() {
            return when {
                chatRoom.hasCapability(ChatRoomCapabilities.Basic.toInt()) -> LinphoneUtils.getDisplayName(chatRoom.peerAddress)
                chatRoom.hasCapability(ChatRoomCapabilities.OneToOne.toInt()) -> LinphoneUtils.getDisplayName(chatRoom.participants.firstOrNull()?.address ?: chatRoom.peerAddress)
                chatRoom.hasCapability(ChatRoomCapabilities.Conference.toInt()) -> chatRoom.subject.orEmpty()
                else -> chatRoom.peerAddress.asStringUriOnly()
            }
        }

    override val securityLevel: ChatRoomSecurityLevel
        get() = chatRoom.securityLevel

    override val showGroupChatAvatar: Boolean
        get() = chatRoom.hasCapability(ChatRoomCapabilities.Conference.toInt()) && !chatRoom.hasCapability(ChatRoomCapabilities.OneToOne.toInt())

    val subject = MutableLiveData<String>()

    val participants = MutableLiveData<String>()

    val unreadMessagesCount = MutableLiveData<Int>()

    val lastUpdate = MutableLiveData<String>()

    val lastMessageText = MutableLiveData<String>()

    val callInProgress = MutableLiveData<Boolean>()

    val remoteIsComposing = MutableLiveData<Boolean>()

    val composingList = MutableLiveData<String>()

    val securityLevelIcon = MutableLiveData<Int>()

    val securityLevelContentDescription = MutableLiveData<Int>()

    val oneToOneChatRoom: Boolean
        get() = chatRoom.hasCapability(ChatRoomCapabilities.OneToOne.toInt())

    val encryptedChatRoom: Boolean
        get() = chatRoom.hasCapability(ChatRoomCapabilities.Encrypted.toInt())

    val basicChatRoom: Boolean
        get() = chatRoom.hasCapability(ChatRoomCapabilities.Basic.toInt())

    val peerSipUri: String
        get() = if (oneToOneChatRoom && !basicChatRoom)
                    chatRoom.participants.firstOrNull()?.address?.asStringUriOnly()
                        ?: chatRoom.peerAddress.asStringUriOnly()
                else chatRoom.peerAddress.asStringUriOnly()

    val oneParticipantOneDevice: Boolean
        get() {
            return chatRoom.hasCapability(ChatRoomCapabilities.OneToOne.toInt()) &&
                chatRoom.me?.devices?.size == 1 &&
                chatRoom.participants.first().devices.size == 1
        }

    val addressToCall: Address
        get() {
            return if (chatRoom.hasCapability(ChatRoomCapabilities.Basic.toInt()))
                chatRoom.peerAddress
            else
                chatRoom.participants.first().address
        }

    val onlyParticipantOnlyDeviceAddress: Address
        get() = chatRoom.participants.first().devices.first().address

    private val contactsUpdatedListener = object : ContactsUpdatedListenerStub() {
        override fun onContactsUpdated() {
            Log.i("[Chat Room] Contacts have changed")
            contactLookup()
        }
    }

    private val coreListener: CoreListenerStub = object : CoreListenerStub() {
        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State,
            message: String
        ) {
            callInProgress.value = core.callsNb > 0
        }
    }

    private val chatRoomListener: ChatRoomListenerStub = object : ChatRoomListenerStub() {
        override fun onStateChanged(chatRoom: ChatRoom, state: ChatRoom.State) {
            Log.i("[Chat Room] $chatRoom state changed: $state")
            if (state == ChatRoom.State.Created) {
                contactLookup()
                updateSecurityIcon()
                subject.value = chatRoom.subject
            }
        }

        override fun onSubjectChanged(chatRoom: ChatRoom, eventLog: EventLog) {
            subject.value = chatRoom.subject
        }

        override fun onChatMessageReceived(chatRoom: ChatRoom, eventLog: EventLog) {
            unreadMessagesCount.value = chatRoom.unreadMessagesCount
            lastMessageText.value = formatLastMessage(eventLog.chatMessage)
        }

        override fun onChatMessageSent(chatRoom: ChatRoom, eventLog: EventLog) {
            lastMessageText.value = formatLastMessage(eventLog.chatMessage)
        }

        override fun onParticipantAdded(chatRoom: ChatRoom, eventLog: EventLog) {
            contactLookup()
            updateSecurityIcon()
        }

        override fun onParticipantRemoved(chatRoom: ChatRoom, eventLog: EventLog) {
            contactLookup()
            updateSecurityIcon()
        }

        override fun onIsComposingReceived(
            chatRoom: ChatRoom,
            remoteAddr: Address,
            isComposing: Boolean
        ) {
            updateRemotesComposing()
        }

        override fun onConferenceJoined(chatRoom: ChatRoom, eventLog: EventLog) {
            contactLookup()
            updateSecurityIcon()
            subject.value = chatRoom.subject
        }

        override fun onSecurityEvent(chatRoom: ChatRoom, eventLog: EventLog) {
            updateSecurityIcon()
        }

        override fun onParticipantDeviceAdded(chatRoom: ChatRoom, eventLog: EventLog) {
            updateSecurityIcon()
        }

        override fun onParticipantDeviceRemoved(chatRoom: ChatRoom, eventLog: EventLog) {
            updateSecurityIcon()
        }

        override fun onEphemeralMessageDeleted(chatRoom: ChatRoom, eventLog: EventLog) {
            Log.i("[Chat Room] Ephemeral message deleted, updated last message displayed")
            lastMessageText.value = formatLastMessage(chatRoom.lastMessageInHistory)
        }
    }

    init {
        chatRoom.core.addListener(coreListener)
        chatRoom.addListener(chatRoomListener)
        coreContext.contactsManager.addListener(contactsUpdatedListener)

        lastMessageText.value = formatLastMessage(chatRoom.lastMessageInHistory)
        unreadMessagesCount.value = chatRoom.unreadMessagesCount
        lastUpdate.value = TimestampUtils.toString(chatRoom.lastUpdateTime, true)

        subject.value = chatRoom.subject
        updateSecurityIcon()

        contactLookup()

        callInProgress.value = chatRoom.core.callsNb > 0
        updateRemotesComposing()
    }

    override fun onCleared() {
        coreContext.contactsManager.removeListener(contactsUpdatedListener)
        chatRoom.removeListener(chatRoomListener)
        chatRoom.core.removeListener(coreListener)

        super.onCleared()
    }

    fun contactLookup() {
        if (chatRoom.hasCapability(ChatRoomCapabilities.OneToOne.toInt())) {
            searchMatchingContact()
        } else {
            getParticipantsNames()
        }
    }

    private fun formatLastMessage(msg: ChatMessage?): String {
        if (msg == null) return ""

        val account = coreContext.core.accountList.find { account ->
            account.params.identityAddress?.asStringUriOnly() == msg.fromAddress.asStringUriOnly()
        }
        val localDisplayName = account?.params?.identityAddress?.displayName

        val sender: String =
            if (msg.isOutgoing && localDisplayName != null) {
                localDisplayName
            } else {
                coreContext.contactsManager.findContactByAddress(msg.fromAddress)?.fullName
                    ?: LinphoneUtils.getDisplayName(msg.fromAddress)
            }
        var body = ""
        for (content in msg.contents) {
            if (content.isFile || content.isFileTransfer) body += content.name + " "
            else if (content.isText) body += content.utf8Text + " "
        }

        return "$sender: $body"
    }

    private fun searchMatchingContact() {
        val remoteAddress = if (chatRoom.hasCapability(ChatRoomCapabilities.Basic.toInt())) {
            chatRoom.peerAddress
        } else {
            if (chatRoom.participants.isNotEmpty()) {
                chatRoom.participants[0].address
            } else {
                Log.e("[Chat Room] $chatRoom doesn't have any participant in state ${chatRoom.state}!")
                return
            }
        }
        contact.value = coreContext.contactsManager.findContactByAddress(remoteAddress)
    }

    private fun getParticipantsNames() {
        if (oneToOneChatRoom) return

        var participantsList = ""
        var index = 0
        for (participant in chatRoom.participants) {
            val contact: Contact? =
                coreContext.contactsManager.findContactByAddress(participant.address)
            participantsList += contact?.fullName ?: LinphoneUtils.getDisplayName(participant.address)
            index++
            if (index != chatRoom.nbParticipants) participantsList += ", "
        }
        participants.value = participantsList
    }

    private fun updateSecurityIcon() {
        securityLevelIcon.value = when (chatRoom.securityLevel) {
            ChatRoomSecurityLevel.Safe -> R.drawable.security_2_indicator
            ChatRoomSecurityLevel.Encrypted -> R.drawable.security_1_indicator
            else -> R.drawable.security_alert_indicator
        }
        securityLevelContentDescription.value = when (chatRoom.securityLevel) {
            ChatRoomSecurityLevel.Safe -> R.string.content_description_security_level_safe
            ChatRoomSecurityLevel.Encrypted -> R.string.content_description_security_level_encrypted
            else -> R.string.content_description_security_level_unsafe
        }
    }

    private fun updateRemotesComposing() {
        remoteIsComposing.value = chatRoom.isRemoteComposing

        var composing = ""
        for (address in chatRoom.composingAddresses) {
            val contact: Contact? = coreContext.contactsManager.findContactByAddress(address)
            composing += if (composing.isNotEmpty()) ", " else ""
            composing += contact?.fullName ?: LinphoneUtils.getDisplayName(address)
        }
        composingList.value = AppUtils.getStringWithPlural(R.plurals.chat_room_remote_composing, chatRoom.composingAddresses.size, composing)
    }
}
