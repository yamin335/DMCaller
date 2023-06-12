package com.bdcom.appdialer.binding

import android.view.View
import androidx.databinding.BindingAdapter

class FragmentBindingAdapters {
    @BindingAdapter("showLoader")
    fun showLoader(view: View, apiCallStatus: Int?) {
        view.visibility = if (apiCallStatus == 1) View.VISIBLE else View.INVISIBLE
    }
}
