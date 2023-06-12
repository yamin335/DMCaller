package com.bdcom.appdialer.fragments

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import com.bdcom.appdialer.utils.AndroidUtil

class ContactsPopupFragment : ContactsFragment(), AdapterView.OnItemClickListener {

    companion object {
        fun newInstance(instance: Int): ContactsPopupFragment {
            val args = Bundle()
            args.putInt(ARGS_INSTANCE, instance)
            val fragment = ContactsPopupFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun showDuplicate(): Boolean {
        return true
    }

    override fun showNumberInList(): Boolean {
        return true
    }

    override fun onItemClick(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
        AndroidUtil.hideKeyboard(requireActivity())
        NewConferenceFragment.updateParticipantList(list[i])
        mFragmentNavigation.popFragment()
    }
}
