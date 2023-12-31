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
package com.bdcom.appdialer.activities.main.history.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bdcom.appdialer.R
import com.bdcom.appdialer.activities.main.adapters.SelectionListAdapter
import com.bdcom.appdialer.activities.main.history.data.GroupedCallLogData
import com.bdcom.appdialer.activities.main.history.viewmodels.CallLogViewModel
import com.bdcom.appdialer.activities.main.viewmodels.ListTopBarViewModel
import com.bdcom.appdialer.databinding.GenericListHeaderBinding
import com.bdcom.appdialer.databinding.HistoryListCellBinding
import com.bdcom.appdialer.utils.Event
import com.bdcom.appdialer.utils.HeaderAdapter
import com.bdcom.appdialer.utils.TimestampUtils
import org.linphone.core.Address

class CallLogsListAdapter(
    selectionVM: ListTopBarViewModel,
    private val viewLifecycleOwner: LifecycleOwner
) : SelectionListAdapter<GroupedCallLogData, RecyclerView.ViewHolder>(selectionVM, CallLogDiffCallback()),
    HeaderAdapter {
    val selectedCallLogEvent: MutableLiveData<Event<GroupedCallLogData>> by lazy {
        MutableLiveData<Event<GroupedCallLogData>>()
    }

    val startCallToEvent: MutableLiveData<Event<Address>> by lazy {
        MutableLiveData<Event<Address>>()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding: HistoryListCellBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.history_list_cell, parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).bind(getItem(position))
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        (holder as ViewHolder).binding.viewModel?.destroy()
    }

    inner class ViewHolder(
        val binding: HistoryListCellBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(callLogGroup: GroupedCallLogData) {
            with(binding) {
                val callLogViewModel = CallLogViewModel(callLogGroup.lastCallLog)
                viewModel = callLogViewModel

                lifecycleOwner = viewLifecycleOwner

                // This is for item selection through ListTopBarFragment
                selectionListViewModel = selectionViewModel
                selectionViewModel.isEditionEnabled.observe(viewLifecycleOwner, {
                    position = adapterPosition
                })

                setClickListener {
                    if (selectionViewModel.isEditionEnabled.value == true) {
                        selectionViewModel.onToggleSelect(adapterPosition)
                    } else {
                        startCallToEvent.value = Event(callLogGroup.lastCallLog.remoteAddress)
                    }
                }

                setLongClickListener {
                    if (selectionViewModel.isEditionEnabled.value == false) {
                        selectionViewModel.isEditionEnabled.value = true
                        // Selection will be handled by click listener
                        true
                    }
                    false
                }

                // This listener is disabled when in edition mode
                setDetailsClickListener {
                    selectedCallLogEvent.value = Event(callLogGroup)
                }

                groupCount = callLogGroup.callLogs.size

                executePendingBindings()
            }
        }
    }

    override fun displayHeaderForPosition(position: Int): Boolean {
        if (position >= itemCount) return false
        val callLogGroup = getItem(position)
        val date = callLogGroup.lastCallLog.startDate
        val previousPosition = position - 1
        return if (previousPosition >= 0) {
            val previousItemDate = getItem(previousPosition).lastCallLog.startDate
            !TimestampUtils.isSameDay(date, previousItemDate)
        } else true
    }

    override fun getHeaderViewForPosition(context: Context, position: Int): View {
        val callLog = getItem(position)
        val date = formatDate(context, callLog.lastCallLog.startDate)
        val binding: GenericListHeaderBinding = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            R.layout.generic_list_header, null, false
        )
        binding.title = date
        binding.executePendingBindings()
        return binding.root
    }

    private fun formatDate(context: Context, date: Long): String {
        if (TimestampUtils.isToday(date)) {
            return context.getString(R.string.today)
        } else if (TimestampUtils.isYesterday(date)) {
            return context.getString(R.string.yesterday)
        }
        return TimestampUtils.toString(date, onlyDate = true, shortDate = false, hideYear = false)
    }
}

private class CallLogDiffCallback : DiffUtil.ItemCallback<GroupedCallLogData>() {
    override fun areItemsTheSame(
        oldItem: GroupedCallLogData,
        newItem: GroupedCallLogData
    ): Boolean {
        return oldItem.lastCallLog.callId == newItem.lastCallLog.callId
    }

    override fun areContentsTheSame(
        oldItem: GroupedCallLogData,
        newItem: GroupedCallLogData
    ): Boolean {
        return false // For headers
    }
}
