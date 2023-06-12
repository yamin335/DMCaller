package com.bdcom.appdialer.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.bdcom.appdialer.R
import com.bdcom.appdialer.models.PhoneContact
import java.util.*

class IndividualContactAdapter(
    context: Context,
    val resource: Int,
    val list: ArrayList<PhoneContact>
) : ArrayAdapter<PhoneContact>(context, resource, list) {

    override fun getItem(position: Int): PhoneContact {
        return list[position]
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        val row: View

        if (convertView == null) {
            row = LayoutInflater.from(context).inflate(resource, null)
            holder = ViewHolder()
            holder.tvNumber = row.findViewById(R.id.tvNumber)
            holder.ivCall = row.findViewById(R.id.ivCall)
//            holder.ivMessage = row.findViewById(R.id.ivMessage)
//            holder.ivMail = row.findViewById(R.id.ivMail)

            holder.ivCall!!.setOnClickListener {
                (parent as ListView).performItemClick(holder.ivCall, position, 0)
            }
//            holder.ivMessage!!.setOnClickListener {
//                (parent as ListView).performItemClick(holder.ivMessage, position, 0)
//            }
//            holder.ivMail!!.setOnClickListener {
//                (parent as ListView).performItemClick(holder.ivMail, position, 0)
//            }

            row.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
            row = convertView
        }

        val contact = getItem(position)
        holder.tvNumber!!.text = contact.number

        return row
    }

    private inner class ViewHolder {
        var tvNumber: TextView? = null
        var ivCall: View? = null
//        var ivMessage: View? = null
//        var ivMail: View? = null
    }

    companion object {
        fun newInstance(context: Context, list: ArrayList<PhoneContact>): IndividualContactAdapter {
            return IndividualContactAdapter(context, R.layout.individual_contact_list_row, list)
        }
    }
}
