package com.bdcom.appdialer.dialog_fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingComponent
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.bdcom.appdialer.R
import com.bdcom.appdialer.adapters.RingtoneListAdapter
import com.bdcom.appdialer.binding.FragmentDataBindingComponent
import com.bdcom.appdialer.databinding.RingtoneSelectionBottomDialogBinding
import com.bdcom.appdialer.models.RingtoneDataModel
import com.bdcom.appdialer.utils.RecyclerItemDivider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RingtoneSelectionBottomSheetDialog constructor(private val callback: SocialMediaClickListener) : BottomSheetDialogFragment() {

    private lateinit var binding: RingtoneSelectionBottomDialogBinding
    private var dataBindingComponent: DataBindingComponent = FragmentDataBindingComponent()

    override fun getTheme(): Int {
        return R.style.CustomBottomSheetDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.dialog_bottom_choose_ringtone,
            container,
            false,
            dataBindingComponent
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val socialMediaList = arrayListOf(
            RingtoneDataModel("Ring Song", "file:///android_asset/ringtone1.mp3"),
            RingtoneDataModel("Ring Back", "file:///android_asset/ringback.wav"),
            RingtoneDataModel("Toy Mono", "file:///android_asset/toy_mono.wav"))
        val socialMediaAdapter = RingtoneListAdapter {
            callback.onRingtoneSelected(it)
        }

        binding.ringtoneRecycler.addItemDecoration(RecyclerItemDivider(requireContext(), LinearLayoutManager.VERTICAL))
        binding.ringtoneRecycler.adapter = socialMediaAdapter
        socialMediaAdapter.submitList(socialMediaList)
    }

    interface SocialMediaClickListener {
        fun onRingtoneSelected(ringTone: RingtoneDataModel)
    }
}
