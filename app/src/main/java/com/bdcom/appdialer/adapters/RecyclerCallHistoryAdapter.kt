package com.bdcom.appdialer.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bdcom.appdialer.R
import com.bdcom.appdialer.models.CallHistory

class RecyclerCallHistoryAdapter(
    var context: Context,
    val list: List<CallHistory>,
    private val actionCallListener: ((CallHistory) -> Unit)
) : RecyclerView.Adapter<RecyclerCallHistoryAdapter.CallHistoryViewHolder>() {

    // this two variable needed for itemclick listener for recyclerview
//    var historyList: List<CallHistory> = emptyList()
//    private val REQUEST_CONTACT_FOR_ADD_TO_EXISTING = 400

    // get the number of callHistory into a list
    override fun getItemCount(): Int {
        return list.size
    }

    // inflates the item views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallHistoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.call_history_list_row, parent, false)
        return CallHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CallHistoryViewHolder, position: Int) {
        // val item = list.get(position)

        val contactNumber = list[position].contactNumber
        val contactName = list[position].contactName
        holder.tvCallee.text = contactName
//
//        if (contactName.isNullOrBlank()){
//              contactName = AndroidUtil.getContactName(contactNumber!!)
//          }

        if (contactName.isNullOrBlank()) {
            holder.tvCallee.text = contactNumber
        } else {
            holder.tvCallee.text = contactName
        }

        holder.tvDate.text = list[position].dialStartDate

        when (list[position].callStatus) {
            1 -> {
                holder.tvCallStatus.setText(R.string.call_type_out_going)
                holder.tvCallStatus.setTextColor(ContextCompat.getColor(context, R.color.outgoingCall))
                holder.ivImage.setImageResource(R.drawable.outgoing_select)
            }
            2 -> {
                holder.tvCallStatus.setText(R.string.call_type_incoming)
                holder.tvCallStatus.setTextColor(ContextCompat.getColor(context, R.color.incomingCall))
                holder.ivImage.setImageResource(R.drawable.incoming_select)
            }
            3 -> {
                holder.tvCallStatus.setText(R.string.call_type_missed)
                holder.tvCallStatus.setTextColor(ContextCompat.getColor(context, R.color.missedCall))
                holder.ivImage.setImageResource(R.drawable.missed_select)
            }
        }

        val callDuration = list[position].callDuration
        if (callDuration.isNullOrBlank()) {
            holder.tvCallDuration.text = ""
        } else {
            holder.tvCallDuration.text = callDuration
        }

        holder.ivCall.setOnClickListener {
            actionCallListener.invoke(list[position])
        }

//        holder.btnADD.setOnClickListener {
//            if (contactNumber != null) {
//
//
//                val dialogBuilder = AlertDialog.Builder(context)
//                val inflater = LayoutInflater.from(context)
//                val dialogView = inflater.inflate(R.layout.layout_custom_addcontacts, null)
//                dialogBuilder.setView(dialogView)
//
//
//                val addtoContacts = dialogView.findViewById<View>(R.id.addTocontacts) as TextView
//                val addtoExistingContacts = dialogView.findViewById<View>(R.id.addToexisting) as TextView
//
//
//                addtoContacts.setOnClickListener {
//                    var contactNumber = list[position].contactNumber
//                    Log.d("======> Number :", contactNumber)
//
//                    val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
//                        type = ContactsContract.RawContacts.CONTENT_TYPE
//
//                        putExtra(ContactsContract.Intents.Insert.PHONE, contactNumber)
//                        //putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_WORK)
//
//                    }
//                    context.startActivity(intent)
//
//                }
//
//                addtoExistingContacts.setOnClickListener {
//                    var contactNumber = list[position].contactNumber
//                    Log.d("", "contactNumber===" + contactNumber)
//                    val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
//
//
// //                    val bundle = Bundle()
// //                    bundle.putExtras("number", contactNumber)
// //                    intent.putExtras(bundle)
//                    intent.putExtra("number", contactNumber)
//
//                    (context as Activity).startActivityForResult(intent, REQUEST_CONTACT_FOR_ADD_TO_EXISTING)
//                }
//
//                dialogBuilder.setNegativeButton("Cancel") { dialog, whichButton ->
//                    //pass
//                }
//
//                val b = dialogBuilder.create()
//                b.show()
//
//                //val theButton = b.getButton(DialogInterface.BUTTON_NEGATIVE)
//                val btnNeg = b.getButton(AlertDialog.BUTTON_NEGATIVE)
//                val layoutParams = btnNeg.layoutParams as LinearLayout.LayoutParams
//                layoutParams.weight = 14f
//                btnNeg.layoutParams = layoutParams
//
//            }
//        }
    }

    inner class CallHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Holds the Textview, imageView that will add each callhistory to recyclerview
        var tvCallee: TextView = itemView.findViewById(R.id.tvCallee)
        var tvDate: TextView = itemView.findViewById(R.id.tvDate)
        var tvCallStatus: TextView = itemView.findViewById(R.id.tvCallStatus)
        var tvCallDuration: TextView = itemView.findViewById(R.id.tvCallDuration)
        var ivImage: ImageView = itemView.findViewById(R.id.ivCalleeImage)
        var ivCall: ImageView = itemView.findViewById(R.id.ivCall)
    }
}
