package edu.rosehulman.whodoyouknowhere.whodoyouknowhere

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth

private lateinit var mItemTouchHelper: ItemTouchHelper
private lateinit var eventOrgAdapter: EventOrgAdapter
private lateinit var recyclerView: RecyclerView
private lateinit var viewManager: LinearLayoutManager
private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "NO USER"

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_USER_ID = "userId"


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [EventOrgFragment.OnEventOrgFragmentSelectedListener] interface
 * to handle interaction events.
 * Use the [EventOrgFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class EventOrgFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var userID: String? = null



    private var listenerEventOrg: OnEventOrgFragmentSelectedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userID = it.getString(ARG_USER_ID)

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
//        val toggle = ActionBarDrawerToggle(
//            activity, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
//        )


        recyclerView = inflater.inflate(R.layout.fragment_event_org, container, false) as RecyclerView
        eventOrgAdapter = EventOrgAdapter(context, uid, listenerEventOrg,userID!!)

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = eventOrgAdapter

        val callback = SimpleItemTouchHelperCallback(eventOrgAdapter)
        mItemTouchHelper = ItemTouchHelper(callback)
        mItemTouchHelper.attachToRecyclerView(recyclerView)
        val fab = (context as MainActivity).getFab()
        fab.show()

        fab.setOnClickListener {
            eventOrgAdapter.showAddEditDialog()
        }

//        drawer_layout.addDrawerListener(toggle)
//        toggle.syncState()

        //  nav_view.setNavigationItemSelectedListener()


        return recyclerView
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnEventOrgFragmentSelectedListener) {
            listenerEventOrg = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnEventOrgFragmentSelectedListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listenerEventOrg = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnEventOrgFragmentSelectedListener {
        // TODO: Update argument type and name
        fun onEventOrgFragmentSelected(event: Event)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment EventOrgFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(userId: String) =
            EventOrgFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_ID, userId)

                }
            }
    }
}
