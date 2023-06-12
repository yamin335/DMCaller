package com.bdcom.appdialer.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bdcom.appdialer.LinphoneApplication
import com.bdcom.appdialer.R
import com.bdcom.appdialer.adapters.RecyclerCallHistoryAdapter
import com.bdcom.appdialer.models.CallHistory
import com.bdcom.appdialer.recycler_extensions.SwipeToDeleteCallback
import com.bdcom.appdialer.utils.AndroidUtil
import com.bdcom.appdialer.utils.AppUtils
import com.roughike.bottombar.BottomBar

class DialCallHistoryFragment : BaseFragment(), AdapterView.OnItemClickListener {

    // private var adapter: CallHistoryAdapter? = null
    private var adapter: RecyclerCallHistoryAdapter? = null

    private val list: ArrayList<CallHistory> = ArrayList()

    var filteredCallList: ArrayList<CallHistory> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        setHasOptionsMenu(true)

        val view = inflater.inflate(R.layout.fragment_dial_call_history, container, false)
        // val listView = view.findViewById(R.id.listView) as ListView
        val mView = view.findViewById(R.id.recyclerView_callhistory) as RecyclerView
        mView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        mView.itemAnimator = DefaultItemAnimator()
        mView.addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))

        if (!list.isEmpty() && !filteredCallList.isEmpty()) {
            list.clear()
            filteredCallList.clear()
        }

        val bottomBar = view.findViewById(R.id.bottomBar) as BottomBar
        bottomBar.setOnTabSelectListener { tabId ->
            filteredCallList = when (tabId) {
                R.id.missed_call -> {
                    getCallHistoryByStatus(3)
                }
                R.id.incoming_call -> {
                    getCallHistoryByStatus(2)
                }
                R.id.outgoing_call -> {
                    getCallHistoryByStatus(1)
                }
                else -> {
                    list
                }
            }
            // adapter = CallHistoryAdapter.newInstance(requireContext(), filteredCallList)
            // listView.adapter = adapter
            adapter = RecyclerCallHistoryAdapter(requireContext(), filteredCallList) {
                val name = it.contactName
                val number = it.contactNumber
                // Log.d("Click CHECK =====> :", name.toString())
                LinphoneApplication.instance.getDatabaseHelper().insertCallHistory(number!!, name!!, 1)
                AppUtils.directCall(number)
                // Fix CallActivity.start(requireContext(), name, number)
            }
            mView.adapter = adapter

//            mView.addOnItemClickListener(object : OnItemClickListener {
//                override fun onItemClicked(position: Int, view: View) {
//
//                    /***** Your click item logic: START ******/
//                    val callHistory = filteredCallList[position]
//                    val name = callHistory.contactName
//                    val number = callHistory.contactNumber
//                    //Log.d("Click CHECK =====> :", name.toString())
//                    LinphoneApplication.instance.getDatabaseHelper().insertCallHistory(number!!, name!!, 1)
//                    CallActivity.start(requireContext(), name, number)
//                    /********* END ***************/
//
//                }
//            })

            val swipeHandler = object : SwipeToDeleteCallback(context) {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, position: Int) {

            // val adapter = mView.adapter as SimpleAdapter
            // adapter.removeAt(viewHolder.adapterPosition)

                    try {
                        val builder = AlertDialog.Builder(requireContext())
                        builder.setMessage("Are you want to delete this number?")
                        builder.setCancelable(false)
                        builder.setPositiveButton("YES") { dialog, which ->

                            deleteCall(filteredCallList[viewHolder.adapterPosition].contactId!!)
                            // list.removeAt(viewHolder.adapterPosition)
                            // adapter!!.notifyDataSetChanged()
                            filteredCallList.removeAt(viewHolder.adapterPosition)
                            adapter!!.notifyDataSetChanged()
                            readCallHistory()
                        }

                        builder.setNegativeButton("Cancel") { dialog, which ->
                            readCallHistory()
                        }
                        val dialog: AlertDialog = builder.create()
                        dialog.show()

                        throw IndexOutOfBoundsException()
                    } catch (e: IndexOutOfBoundsException) {
                        e.printStackTrace()
                    }
                }
            }

            val itemTouchHelper = ItemTouchHelper(swipeHandler)
            itemTouchHelper.attachToRecyclerView(mView)
        }

        /*
        listView.onItemClickListener = this

        val touchListener = SwipeDismissListViewTouchListener(
        listView,
        object : SwipeDismissListViewTouchListener.DismissCallbacks {
        override fun canDismiss(position: Int): Boolean {
        // Toast.makeText(context,position.toString()+" "+list[position].contactId+" "+filteredCallList[position].contactId,Toast.LENGTH_SHORT).show()
        return true
        }

        override fun onDismiss(listView: ListView, reverseSortedPositions: IntArray) {
        for (position in reverseSortedPositions) {
        deleteCall(filteredCallList[position].contactId!!)
        list.removeAt(position)
        filteredCallList.removeAt(position)
        readCallHistory()
        }
        }
        })
        listView.setOnTouchListener(touchListener)
        */

        return view
    }

    private fun getCallHistoryByStatus(status: Int): ArrayList<CallHistory> {
        var filteredCallList: ArrayList<CallHistory> = ArrayList<CallHistory>()
        for (item in list) {
            if (item.callStatus == status) {
                filteredCallList.add(item)
            }
        }
        return filteredCallList
    }

    private fun readCallHistory() {
        if (!list.isEmpty()) {
            list.clear()
        }
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), ContactsFragment.PERMISSION_READ_CONTACT)
            // After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            LinphoneApplication.instance.getDatabaseHelper().readCallHistory(null, list)
            updateList()
            updateContactNameIntoList()
        }
    }

    private fun updateContactNameIntoList() {
            for (i in list.indices) {
                var history: CallHistory = list.get(i)
                if (history.contactName.isNullOrBlank()) {
                    list.get(i).contactName = AndroidUtil.getContactName(history.contactNumber!!)
                }
            }
            updateList()
    }

    private fun updateList() {
        adapter!!.notifyDataSetChanged()
    }

    private fun deleteCall(id: String) {
        LinphoneApplication.instance.getDatabaseHelper().deleteCallHistory(id)
    }

    override fun onResume() {
        super.onResume()
        readCallHistory()
    }

    override fun onItemClick(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
        val callHistory = list[i]
        val name = callHistory.contactName
        val number = callHistory.contactNumber
        LinphoneApplication.instance.getDatabaseHelper().insertCallHistory(number!!, name!!, 1)
        AppUtils.directCall(number)
        // Fix CallActivity.start(requireContext(), name, number)
    }

    companion object {
        fun newInstance(instance: Int): DialCallHistoryFragment {
            val args = Bundle()
            args.putInt(BaseFragment.ARGS_INSTANCE, instance)
            val fragment = DialCallHistoryFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
