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
package com.bdcom.appdialer.activities.main.sidemenu.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.view.View
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bdcom.appdialer.LinphoneApplication.Companion.coreContext
import com.bdcom.appdialer.LinphoneApplication.Companion.corePreferences
import com.bdcom.appdialer.R
import com.bdcom.appdialer.activities.GenericFragment
import com.bdcom.appdialer.activities.assistant.AssistantActivity
import com.bdcom.appdialer.activities.main.settings.SettingListenerStub
import com.bdcom.appdialer.activities.main.sidemenu.viewmodels.SideMenuViewModel
import com.bdcom.appdialer.activities.main.viewmodels.SharedMainViewModel
import com.bdcom.appdialer.activities.navigateToAbout
import com.bdcom.appdialer.activities.navigateToAccountSettings
import com.bdcom.appdialer.activities.navigateToRecordings
import com.bdcom.appdialer.activities.navigateToSettings
import com.bdcom.appdialer.databinding.SideMenuFragmentBinding
import com.bdcom.appdialer.utils.Event
import com.bdcom.appdialer.utils.FileUtils
import com.bdcom.appdialer.utils.ImageUtils
import com.bdcom.appdialer.utils.PermissionHelper
import java.io.File
import kotlinx.coroutines.launch
import org.linphone.core.tools.Log

class SideMenuFragment : GenericFragment<SideMenuFragmentBinding>() {
    private lateinit var viewModel: SideMenuViewModel
    private lateinit var sharedViewModel: SharedMainViewModel
    private var temporaryPicturePath: File? = null

    override fun getLayoutId(): Int = R.layout.side_menu_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this

        viewModel = ViewModelProvider(this).get(SideMenuViewModel::class.java)
        binding.viewModel = viewModel

        sharedViewModel = requireActivity().run {
            ViewModelProvider(this).get(SharedMainViewModel::class.java)
        }

        sharedViewModel.accountRemoved.observe(viewLifecycleOwner, {
            Log.i("[Side Menu] Account removed, update accounts list")
            viewModel.updateAccountsList()
        })

        viewModel.accountsSettingsListener = object : SettingListenerStub() {
            override fun onAccountClicked(identity: String) {
                val args = Bundle()
                args.putString("Identity", identity)
                Log.i("[Side Menu] Navigation to settings for account with identity: $identity")

                sharedViewModel.toggleDrawerEvent.value = Event(true)
                navigateToAccountSettings(identity)
            }
        }

        binding.setSelfPictureClickListener {
            pickFile()
        }

        binding.setAssistantClickListener {
            sharedViewModel.toggleDrawerEvent.value = Event(true)
            startActivity(Intent(context, AssistantActivity::class.java))

            if (corePreferences.enableAnimations) {
                requireActivity().overridePendingTransition(R.anim.enter_right, R.anim.exit_left)
            }
        }

        binding.setSettingsClickListener {
            sharedViewModel.toggleDrawerEvent.value = Event(true)
            navigateToSettings()
        }

        binding.setRecordingsClickListener {
            sharedViewModel.toggleDrawerEvent.value = Event(true)
            navigateToRecordings()
        }

        binding.setAboutClickListener {
            sharedViewModel.toggleDrawerEvent.value = Event(true)
            navigateToAbout()
        }

        binding.setQuitClickListener {
            requireActivity().finishAndRemoveTask()
            coreContext.stop()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            lifecycleScope.launch {
                val contactImageFilePath = ImageUtils.getFilePathFromPickerIntent(data, temporaryPicturePath)
                if (contactImageFilePath != null) {
                    viewModel.setPictureFromPath(contactImageFilePath)
                }
            }
        }
    }

    private fun pickFile() {
        val cameraIntents = ArrayList<Intent>()

        // Handles image picking
        val galleryIntent = Intent(Intent.ACTION_PICK)
        galleryIntent.type = "image/*"

        if (PermissionHelper.get().hasCameraPermission()) {
            // Allows to capture directly from the camera
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val tempFileName = System.currentTimeMillis().toString() + ".jpeg"
            val file = FileUtils.getFileStoragePath(tempFileName)
            temporaryPicturePath = file
            val publicUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getString(R.string.file_provider),
                file
            )
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, publicUri)
            captureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            cameraIntents.add(captureIntent)
        }

        val chooserIntent =
            Intent.createChooser(galleryIntent, getString(R.string.chat_message_pick_file_dialog))
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(arrayOf<Parcelable>()))

        startActivityForResult(chooserIntent, 0)
    }
}
