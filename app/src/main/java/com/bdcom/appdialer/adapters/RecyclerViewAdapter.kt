package com.bdcom.appdialer.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bdcom.appdialer.R
import com.bdcom.appdialer.models.PhoneContact
import com.bdcom.appdialer.utils.AndroidUtil
import com.bdcom.appdialer.utils.ImageUtil
import java.util.*

class RecyclerViewAdapter internal constructor(
    private val list: ArrayList<PhoneContact>,
    internal var context: Context
) : RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.conference_contact, parent, false)
        return RecyclerViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {

        val contact = list[position]
        val contactName = contact.name!!
        var name = contactName

        if (AndroidUtil.isValidPhoneNumber(name)) {
            name = "#"
        }
        holder.ivCircle.setImageDrawable(ImageUtil.circularImage(name, contact.color))

        if (contactName === "host") {
            holder.ivDelete.visibility = View.GONE
        }

        if (contact.showDeleteIcon) {
            holder.ivDelete.visibility = View.VISIBLE
        } else {
            holder.ivDelete.visibility = View.GONE
        }

        holder.tvName.text = contactName

        holder.ivCircle.setOnLongClickListener(View.OnLongClickListener {
            if (contactName === "host") {
                return@OnLongClickListener false
            } else {
                holder.ivDelete.visibility = View.VISIBLE
                contact.showDeleteIcon = true
            }
            true
        })

        holder.ivCircle.setOnClickListener {
            if (contact.showDeleteIcon) {
                holder.ivDelete.visibility = View.GONE
                contact.showDeleteIcon = false
            }
        }

        holder.ivDelete.setOnClickListener(View.OnClickListener {
            list.remove(contact)
            notifyDataSetChanged()
        })
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class RecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var ivCircle: ImageView

        internal var ivDelete: ImageView

        internal var tvName: TextView

        init {
            ivCircle = itemView.findViewById(R.id.ivCalleeImage)
            ivDelete = itemView.findViewById(R.id.ivDelete)
            tvName = itemView.findViewById(R.id.tvName)
        }
    }
}
