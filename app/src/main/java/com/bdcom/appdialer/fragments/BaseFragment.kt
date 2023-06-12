package com.bdcom.appdialer.fragments

import android.content.Context
import android.os.Bundle

abstract class BaseFragment : androidx.fragment.app.Fragment() {

    lateinit var mFragmentNavigation: FragmentNavigation

    private var mInt = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        if (args != null) {
            mInt = args.getInt(ARGS_INSTANCE)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentNavigation) {
            mFragmentNavigation = context
        }
    }

    interface FragmentNavigation {
        fun pushFragment(fragment: androidx.fragment.app.Fragment)
        fun popFragment()
    }

    companion object {
        const val ARGS_INSTANCE = "com.bdcom.rtcphone.fragments.argsInstance"
    }
}
