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
package com.bdcom.appdialer.activities.call.views

import android.content.Context
import android.os.SystemClock
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.databinding.DataBindingUtil
import com.bdcom.appdialer.R
import com.bdcom.appdialer.activities.call.data.ConferenceParticipantData
import com.bdcom.appdialer.databinding.CallConferenceParticipantBinding
import org.linphone.core.tools.Log

class ConferenceParticipantView : LinearLayout {
    private lateinit var binding: CallConferenceParticipantBinding

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(
        context,
        attrs
    ) {
        init(context)
    }

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    fun init(context: Context) {
        binding = DataBindingUtil.inflate(
            LayoutInflater.from(context), R.layout.call_conference_participant, this, true
        )
    }

    fun setData(data: ConferenceParticipantData) {
        binding.data = data

        val currentTimeSecs = System.currentTimeMillis()
        val participantTime = data.participant.creationTime * 1000 // Linphone timestamps are in seconds
        val diff = currentTimeSecs - participantTime
        Log.i("[Conference Participant] Participant joined conference at $participantTime == ${diff / 1000} seconds ago.")
        binding.callTimer.base = SystemClock.elapsedRealtime() - diff
        binding.callTimer.start()
    }
}
