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
package com.bdcom.appdialer.activities.main.chat.data

import android.os.CountDownTimer
import android.text.Spannable
import android.text.util.Linkify
import androidx.core.text.util.LinkifyCompat
import androidx.lifecycle.MutableLiveData
import com.bdcom.appdialer.LinphoneApplication.Companion.coreContext
import com.bdcom.appdialer.R
import com.bdcom.appdialer.contact.GenericContactData
import com.bdcom.appdialer.utils.AppUtils
import com.bdcom.appdialer.utils.TimestampUtils
import org.linphone.core.ChatMessage
import org.linphone.core.ChatMessageListenerStub
import org.linphone.core.Content
import org.linphone.core.tools.Log

class ChatMessageData(
    val chatMessage: ChatMessage,
    private var contentListener: OnContentClickedListener? = null
) : GenericContactData(chatMessage.fromAddress) {
    val sendInProgress = MutableLiveData<Boolean>()

    val transferInProgress = MutableLiveData<Boolean>()

    val showImdn = MutableLiveData<Boolean>()

    val imdnIcon = MutableLiveData<Int>()

    val backgroundRes = MutableLiveData<Int>()

    val hideAvatar = MutableLiveData<Boolean>()

    val hideTime = MutableLiveData<Boolean>()

    val contents = MutableLiveData<ArrayList<ChatMessageContentData>>()

    val time = MutableLiveData<String>()

    val ephemeralLifetime = MutableLiveData<String>()

    val text = MutableLiveData<Spannable>()

    private var countDownTimer: CountDownTimer? = null

    private val listener = object : ChatMessageListenerStub() {
        override fun onMsgStateChanged(message: ChatMessage, state: ChatMessage.State) {
            time.value = TimestampUtils.toString(chatMessage.time)
            updateChatMessageState(state)

            // TODO FIXME : find a way to refresh outgoing message downloaded
            if (state == ChatMessage.State.FileTransferDone && !message.isOutgoing) {
                Log.i("[Chat Message] File transfer done")
                updateContentsList()

                coreContext.exportFilesInMessageToMediaStore(message)
            }
        }

        override fun onEphemeralMessageTimerStarted(message: ChatMessage) {
            updateEphemeralTimer()
        }
    }

    init {
        chatMessage.addListener(listener)

        backgroundRes.value = if (chatMessage.isOutgoing) R.drawable.chat_bubble_outgoing_full else R.drawable.chat_bubble_incoming_full
        hideAvatar.value = false
        time.value = TimestampUtils.toString(chatMessage.time)
        updateEphemeralTimer()

        updateChatMessageState(chatMessage.state)
        updateContentsList()
    }

    override fun destroy() {
        super.destroy()

        contents.value.orEmpty().forEach(ChatMessageContentData::destroy)
        chatMessage.removeListener(listener)
        contentListener = null
    }

    fun updateBubbleBackground(hasPrevious: Boolean, hasNext: Boolean) {
        if (hasPrevious) {
            hideTime.value = true
        }

        if (chatMessage.isOutgoing) {
            if (hasNext && hasPrevious) {
                backgroundRes.value = R.drawable.chat_bubble_outgoing_split_2
            } else if (hasNext) {
                backgroundRes.value = R.drawable.chat_bubble_outgoing_split_1
            } else if (hasPrevious) {
                backgroundRes.value = R.drawable.chat_bubble_outgoing_split_3
            } else {
                backgroundRes.value = R.drawable.chat_bubble_outgoing_full
            }
        } else {
            if (hasNext && hasPrevious) {
                hideAvatar.value = true
                backgroundRes.value = R.drawable.chat_bubble_incoming_split_2
            } else if (hasNext) {
                backgroundRes.value = R.drawable.chat_bubble_incoming_split_1
            } else if (hasPrevious) {
                hideAvatar.value = true
                backgroundRes.value = R.drawable.chat_bubble_incoming_split_3
            } else {
                backgroundRes.value = R.drawable.chat_bubble_incoming_full
            }
        }
    }

    private fun updateChatMessageState(state: ChatMessage.State) {
        transferInProgress.value = state == ChatMessage.State.FileTransferInProgress

        sendInProgress.value = state == ChatMessage.State.InProgress || state == ChatMessage.State.FileTransferInProgress

        showImdn.value = when (state) {
            ChatMessage.State.DeliveredToUser, ChatMessage.State.Displayed, ChatMessage.State.NotDelivered -> true
            else -> false
        }

        imdnIcon.value = when (state) {
            ChatMessage.State.DeliveredToUser -> R.drawable.chat_delivered
            ChatMessage.State.Displayed -> R.drawable.chat_read
            else -> R.drawable.chat_error
        }
    }

    private fun updateContentsList() {
        contents.value.orEmpty().forEach(ChatMessageContentData::destroy)

        val list = arrayListOf<ChatMessageContentData>()
        for (content in chatMessage.contents) {
            if (content.isFileTransfer || content.isFile) {
                list.add(ChatMessageContentData(content, chatMessage, contentListener))
            } else if (content.isText) {
                val spannable = Spannable.Factory.getInstance().newSpannable(content.utf8Text)
                LinkifyCompat.addLinks(spannable, Linkify.WEB_URLS)
                text.value = spannable
            }
        }

        contents.value = list
    }

    private fun updateEphemeralTimer() {
        if (chatMessage.isEphemeral) {
            if (chatMessage.ephemeralExpireTime == 0L) {
                // This means the message hasn't been read by all participants yet, so the countdown hasn't started
                // In this case we simply display the configured value for lifetime
                ephemeralLifetime.value = formatLifetime(chatMessage.ephemeralLifetime)
            } else {
                // Countdown has started, display remaining time
                val remaining = chatMessage.ephemeralExpireTime - (System.currentTimeMillis() / 1000)
                ephemeralLifetime.value = formatLifetime(remaining)
                if (countDownTimer == null) {
                    countDownTimer = object : CountDownTimer(remaining * 1000, 1000) {
                        override fun onFinish() {}

                        override fun onTick(millisUntilFinished: Long) {
                            ephemeralLifetime.postValue(formatLifetime(millisUntilFinished / 1000))
                        }
                    }
                    countDownTimer?.start()
                }
            }
        }
    }

    private fun formatLifetime(seconds: Long): String {
        val days = seconds / 86400
        return when {
            days >= 1L -> AppUtils.getStringWithPlural(R.plurals.days, days.toInt())
            else -> String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60))
        }
    }

    private fun addContentToMediaStore(content: Content) {
        coreContext.addContentToMediaStore(content)
    }
}
