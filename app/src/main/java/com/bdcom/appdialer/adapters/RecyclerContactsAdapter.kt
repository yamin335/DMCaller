package com.bdcom.appdialer.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bdcom.appdialer.R
import com.bdcom.appdialer.models.PhoneContact
import com.bdcom.appdialer.utils.AndroidUtil
import com.bdcom.appdialer.utils.ImageUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecyclerContactsAdapter(
    var context: Context,
    var list: java.util.ArrayList<PhoneContact>,
    private val showNumber: Boolean
) : RecyclerView.Adapter<RecyclerContactsAdapter.ContactHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): RecyclerContactsAdapter.ContactHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.contacts_list_row, parent, false)
        return ContactHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerContactsAdapter.ContactHolder, position: Int) {

        val contact = list[position]
        var name = contact.name ?: "Unknown"
        holder.tvName.text = name

        if (AndroidUtil.isValidPhoneNumber(name)) {
            name = "#"
        }

        CoroutineScope(Dispatchers.Main.immediate).launch {
            val uri = AndroidUtil.getPhotoUri(contact.id!!)
            if (uri != null) {
                (context as AppCompatActivity).runOnUiThread {
                    holder.civImage.visibility = View.VISIBLE
                    Glide.with(this@RecyclerContactsAdapter.context as AppCompatActivity)
                        .load(uri)
                        .apply(RequestOptions().override(60, 60))
                        .into(holder.civImage)
                }
            } else {
                val drawable = ImageUtil.circularImage(name, contact.color)
                (context as AppCompatActivity).runOnUiThread {
                    holder.civImage.visibility = View.GONE
                    holder.ivImage.setImageDrawable(drawable)
                }
            }
        }
//        Thread {
//            var uri = AndroidUtil.getPhotoUri(contact.id!!)
//            if (uri != null) {
//
//                (context as AppCompatActivity).runOnUiThread {
//                    holder.civImage.visibility = View.VISIBLE
//                    Glide.with(this@RecyclerContactsAdapter.context as AppCompatActivity)
//                        .load(uri)
//                        .apply(RequestOptions().override(60, 60))
//                        .into(holder.civImage)
//                }
//            } else {
//                var drawable = ImageUtil.circularImage(name, contact.color)
//                (context as AppCompatActivity).runOnUiThread {
//                    holder.civImage.visibility = View.GONE
//                    holder.ivImage.setImageDrawable(drawable)
//                }
//            }
//        }.start()

//        print(contact.name)

        if (showNumber && name != "#") {
            holder.tvNumber.text = contact.number
            holder.tvNumber.visibility = View.VISIBLE
        }
    }

    inner class ContactHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var ivImage: ImageView
        var civImage: CircleImageView
        var tvName: TextView
        var tvNumber: TextView

        init {
            ivImage = itemView.findViewById(R.id.ivImage)
            civImage = itemView.findViewById(R.id.civImage)
            tvName = itemView.findViewById(R.id.tvName)
            tvNumber = itemView.findViewById(R.id.tvNumber)
        }
    }
}
