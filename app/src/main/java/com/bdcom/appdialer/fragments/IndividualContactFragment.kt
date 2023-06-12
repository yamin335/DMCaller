package com.bdcom.appdialer.fragments

import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.Contacts
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bdcom.appdialer.LinphoneApplication
import com.bdcom.appdialer.R
import com.bdcom.appdialer.adapters.IndividualContactAdapter
import com.bdcom.appdialer.models.PhoneContact
import com.bdcom.appdialer.utils.AndroidUtil
import com.bdcom.appdialer.utils.AppUtils
import com.bdcom.appdialer.utils.ImageUtil
import com.bdcom.appdialer.utils.MyPreference
import com.bdcom.appdialer.views.FavToogle
import com.google.android.gms.common.util.CollectionUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_individual_contacts_view.*

class IndividualContactFragment : BaseFragment(), AdapterView.OnItemClickListener {

    private var adapter: IndividualContactAdapter? = null

    private val list: ArrayList<PhoneContact> = ArrayList()

    protected var contact: Contacts? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        setHasOptionsMenu(true)
        val view = inflater.inflate(R.layout.fragment_individual_contacts_view, container, false)

        val tvContactName = view.findViewById<TextView>(R.id.tvContactName)
        val ivContactImage = view.findViewById<ImageView>(R.id.ivContactImage)
        val civContactImage = view.findViewById<ImageView>(R.id.civContactImage)

        // ivAddFavourite.setOnClickListener { toggleFavorite() }

//        ivAddFavourite.apply {
//            setImageDrawable(getStarDrawable(true))
//            //tag = contact!!.starred
//            //applyColorFilter(textColor)
//        }

        val name = contactName()
        if (name.isBlank()) {
            val number = contactNumber()
            tvContactName.text = number
        } else {
            tvContactName.text = name
        }

        val color = contactColor()

        var imageUri = Uri.parse(contactImage())

        if (imageUri.path != "null") {
            ivContactImage.visibility = View.GONE
            civContactImage.visibility = View.VISIBLE
            civContactImage.setImageURI(imageUri)
        } else {
            civContactImage.visibility = View.GONE
            ivContactImage.setImageDrawable(ImageUtil.circularImage(name, color))
        }

        if (!list.isEmpty()) {
            list.clear()
        }

        val listView = view.findViewById(R.id.listView) as ListView

        adapter = IndividualContactAdapter.newInstance(requireContext(), list)
        listView.adapter = adapter
        listView.onItemClickListener = this

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        ivAddFavourite.onToggleListener = object : FavToogle.OnToggleListener {
            override fun onToggle(isOn: Boolean) {
                var favouriteList = getFavouriteList()

                if (isOn) {
                    (favouriteList as ArrayList<Int>).add(getContactId())
                    MyPreference.with(context).addObject("favourite", favouriteList).save()
                    Snackbar.make(rootLayout, "Added to Favourites!", Snackbar.LENGTH_LONG).show()
                } else {
                    (favouriteList as ArrayList<Int>).removeAll { item -> item == getContactId() }
                    MyPreference.with(context).addObject("favourite", favouriteList).save()
                    Snackbar.make(rootLayout, "Removed from Favourites!", Snackbar.LENGTH_LONG).show()
                }
            }
        }

        if (isFavouriteContact(getContactId())) {
            ivAddFavourite.toggle()
        }
    }

    private fun isFavouriteContact(id: Int): Boolean {
        var favouriteList = getFavouriteList()
        if (CollectionUtils.isEmpty(favouriteList)) {
            return false
        }

        return favouriteList!!.count { item -> (item as Double).toInt() == id } > 0
    }

    private fun getFavouriteList(): java.util.ArrayList<*>? {
        var favouriteList = MyPreference.with(context).getObject("favourite", ArrayList::class.java)
        if (favouriteList == null) {
            favouriteList = ArrayList<Int>()
        }
        return favouriteList
    }

    override fun onResume() {
        super.onResume()
        readContacts()
    }

    override fun onItemClick(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
        val contact = list[i]
        when (view?.id) {
            R.id.ivCall -> {
                call(contact)
            }

//            R.id.ivMessage -> {}
//
//            R.id.ivMail -> {}
        }
    }

    private fun call(contact: PhoneContact) {
        var name = contact.name
        if (name.isNullOrEmpty()) {
            name = ""
        }
        val number = contact.number
        LinphoneApplication.instance.getDatabaseHelper().insertCallHistory(number!!, name, 1)
        // Fix CallActivity.start(requireContext(), name, number)
        AppUtils.directCall(number)
    }

    private fun contactColor(): Int {
        return this.requireArguments().getInt("color")
    }

    private fun contactImage(): String {
        return this.requireArguments().getString("image") ?: ""
    }

    private fun contactNumber(): String {
        return this.requireArguments().getString("number") ?: ""
    }

    private fun contactName(): String {
        val name = this.requireArguments().getString("name") ?: ""
        if (AndroidUtil.isValidPhoneNumber(name)) {
            return ""
        }
        return name
    }

    private fun getContactId(): Int {
        return this.requireArguments().getInt("id")
    }

    private fun readContacts() {
        val name = contactName()
        if (name.isBlank()) {
            if (!list.isEmpty()) {
                list.clear()
            }
            addDefaultContact()
            updateList()
        } else {
            readContacts(name)
        }
    }

    private fun readContacts(query: String) {
        val contacts = ContactsLoadTask(query).execute().get() as ArrayList<PhoneContact>
        updateList(contacts)
    }

    private class ContactsLoadTask(var query: String) : AsyncTask<Void, Void, List<PhoneContact>>() {
        override fun doInBackground(vararg params: Void?): List<PhoneContact>? {
            return readContactsFromDevice(query)
        }

        private fun readContactsFromDevice(query: String): List<PhoneContact> {
            val contacts: ArrayList<PhoneContact> = ArrayList()
            AndroidUtil.readContactsByName(contacts, query)
            return contacts
        }
    }

    private fun updateList(contacts: ArrayList<PhoneContact>) {
        if (!list.isEmpty()) {
            list.clear()
        }
        if (contacts.isEmpty()) {
            addDefaultContact()
        } else {
            list.addAll(contacts)
        }
        updateList()
    }

    private fun addDefaultContact() {
        val number = contactNumber()
        val contact = PhoneContact()
        contact.number = number
        list.add(contact)
    }

    private fun updateList() {
        adapter!!.notifyDataSetChanged()
    }

    companion object {

        fun newInstance(instance: Int): IndividualContactFragment {
            val args = Bundle()
            args.putInt(ARGS_INSTANCE, instance)
            val fragment = IndividualContactFragment()
            fragment.arguments = args
            return fragment
        }
    }
} // Required empty public constructor
