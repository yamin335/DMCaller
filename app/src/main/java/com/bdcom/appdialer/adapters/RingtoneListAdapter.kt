package com.bdcom.appdialer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bdcom.appdialer.R
import com.bdcom.appdialer.models.RingtoneDataModel
import kotlinx.android.synthetic.main.list_item_ringtone.view.*

class RingtoneListAdapter internal constructor(
    private val onItemClickListener: (RingtoneDataModel) -> Unit
) : RecyclerView.Adapter<RingtoneListAdapter.ItemViewHolder>() {

    private var selectedItemPosition = -1

    private var mediaList: ArrayList<RingtoneDataModel> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_ringtone, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return mediaList.size
    }

    fun setSelected(position: Int) {
        selectedItemPosition = position
        notifyDataSetChanged()
    }

    fun submitList(mediaList: ArrayList<RingtoneDataModel>) {
        this.mediaList = mediaList
        notifyDataSetChanged()
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(position: Int) {
            val item = mediaList[position]

            itemView.toneTitle.text = item.title
            itemView.isChosen.visibility = if (position == selectedItemPosition) View.VISIBLE else View.INVISIBLE

            itemView.container.setOnClickListener {
                onItemClickListener.invoke(item)
                selectedItemPosition = position
                notifyDataSetChanged()
            }
        }
    }
}
