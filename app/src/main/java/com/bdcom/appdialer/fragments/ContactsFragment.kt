package com.bdcom.appdialer.fragments

    import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bdcom.appdialer.LinphoneApplication
import com.bdcom.appdialer.R
import com.bdcom.appdialer.adapters.RecyclerContactsAdapter
import com.bdcom.appdialer.models.PhoneContact
import com.bdcom.appdialer.recycler_extensions.OnItemClickListener
import com.bdcom.appdialer.recycler_extensions.addOnItemClickListener
import com.bdcom.appdialer.utils.AndroidUtil
import com.bdcom.appdialer.utils.MyPreference
import com.gjiazhe.wavesidebar.WaveSideBar
import com.google.gson.Gson
import kotlin.collections.ArrayList
import kotlinx.android.synthetic.main.fragment_contacts.*
import kotlinx.coroutines.*

open class ContactsFragment : BaseFragment(), SearchView.OnQueryTextListener,
        OnItemClickListener {

        // private var adapter: ContactAdapter? = null
        private var adapter: RecyclerContactsAdapter? = null

       // private var favadapter: ContactAdapter? = null
        var isFavClicked: Boolean = false
        protected val list: ArrayList<PhoneContact> = ArrayList()
        // protected val favlist: ArrayList<PhoneContact> = ArrayList()
        var favouriteContactsL = ArrayList<PhoneContact>()
        // var searchView: SearchView? = null

        private var locationManager: LocationManager? = null
        private var waveSideBar: WaveSideBar? = null

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            super.onCreateView(inflater, container, savedInstanceState)
            setHasOptionsMenu(true)

            // val bundle = intent.getBundle()
            // bundle.getString("key")

            // checkGpsPermission()
            askPermissions()

            if (Build.VERSION.SDK_INT >= 21) {
                val window = requireActivity().window
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            }

            if (!list.isEmpty()) {
                list.clear()
            }

            val view = inflater.inflate(R.layout.fragment_contacts, container, false)
//            val listView = view.findViewById(R.id.listView) as ListView

            val recyclerView = view.findViewById(R.id.recyclerView_calls) as RecyclerView
            recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            recyclerView.itemAnimator = DefaultItemAnimator()
            recyclerView.addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))

            waveSideBar = view.findViewById(R.id.side_bar) as WaveSideBar
            for (i in list.indices) {
                val i_index = list[i].name!!.first().toString()
                waveSideBar!!.setIndexItems(i_index)
            }

            waveSideBar!!.setOnSelectIndexItemListener(WaveSideBar.OnSelectIndexItemListener { index ->
                for (i in list.indices) {
                    val i_name = list[i].name!!.first().toString()
                    if (i_name.equals(index)) {
                        (recyclerView.getLayoutManager() as LinearLayoutManager).scrollToPositionWithOffset(i, 0)
                        return@OnSelectIndexItemListener
                    }
                }
            })

            val searchView = view.findViewById<SearchView>(R.id.search_contact)

            val showNumber = showNumberInList()
//            adapter = ContactAdapter.newInstance(requireContext(), list, showNumber)
//            listView.adapter = adapter

            adapter = RecyclerContactsAdapter(requireContext(), list, showNumber)
            recyclerView.adapter = adapter

            // askPermissions()

            // listView.onItemClickListener = this
            recyclerView.addOnItemClickListener(this)
            searchView.setOnQueryTextListener(this)

//            listView.setOnScrollListener(object : AbsListView.OnScrollListener {
//
//                override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
//                    if (scrollState != 0) {
//                        AndroidUtil.hideKeyboard(activity!!)
//                    }
//                }
//
//                override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
//                    // Do nothing
//                }
//            })

            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    if (newState != 0) {
                        AndroidUtil.hideKeyboard(activity!!)
                    }

                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        Log.d("scroll", "idle")
                    } else if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                        Log.d("scroll", "settling")
                    } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        Log.d("scroll", "dragging")
                    }
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    Log.d("scroll", "scrolling")
                }
            })
//
//

            return view
        }

        @SuppressLint("ResourceAsColor")
        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)

            // askPermissions()

            tabFav.setOnClickListener {
                // searchView!!.setQuery("", false)

                // ll_border.setBackgroundResource(R.drawable.boder)
                isFavClicked = true
                animateTextView(tabFav, 16f)
                tabFav.setBackgroundResource(R.drawable.contact_tab_bg)
                tabFav.setTextColor(resources.getColor(R.color.ef_white))

                animateTextView(tabAll, 13f)
                tabAll.setBackgroundResource(R.drawable.contact_tab_bg_unselect)
                tabAll.setTextColor(R.color.colorPrimary)

                Handler().postDelayed(this::showFavouriteContacts, 0)
            }

            tabAll.setOnClickListener {
                // searchView!!.setQuery("", false)
                // ll_border.setBackgroundResource(R.drawable.boder)
                isFavClicked = false
                animateTextView(tabFav, 13f)
                tabFav.setBackgroundResource(R.drawable.contact_tab_bg_unselect)
                tabFav.setTextColor(R.color.colorPrimary)

                animateTextView(tabAll, 16f)
                tabAll.setBackgroundResource(R.drawable.contact_tab_bg)
                tabAll.setTextColor(resources.getColor(R.color.ef_white))

                CoroutineScope(Dispatchers.Main.immediate).launch {
                    showAllContacts()
                }
                // Handler().postDelayed(this::showAllContacts, 0)
            }

            tabAll.textSize = 16f
            tabFav.textSize = 13f
        }

        private fun showAllContacts() {
            // readContacts("")
            adapter?.list = this.list
            adapter?.notifyDataSetChanged()
        }

        var favouriteContacts = ArrayList<PhoneContact>()

        private fun showFavouriteContacts() {
            // list.clear()
            var savedlist: java.util.ArrayList<*>? = MyPreference.with(context).getObject("favourite", ArrayList::class.java) // ?: return

            favouriteContacts.clear()
            // var index = 0
            if (savedlist != null) {

                for (item in savedlist!!) {
                    // index = 0
                    for (contact in this.list) {
                        if (contact.id == (item as Double).toInt()) {
                            Log.d("contact id==>", contact.id.toString())
                            favouriteContacts.add(contact)
                        }
                        // index = index + 1;
                    }
                }
            }

            // adapter?.list = favouriteContacts
            adapter?.list = favouriteContacts
            adapter?.notifyDataSetChanged()
            // updateList(favouriteContacts)
            Log.d("adapterList ===>", adapter?.list.toString())

            Log.d("===>>> favourites", Gson().toJson(savedlist))
        }

        private fun animateTextView(textView: TextView, target: Float) {
            val px = textView.textSize
            val sp = px / resources.displayMetrics.scaledDensity

            val animator = ValueAnimator.ofFloat(sp, target)
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.duration = 300

            animator.addUpdateListener { animation ->
                textView.textSize = animation.animatedValue as Float
            }
            animator.start()
        }

    override fun onResume() {
        super.onResume()
        if (this.list.isEmpty()) {
            readContacts("")
        } else {
            showAllContacts()
        }
    }

        override fun onStart() {
            super.onStart()
            // EventBus.getDefault().register(this)
        }

        override fun onStop() {
            super.onStop()
            // EventBus.getDefault().unregister(this)
        }

        private fun askPermissions() {
            val status = ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
            if (status == PackageManager.PERMISSION_GRANTED) {
                    readContacts("")
                    askForRecordAudio()
            } else {
                requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), ContactsFragment.PERMISSION_READ_CONTACT)
            }
        }

        private fun askForRecordAudio() {
            val status = ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)

            if (status == PackageManager.PERMISSION_GRANTED) {
                askForExternalStorage()
            } else {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), ContactsFragment.PERMISSION_RECORD_AUDIO)
            }
        }

        private fun askForExternalStorage() {
            val status = ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)

            if (status == PackageManager.PERMISSION_GRANTED) {
                // checkGpsPermission()
            } else {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), ContactsFragment.PERMISSIONS_READ_EXTERNAL_STORAGE_DEVICE_RINGTONE)
            }
        }

        private fun checkGpsPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle("Location Permission")
                    builder.setMessage("The app needs location permissions. Please grant this permission to continue using the features of the app.")
                    builder.setPositiveButton(android.R.string.yes) { dialogInterface, i -> requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSION_REQUEST_COARSE_LOCATION) }
                    builder.setNegativeButton("Cancel", null)
                    builder.show()
                }
            } else {
                locationManager = requireActivity().getSystemService(LOCATION_SERVICE) as LocationManager?
                val isGpsProviderEnabled: Boolean
                val isNetworkProviderEnabled: Boolean
                isGpsProviderEnabled = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
                isNetworkProviderEnabled = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                if (!isGpsProviderEnabled && !isNetworkProviderEnabled) {
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle("Location Permission")
                    builder.setMessage("The app needs location permissions. Please grant this permission to continue using the features of the app.")
                    builder.setPositiveButton(android.R.string.yes) { dialogInterface, i ->
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        startActivity(intent)
                    }
                    builder.setNegativeButton("Cancel", null)
                    builder.show()
                }
            }
        }

        private fun readContacts(query: String) {
            loadDataAsync(query)
        }

        var contacts: ArrayList<PhoneContact> = ArrayList()

        private fun loadDataAsync(query: String) = GlobalScope.launch {
            if (isFavClicked) {
                //  contacts = favouriteContacts
            } else {
                contacts = ArrayList()
                withContext(Dispatchers.Default) {
                    val showAll = showDuplicate()
                    AndroidUtil.readContacts(contacts, query, showAll)
                    CoroutineScope(Dispatchers.Main.immediate).launch {
                        updateList(contacts)
                    }
                }
            }
        }

        open fun showDuplicate(): Boolean {
            return false
        }

        open fun showNumberInList(): Boolean {
            return false
        }

        private fun updateList(contacts: ArrayList<PhoneContact>) {
            if (list.isNotEmpty()) {
                list.clear()
            }

            list.addAll(contacts)

            adapter?.notifyDataSetChanged()
        }

        public fun reloadContacts() {
            readContacts("")
        }

        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ) {
            if (requestCode == PERMISSION_READ_CONTACT) {
                if (!grantResults.isEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    readContacts("")
                    askForRecordAudio()
                } else {
                    // Not granted
                    askForRecordAudio()
                }
            }

            if (requestCode == PERMISSIONS_READ_EXTERNAL_STORAGE_DEVICE_RINGTONE) {
                if (!grantResults.isEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // checkGpsPermission()
                } else {
                    // checkGpsPermission()
                }
            }

            if (requestCode == PERMISSION_REQUEST_COARSE_LOCATION) {
                if (!grantResults.isEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("ContactFragment", "coarse location permission granted")
                } else {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts("package", LinphoneApplication.instance.packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }
            }

            if (requestCode == PERMISSION_RECORD_AUDIO) {
                if (!grantResults.isEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Do nothing
                    askForExternalStorage()
                } else {
                    // Not granted
                    askForExternalStorage()
                }
            } else {
               // askForRecordAudio()
            }

            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }

        companion object {

            public const val PERMISSION_READ_CONTACT = 123

            private const val PERMISSION_RECORD_AUDIO = 124

            private val PERMISSION_REQUEST_COARSE_LOCATION = 300

            private val PERMISSIONS_READ_EXTERNAL_STORAGE_DEVICE_RINGTONE = 210

            fun newInstance(instance: Int): ContactsFragment {
                val args = Bundle()
                args.putInt(BaseFragment.ARGS_INSTANCE, instance)
                val fragment = ContactsFragment()
                fragment.arguments = args
                return fragment
            }
        }

        override fun onQueryTextSubmit(s: String): Boolean {
            return false
        }

        override fun onQueryTextChange(filter: String): Boolean {
            if (isFavClicked) {

                favouriteContactsL.clear()

                for (contact in favouriteContacts) {
                    val name = contact.name
                    if (name!!.toLowerCase().contains(filter.toLowerCase())) {
                        favouriteContactsL.add(contact)
                    }
                }

                adapter?.list = favouriteContactsL
                adapter?.notifyDataSetChanged()
                Log.d("=====> ADAPTERList :", adapter.toString())
            } else {
                readContacts(filter.trim())
            }
            return false
        }

        override fun onItemClicked(i: Int, view: View) {
            AndroidUtil.hideKeyboard(requireActivity())
            var contact = list[i]

            if (isFavClicked) {
                try {
                    // favouriteContactsL = favouriteContacts
                    if (favouriteContactsL.isNotEmpty()) {
                        contact = favouriteContactsL[i]
                    } else {
                        contact = favouriteContacts[i]
                    }

                    throw IndexOutOfBoundsException()
                } catch (e: IndexOutOfBoundsException) {
                    e.printStackTrace()
                }
            } else {
                contact = list[i]
            }

            val fragment = IndividualContactFragment.newInstance(0)
            val bundle = fragment.requireArguments()
            bundle.putString("name", contact.name)
            bundle.putString("number", contact.number)
            bundle.putInt("color", contact.color)
            bundle.putString("image", contact.image.toString())
            bundle.putInt("id", contact.id!!)

            Log.d("Click : name ===>", contact.name.toString())

            fragment.arguments = bundle
            mFragmentNavigation.pushFragment(fragment)
        }
}
