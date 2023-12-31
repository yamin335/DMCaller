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
package com.bdcom.appdialer.activities.main.recordings.fragments

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bdcom.appdialer.R
import com.bdcom.appdialer.activities.main.fragments.MasterFragment
import com.bdcom.appdialer.activities.main.recordings.adapters.RecordingsListAdapter
import com.bdcom.appdialer.activities.main.recordings.data.RecordingData
import com.bdcom.appdialer.activities.main.recordings.viewmodels.RecordingsViewModel
import com.bdcom.appdialer.databinding.RecordingsFragmentBinding
import com.bdcom.appdialer.utils.RecyclerViewHeaderDecoration

class RecordingsFragment : MasterFragment<RecordingsFragmentBinding, RecordingsListAdapter>() {
    private lateinit var viewModel: RecordingsViewModel

    private var videoX: Float = 0f
    private var videoY: Float = 0f

    override fun getLayoutId(): Int = R.layout.recordings_fragment

    override fun onDestroyView() {
        binding.recordingsList.adapter = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this

        viewModel = ViewModelProvider(this).get(RecordingsViewModel::class.java)
        binding.viewModel = viewModel

        _adapter = RecordingsListAdapter(listSelectionViewModel, viewLifecycleOwner)
        binding.recordingsList.adapter = adapter

        val layoutManager = LinearLayoutManager(activity)
        binding.recordingsList.layoutManager = layoutManager

        // Divider between items
        val dividerItemDecoration = DividerItemDecoration(context, layoutManager.orientation)
        dividerItemDecoration.setDrawable(resources.getDrawable(R.drawable.divider, null))
        binding.recordingsList.addItemDecoration(dividerItemDecoration)

        // Displays the first letter header
        val headerItemDecoration = RecyclerViewHeaderDecoration(adapter)
        binding.recordingsList.addItemDecoration(headerItemDecoration)

        viewModel.recordingsList.observe(viewLifecycleOwner, { recordings ->
            adapter.submitList(recordings)
        })

        binding.setBackClickListener { findNavController().popBackStack() }

        binding.setEditClickListener { listSelectionViewModel.isEditionEnabled.value = true }

        binding.setVideoTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    videoX = v.x - event.rawX
                    videoY = v.y - event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    v.animate()
                        .x(event.rawX + videoX)
                        .y(event.rawY + videoY)
                        .setDuration(0)
                        .start()
                }
                else -> {
                    v.performClick()
                    false
                }
            }
            true
        }

        adapter.isVideoRecordingPlayingEvent.observe(viewLifecycleOwner, {
            it.consume { value ->
                viewModel.isVideoVisible.value = value
            }
        })

        adapter.setVideoTextureView(binding.recordingVideoSurface)
    }

    override fun deleteItems(indexesOfItemToDelete: ArrayList<Int>) {
        val list = ArrayList<RecordingData>()
        for (index in indexesOfItemToDelete) {
            val recording = adapter.currentList[index]
            list.add(recording)
        }
        viewModel.deleteRecordings(list)
    }
}
