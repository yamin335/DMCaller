package com.bdcom.appdialer.fragments

import android.os.Bundle
import android.view.*
import com.bdcom.appdialer.R

class ConferenceFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_conference, container, false)
        setHasOptionsMenu(true)
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.create_new_conference, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.addConference) {
            mFragmentNavigation.pushFragment(NewConferenceFragment.newInstance(0))
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        fun newInstance(instance: Int): ConferenceFragment {
            val args = Bundle()
            args.putInt(BaseFragment.ARGS_INSTANCE, instance)
            val fragment = ConferenceFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
