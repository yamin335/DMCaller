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
package com.bdcom.appdialer.activities.main.sidemenu.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bdcom.appdialer.LinphoneApplication.Companion.coreContext
import com.bdcom.appdialer.LinphoneApplication.Companion.corePreferences
import com.bdcom.appdialer.activities.main.settings.SettingListenerStub
import com.bdcom.appdialer.activities.main.settings.viewmodels.AccountSettingsViewModel
import org.linphone.core.*

class SideMenuViewModel : ViewModel() {
    val showAccounts: Boolean = corePreferences.showAccountsInSideMenu
    val showAssistant: Boolean = corePreferences.showAssistantInSideMenu
    val showSettings: Boolean = corePreferences.showSettingsInSideMenu
    val showRecordings: Boolean = corePreferences.showRecordingsInSideMenu
    val showAbout: Boolean = corePreferences.showAboutInSideMenu
    val showQuit: Boolean = corePreferences.showQuitInSideMenu

    val defaultAccountViewModel = MutableLiveData<AccountSettingsViewModel>()
    val defaultAccountFound = MutableLiveData<Boolean>()
    val defaultAccountAvatar = MutableLiveData<String>()

    val accounts = MutableLiveData<ArrayList<AccountSettingsViewModel>>()

    lateinit var accountsSettingsListener: SettingListenerStub

    private var accountClickListener = object : SettingListenerStub() {
        override fun onAccountClicked(identity: String) {
            accountsSettingsListener.onAccountClicked(identity)
        }
    }

    private val listener: CoreListenerStub = object : CoreListenerStub() {
        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState,
            message: String
        ) {
            if (coreContext.core.accountList.size != accounts.value?.size) {
                // Only refresh the list if an account has been added or removed
                updateAccountsList()
            }
        }
    }

    init {
        defaultAccountFound.value = false
        defaultAccountAvatar.value = corePreferences.defaultAccountAvatarPath
        coreContext.core.addListener(listener)
        updateAccountsList()
    }

    override fun onCleared() {
        coreContext.core.removeListener(listener)
        super.onCleared()
    }

    fun updateAccountsList() {
        defaultAccountFound.value = false // Do not assume a default account will still be found
        val list = arrayListOf<AccountSettingsViewModel>()
        if (coreContext.core.accountList.isNotEmpty()) {
            val defaultAccount = coreContext.core.defaultAccount
            if (defaultAccount != null) {
                val defaultViewModel = AccountSettingsViewModel(defaultAccount)
                defaultViewModel.accountsSettingsListener = accountClickListener
                defaultAccountViewModel.value = defaultViewModel
                defaultAccountFound.value = true
            }

            for (account in coreContext.core.accountList) {
                if (account != coreContext.core.defaultAccount) {
                    val viewModel = AccountSettingsViewModel(account)
                    viewModel.accountsSettingsListener = accountClickListener
                    list.add(viewModel)
                }
            }
        }
        accounts.value = list
    }

    fun setPictureFromPath(picturePath: String) {
        corePreferences.defaultAccountAvatarPath = picturePath
        defaultAccountAvatar.value = corePreferences.defaultAccountAvatarPath
    }
}
