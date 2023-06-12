package com.bdcom.appdialer.fragments

import android.os.Bundle
import android.view.*
import android.view.MenuInflater
import android.widget.Toast
import com.bdcom.appdialer.LinphoneApplication
import com.bdcom.appdialer.R
import com.bdcom.appdialer.models.PhoneContact

class NewConferenceFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_new_conference, container, false)

        val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView)
        recyclerView!!.layoutManager = androidx.recyclerview.widget.GridLayoutManager(context, 3)

        addHost()
        // Need_to_fix recyclerView.adapter = RecyclerViewAdapter(list, context!!)
        setHasOptionsMenu(true)
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.add_conference_contact, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.addContact) {
            mFragmentNavigation.pushFragment(ContactsPopupFragment.newInstance(0))
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        private val list = ArrayList<PhoneContact>()

        private fun addHost() {
            if (list.isEmpty()) {
                list.add(getHost())
            }
        }

        private fun getHost(): PhoneContact {
            val host = PhoneContact()
            host.name = "host"
            return host
        }

        @JvmStatic
        fun updateParticipantList(contact: PhoneContact) {
            if (list.isEmpty()) {
                list.add(getHost())
            } else {
                if (listContains(contact)) {
                    Toast.makeText(LinphoneApplication.getApplicationContext(), R.string.contact_already_exists_warning_message, Toast.LENGTH_SHORT).show()
                } else {
                    list.add(contact)
                }
            }
        }

        private fun listContains(contact: PhoneContact): Boolean {
            for (item in list) {
                if (item.name == contact.name && item.number == contact.number) {
                    return true
                }
            }
            return false
        }

        fun newInstance(instance: Int): NewConferenceFragment {
            val args = Bundle()
            args.putInt(BaseFragment.ARGS_INSTANCE, instance)
            val fragment = NewConferenceFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
