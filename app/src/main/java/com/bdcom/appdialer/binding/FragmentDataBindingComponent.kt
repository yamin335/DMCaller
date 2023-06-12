package com.bdcom.appdialer.binding

import androidx.databinding.DataBindingComponent

class FragmentDataBindingComponent : DataBindingComponent {
    private val adapter = FragmentBindingAdapters()

    override fun getFragmentBindingAdapters() = adapter
}
