package com.bdcom.appdialer.adapters

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.bdcom.appdialer.R
import com.bdcom.appdialer.models.PhoneContact
import com.bdcom.appdialer.utils.AndroidUtil
import com.bdcom.appdialer.utils.ImageUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import de.hdodenhof.circleimageview.CircleImageView
import java.util.*

class ContactAdapter(
    context: Context,
    val resource: Int,
    var list: ArrayList<PhoneContact>,
    private val showNumber: Boolean
) : ArrayAdapter<PhoneContact>(context, resource, list) {

    override fun getItem(position: Int): PhoneContact {
        return list[position]
    }

    override fun getCount(): Int {
        return list.size
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        val row: View

        if (convertView == null) {
            row = LayoutInflater.from(context).inflate(resource, null)
            holder = ViewHolder()
            holder.ivImage = row.findViewById(R.id.ivImage)
            holder.civImage = row.findViewById(R.id.civImage)
            holder.tvName = row.findViewById(R.id.tvName)
            holder.tvNumber = row.findViewById(R.id.tvNumber)

            row.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
            row = convertView
        }

        val contact = getItem(position)
        var name = contact.name!!
        holder.tvName.text = name

        if (AndroidUtil.isValidPhoneNumber(name)) {
            name = "#"
        }

        //

//
//        val imageUri = contact.image
//        if (imageUri != null) {
//            holder.civImage.visibility = View.VISIBLE
//            holder.civImage.setImageURI(imageUri)
//        } else {
//            holder.civImage.visibility = View.GONE
//            holder.ivImage.setImageDrawable(ImageUtil.circularImage(name, contact.color))
//        }

        Thread(Runnable {
            var uri = AndroidUtil.getPhotoUri(contact.id!!)
            if (uri != null) {

                (context as AppCompatActivity).runOnUiThread {
                    holder.civImage.visibility = View.VISIBLE
                    Glide.with(this@ContactAdapter.context)
                            .load(uri)
                            .apply(RequestOptions().override(60, 60))
                            .into(holder.civImage)
                }
            } else {
                var drawable = ImageUtil.circularImage(name, contact.color)
                (context as AppCompatActivity).runOnUiThread {
                    holder.civImage.visibility = View.GONE
                    holder.ivImage.setImageDrawable(drawable)
                }
            }
        }).start()

        //

        if (showNumber && name != "#") {
            holder.tvNumber.text = contact.number
            holder.tvNumber.visibility = View.VISIBLE
        }

        return row
    }

    private inner class ViewHolder {
        lateinit var ivImage: ImageView
        lateinit var civImage: CircleImageView
        lateinit var tvName: TextView
        lateinit var tvNumber: TextView
    }

    companion object {
        fun newInstance(context: Context, list: ArrayList<PhoneContact>, showNumber: Boolean): ContactAdapter {
            return ContactAdapter(context, R.layout.contacts_list_row, list, showNumber)
        }
    }
}
