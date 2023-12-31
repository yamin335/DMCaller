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
package com.bdcom.appdialer.activities.main.history.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bdcom.appdialer.LinphoneApplication.Companion.coreContext
import com.bdcom.appdialer.activities.main.history.data.GroupedCallLogData
import com.bdcom.appdialer.contact.ContactsUpdatedListenerStub
import com.bdcom.appdialer.utils.Event
import com.bdcom.appdialer.utils.LinphoneUtils
import com.bdcom.appdialer.utils.TimestampUtils
import org.linphone.core.*
import org.linphone.core.tools.Log

class CallLogsListViewModel : ViewModel() {
    val callLogs = MutableLiveData<ArrayList<GroupedCallLogData>>()
    val missedCallLogs = MutableLiveData<ArrayList<GroupedCallLogData>>()

    val missedCallLogsSelected = MutableLiveData<Boolean>()

    val contactsUpdatedEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    private val listener: CoreListenerStub = object : CoreListenerStub() {
        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State,
            message: String
        ) {
            updateCallLogs()
        }
    }

    private val contactsUpdatedListener = object : ContactsUpdatedListenerStub() {
        override fun onContactsUpdated() {
            Log.i("[Call Logs] Contacts have changed")
            contactsUpdatedEvent.value = Event(true)
        }
    }

    init {
        missedCallLogsSelected.value = false
        updateCallLogs()

        coreContext.core.addListener(listener)
        coreContext.contactsManager.addListener(contactsUpdatedListener)
    }

    override fun onCleared() {
        coreContext.contactsManager.removeListener(contactsUpdatedListener)
        coreContext.core.removeListener(listener)

        super.onCleared()
    }

    fun deleteCallLogGroup(callLog: GroupedCallLogData?) {
        if (callLog != null) {
            for (log in callLog.callLogs) {
                coreContext.core.removeCallLog(log)
            }
        }

        updateCallLogs()
    }

    fun deleteCallLogGroups(listToDelete: ArrayList<GroupedCallLogData>) {
        for (callLog in listToDelete) {
            for (log in callLog.callLogs) {
                coreContext.core.removeCallLog(log)
            }
        }

        updateCallLogs()
    }

    private fun updateCallLogs() {
        val list = arrayListOf<GroupedCallLogData>()
        val missedList = arrayListOf<GroupedCallLogData>()

        var previousCallLogGroup: GroupedCallLogData? = null
        var previousMissedCallLogGroup: GroupedCallLogData? = null
        for (callLog in coreContext.core.callLogs) {
            if (previousCallLogGroup == null) {
                previousCallLogGroup = GroupedCallLogData(callLog)
            } else if (previousCallLogGroup.lastCallLog.localAddress.weakEqual(callLog.localAddress) && previousCallLogGroup.lastCallLog.remoteAddress.weakEqual(callLog.remoteAddress)) {
                if (TimestampUtils.isSameDay(previousCallLogGroup.lastCallLog.startDate, callLog.startDate)) {
                    previousCallLogGroup.callLogs.add(callLog)
                    previousCallLogGroup.lastCallLog = callLog
                } else {
                    list.add(previousCallLogGroup)
                    previousCallLogGroup = GroupedCallLogData(callLog)
                }
            } else {
                list.add(previousCallLogGroup)
                previousCallLogGroup = GroupedCallLogData(callLog)
            }

            if (LinphoneUtils.isCallLogMissed(callLog)) {
                if (previousMissedCallLogGroup == null) {
                    previousMissedCallLogGroup = GroupedCallLogData(callLog)
                } else if (previousMissedCallLogGroup.lastCallLog.localAddress.weakEqual(callLog.localAddress) && previousMissedCallLogGroup.lastCallLog.remoteAddress.weakEqual(callLog.remoteAddress)) {
                    if (TimestampUtils.isSameDay(previousMissedCallLogGroup.lastCallLog.startDate, callLog.startDate)) {
                        previousMissedCallLogGroup.callLogs.add(callLog)
                        previousMissedCallLogGroup.lastCallLog = callLog
                    } else {
                        missedList.add(previousMissedCallLogGroup)
                        previousMissedCallLogGroup = GroupedCallLogData(callLog)
                    }
                } else {
                    missedList.add(previousMissedCallLogGroup)
                    previousMissedCallLogGroup = GroupedCallLogData(callLog)
                }
            }
        }

        if (previousCallLogGroup != null && !list.contains(previousCallLogGroup)) {
            list.add(previousCallLogGroup)
        }
        if (previousMissedCallLogGroup != null && !missedList.contains(previousMissedCallLogGroup)) {
            missedList.add(previousMissedCallLogGroup)
        }

        callLogs.value = list
        missedCallLogs.value = missedList
    }
}
