package com.bdcom.appdialer.fragments

import android.database.Cursor
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingComponent
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.bdcom.appdialer.LinphoneApplication
import com.bdcom.appdialer.LinphoneApplication.Companion.getApplicationContext
import com.bdcom.appdialer.R
import com.bdcom.appdialer.adapters.RingtoneListAdapter
import com.bdcom.appdialer.binding.FragmentDataBindingComponent
import com.bdcom.appdialer.databinding.RingtoneSelectionBottomDialogBinding
import com.bdcom.appdialer.models.RingtoneDataModel
import com.bdcom.appdialer.utils.Constants.RING_TONE_PATH
import com.bdcom.appdialer.utils.RecyclerItemDivider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RingtoneSettingsFragment : BaseFragment() {
    private lateinit var binding: RingtoneSelectionBottomDialogBinding
    private var dataBindingComponent: DataBindingComponent = FragmentDataBindingComponent()
    var ringtone: Ringtone? = null

    override fun onPause() {
        super.onPause()
        ringtone?.stop()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

    fun listRingtones() {
        val manager = RingtoneManager(this.activity)
        manager.setType(RingtoneManager.TYPE_NOTIFICATION)
        // manager.setType(RingtoneManager.TYPE_RINGTONE);//For Get System Ringtone
        val cursor: Cursor = manager.getCursor()
        while (cursor.moveToNext()) {
            val title: String = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri: String = manager.getRingtoneUri(cursor.getPosition()).toString()
            val ringtoneName: String = cursor.getString(cursor.getColumnIndex("title"))
            Log.e("All Data", "getNotifications: $title-=---$uri------$ringtoneName")
            // Do something with the title and the URI of ringtone
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val socialMediaList = arrayListOf(
//            RingtoneDataModel(1, "Ring Song", "/share/sounds/linphone/rings/ringtone1.mp3"),
//            RingtoneDataModel(1, "Ring Back", "/share/sounds/linphone/rings/ringback.wav"),
//            RingtoneDataModel(1, "Toy Mono", "/toy_mono.wav")
//        )

        // val socialMediaList = MutableList(20) { RingtoneDataModel() }

        val preference = LinphoneApplication.instance.getSharedPreferences()
        var savedTone = preference.getString(RING_TONE_PATH, "")
        val defaultRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString()
        val defaultTone = RingtoneManager.getRingtone(getApplicationContext(), Uri.parse(defaultRingtone))
        val defaultToneModel = RingtoneDataModel(defaultTone.getTitle(requireContext()), defaultRingtone)

        if (savedTone.isNullOrBlank()) {
            savedTone = defaultRingtone
            preference.edit().putString(RING_TONE_PATH, savedTone).apply()
        }

        val savedRingTone = RingtoneManager.getRingtone(getApplicationContext(), Uri.parse(savedTone))
        val savedRingToneModel = RingtoneDataModel(savedRingTone.getTitle(requireContext()), savedTone)

        val ringToneList: ArrayList<RingtoneDataModel> = ArrayList()

        val manager = RingtoneManager(this.activity)
        // manager.setType(RingtoneManager.TYPE_NOTIFICATION)
        manager.setType(RingtoneManager.TYPE_RINGTONE) // For Get System Ringtone
        val cursor: Cursor = manager.cursor
        while (cursor.moveToNext()) {
            val title: String = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri: String = manager.getRingtoneUri(cursor.position).toString()
            val ringtoneName: String = cursor.getString(cursor.getColumnIndex("title"))
            Log.e("All Data", "getNotifications: $title-=---$uri------$ringtoneName")
            if (defaultToneModel.title != title) ringToneList.add(RingtoneDataModel(ringtoneName, uri))
            // Do something with the title and the URI of ringtone
        }
        cursor.close()
        ringToneList.add(0, defaultToneModel)

        val ringtoneAdapter = RingtoneListAdapter {
            try {
                ringtone?.stop()
                ringtone = RingtoneManager.getRingtone(getApplicationContext(), Uri.parse(it.path))
                ringtone?.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            LinphoneApplication.coreContext.core.ring = it.path
            preference.edit().putString(RING_TONE_PATH, it.path).apply()
        }

        binding.ringtoneRecycler.addItemDecoration(RecyclerItemDivider(requireContext(), LinearLayoutManager.VERTICAL))
        binding.ringtoneRecycler.adapter = ringtoneAdapter
        ringtoneAdapter.submitList(ringToneList)

        var position = 0
        ringToneList.forEachIndexed { index, tone ->
            if (tone.path == savedTone) {
                position = index
                return@forEachIndexed
            }
        }

        CoroutineScope(Dispatchers.Main.immediate).launch {
            ringtoneAdapter.setSelected(position)
            delay(1000)
            binding.ringtoneRecycler.scrollToPosition(position)
        }
    }

    companion object {
        fun newInstance(instance: Int): RingtoneSettingsFragment {
            val args = Bundle()
            args.putInt(ARGS_INSTANCE, instance)
            val fragment = RingtoneSettingsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
