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
package com.bdcom.appdialer.activities.call.fragments

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.ViewModelProvider
import com.bdcom.appdialer.LinphoneApplication.Companion.coreContext
import com.bdcom.appdialer.LinphoneApplication.Companion.corePreferences
import com.bdcom.appdialer.R
import com.bdcom.appdialer.activities.BottomTabsActivity
import com.bdcom.appdialer.activities.GenericFragment
import com.bdcom.appdialer.activities.call.viewmodels.CallsViewModel
import com.bdcom.appdialer.activities.call.viewmodels.ConferenceViewModel
import com.bdcom.appdialer.activities.call.viewmodels.ControlsViewModel
import com.bdcom.appdialer.activities.call.viewmodels.SharedCallViewModel
import com.bdcom.appdialer.activities.main.MainActivity
import com.bdcom.appdialer.activities.main.viewmodels.DialogViewModel
import com.bdcom.appdialer.databinding.CallControlsFragmentBinding
import com.bdcom.appdialer.utils.*
import org.linphone.core.Call
import org.linphone.core.tools.Log
import org.linphone.mediastream.Version

class ControlsFragment : GenericFragment<CallControlsFragmentBinding>() {
    private lateinit var callsViewModel: CallsViewModel
    private lateinit var controlsViewModel: ControlsViewModel
    private lateinit var conferenceViewModel: ConferenceViewModel
    private lateinit var sharedViewModel: SharedCallViewModel

    private var dialog: Dialog? = null

    override fun getLayoutId(): Int = R.layout.call_controls_fragment

    // We have to use lateinit here because we need to compute the screen width first
    private lateinit var numpadAnimator: ValueAnimator

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this

        sharedViewModel = requireActivity().run {
            ViewModelProvider(this).get(SharedCallViewModel::class.java)
        }

        callsViewModel = ViewModelProvider(this).get(CallsViewModel::class.java)
        binding.viewModel = callsViewModel

        controlsViewModel = ViewModelProvider(this).get(ControlsViewModel::class.java)
        binding.controlsViewModel = controlsViewModel

        conferenceViewModel = ViewModelProvider(this).get(ConferenceViewModel::class.java)
        binding.conferenceViewModel = conferenceViewModel

        callsViewModel.currentCallViewModel.observe(viewLifecycleOwner, {
            if (it != null) {
                binding.activeCallTimer.base =
                    SystemClock.elapsedRealtime() - (1000 * it.call.duration) // Linphone timestamps are in seconds
                binding.activeCallTimer.start()
            }
        })

        callsViewModel.noMoreCallEvent.observe(viewLifecycleOwner, {
            it.consume {
                requireActivity().finishOpeningBottomTabActivity()
            }
        })

        callsViewModel.askWriteExternalStoragePermissionEvent.observe(viewLifecycleOwner, {
            it.consume {
                if (!PermissionHelper.get().hasWriteExternalStorage()) {
                    Log.i("[Controls Fragment] Asking for WRITE_EXTERNAL_STORAGE permission")
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                }
            }
        })

        callsViewModel.callUpdateEvent.observe(viewLifecycleOwner, {
            it.consume { call ->
                if (call.state == Call.State.StreamsRunning) {
                    dialog?.dismiss()
                } else if (call.state == Call.State.UpdatedByRemote) {
                    if (call.currentParams.videoEnabled() != call.remoteParams?.videoEnabled()) {
                        showCallVideoUpdateDialog(call)
                    }
                }
            }
        })

        controlsViewModel.chatClickedEvent.observe(viewLifecycleOwner, {
            it.consume {
                val intent = Intent()
                intent.setClass(requireContext(), MainActivity::class.java)
                intent.putExtra("Chat", true)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        })

        controlsViewModel.addCallClickedEvent.observe(viewLifecycleOwner, {
            it.consume {
                val intent = Intent()
                intent.setClass(requireContext(), BottomTabsActivity::class.java)
                AppUtils.isConference = true
                intent.putExtra("Dialer", true)
                intent.putExtra("Transfer", false)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        })

        controlsViewModel.transferCallClickedEvent.observe(viewLifecycleOwner, {
            it.consume {
                val intent = Intent()
                intent.setClass(requireContext(), MainActivity::class.java)
                intent.putExtra("Dialer", true)
                intent.putExtra("Transfer", true)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        })

        controlsViewModel.askPermissionEvent.observe(viewLifecycleOwner, {
            it.consume { permission ->
                Log.i("[Controls Fragment] Asking for $permission permission")
                requestPermissions(arrayOf(permission), 0)
            }
        })

        controlsViewModel.toggleNumpadEvent.observe(viewLifecycleOwner, {
            it.consume { open ->
                if (this::numpadAnimator.isInitialized) {
                    if (open) {
                        numpadAnimator.start()
                    } else {
                        numpadAnimator.reverse()
                    }
                }
            }
        })

        controlsViewModel.somethingClickedEvent.observe(viewLifecycleOwner, {
            it.consume {
                sharedViewModel.resetHiddenInterfaceTimerInVideoCallEvent.value = Event(true)
            }
        })

        if (Version.sdkAboveOrEqual(Version.API23_MARSHMALLOW_60)) {
            checkPermissions()
        }
    }

    override fun onStart() {
        super.onStart()

        initNumpadLayout()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 0) {
            for (i in permissions.indices) {
                when (permissions[i]) {
                    Manifest.permission.RECORD_AUDIO -> if (grantResults[i] == PERMISSION_GRANTED) {
                        Log.i("[Controls Fragment] RECORD_AUDIO permission has been granted")
                        controlsViewModel.updateMuteMicState()
                    }
                    Manifest.permission.CAMERA -> if (grantResults[i] == PERMISSION_GRANTED) {
                        Log.i("[Controls Fragment] CAMERA permission has been granted")
                        coreContext.core.reloadVideoDevices()
                    }
                }
            }
        } else if (requestCode == 1 && grantResults[0] == PERMISSION_GRANTED) {
            callsViewModel.takeScreenshot()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @TargetApi(Version.API23_MARSHMALLOW_60)
    private fun checkPermissions() {
        val permissionsRequiredList = arrayListOf<String>()

        if (!PermissionHelper.get().hasRecordAudioPermission()) {
            Log.i("[Controls Fragment] Asking for RECORD_AUDIO permission")
            permissionsRequiredList.add(Manifest.permission.RECORD_AUDIO)
        }

        if (coreContext.isVideoCallOrConferenceActive() && !PermissionHelper.get().hasCameraPermission()) {
            Log.i("[Controls Fragment] Asking for CAMERA permission")
            permissionsRequiredList.add(Manifest.permission.CAMERA)
        }

        if (permissionsRequiredList.isNotEmpty()) {
            val permissionsRequired = arrayOfNulls<String>(permissionsRequiredList.size)
            permissionsRequiredList.toArray(permissionsRequired)
            requestPermissions(permissionsRequired, 0)
        }
    }

    private fun showCallVideoUpdateDialog(call: Call) {
        val viewModel = DialogViewModel(AppUtils.getString(R.string.call_video_update_requested_dialog))
        dialog = DialogUtils.getDialog(requireContext(), viewModel)

        viewModel.showCancelButton({
            callsViewModel.answerCallVideoUpdateRequest(call, false)
            dialog?.dismiss()
        }, getString(R.string.dialog_decline))

        viewModel.showOkButton({
            callsViewModel.answerCallVideoUpdateRequest(call, true)
            dialog?.dismiss()
        }, getString(R.string.dialog_accept))

        dialog?.show()
    }

    private fun initNumpadLayout() {
        val screenWidth = coreContext.screenWidth
        numpadAnimator = ValueAnimator.ofFloat(screenWidth, 0f).apply {
            addUpdateListener {
                val value = it.animatedValue as Float
                view?.findViewById<LinearLayout>(R.id.numpad)?.translationX = -value
                duration = if (corePreferences.enableAnimations) 500 else 0
            }
        }
        // Hide the numpad here as we can't set the translationX property on include tag in layout
        if (controlsViewModel.numpadVisibility.value == false) {
            view?.findViewById<LinearLayout>(R.id.numpad)?.translationX = -screenWidth
        }
    }
}
