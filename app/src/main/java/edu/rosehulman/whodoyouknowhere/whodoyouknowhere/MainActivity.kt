package edu.rosehulman.whodoyouknowhere.whodoyouknowhere

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.mindorks.placeholderview.SwipeDecor
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.add_user_dialog.view.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import java.util.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    BottomNavigationView.OnNavigationItemSelectedListener, EventOrgFragment.OnEventOrgFragmentSelectedListener {


    private var isNewUser = false

    private val margin = 160 //160
    private val animationDuration = 300
    private var isToUndo = false
    private lateinit var cardViewHolderSize: Point
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val authID: String? = auth.currentUser?.uid
    private var user: User? = null

    lateinit var authListener: FirebaseAuth.AuthStateListener
    private var backButtonCount = 0

    private var eventList: ArrayList<Event> = ArrayList()


    private val userRef = FirebaseFirestore
        .getInstance()
        .collection(Constants.USERS_COLLECTION)
    private val eventsRef = FirebaseFirestore
        .getInstance()
        .collection(Constants.EVENTS_COLLECTION)


    init {

//        userRef.whereEqualTo("authID", auth.currentUser!!.uid).get().addOnSuccessListener {
//            for(user in it.documents){
//                this.user = user.toObject(User::class.java)
//            }
//        }


//        userRef.whereEqualTo("authID", authID).get().addOnSuccessListener { snapshot ->
//            if (snapshot.isEmpty) {
//                isNewUser = true
//                Log.d(Constants.TAG, "New User with AUTH UID: ${authID} detected.")
//            } else {
//                isNewUser = false
//                Log.d(Constants.TAG, "Existing AUTH UID is: ${authID}")
//                for(doc in snapshot.documents){
//                    var docUser = User.fromSnapshot(doc)
//                    if(docUser.authID==authID){
//                        user = docUser
//                    }
//                }
//            }
//        }

//        val currentUser = auth.currentUser!!.metadata
//        isNewUser = currentUser!!.creationTimestamp == currentUser.lastSignInTimestamp
//
//        Log.d(Constants.DEBUG, "CreationDate: ${currentUser.creationTimestamp}")
//        Log.d(Constants.DEBUG, "LastSignIn: ${currentUser.lastSignInTimestamp}")
//
//        if (isNewUser) {
//            Log.d(Constants.TAG, "New User with AUTH UID: ${auth.currentUser!!.uid} detected.")
//        }
//        Log.d(Constants.TAG, "Existing AUTH UID is: ${auth.currentUser!!.uid}")
//
//        user = User()

//        userRef.addSnapshotListener { snapshot, fireStoreException ->
//            if (fireStoreException != null) {
//                Log.d(Constants.TAG, "Firebase error: $fireStoreException")
//                return@addSnapshotListener
//            }
//        userRef.add(user!!).addOnSuccessListener {
//            Log.d(Constants.TAG, "User added with ID: ${it.id}")
//            user!!.userID = it.id
//            userRef.document(it.id).set(user!!)
//        }

        //  }

        this.addEventSnapshotListener()
    }

    fun addEventSnapshotListener() {
        eventsRef
            .addSnapshotListener { snapshot, fireStoreException ->
                if (fireStoreException != null) {
                    Log.d(Constants.TAG, "Firebase error: $fireStoreException")
                    return@addSnapshotListener
                }
                processEventSnapshotDiffs(snapshot!!)

            }
    }

    fun processEventSnapshotDiffs(snapshot: QuerySnapshot) {

            for (documentChange in snapshot.documentChanges) {

                val event = Event.fromSnapshot(documentChange.document)

                when (documentChange.type) {
                    DocumentChange.Type.ADDED -> {
                       // if (!event.userMap.containsKey(user!!.userID)) {

                            Log.d(Constants.TAG, "Adding $event in Main")
                            eventList.add(0, event)
                        //}

                    }
                    DocumentChange.Type.REMOVED -> {
                      //   if (!event.userMap.containsKey(user!!.userID)) {
                        Log.d(Constants.TAG, "Removing $event in Main")
                        val index = eventList.indexOfFirst { it.id == event.id }
                        eventList.removeAt(index)
                   // }
                    }
                    DocumentChange.Type.MODIFIED -> {
                      //   if ( !event.userMap.containsKey(user!!.userID)) {
                        Log.d(Constants.TAG, "Modifying $event in Main")
                        val index = eventList.indexOfFirst { it.id == event.id }
                        eventList[index] = event
                       //  }
                    }
                }


        }
        swipeView.removeAllViews()
        for (event in eventList) {
            swipeView!!.addView(EventCard(this, event, cardViewHolderSize))
        }
        Log.d(Constants.TAG, "EventList: ${eventList}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        bottom_nav_view.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        initializeListeners()


        var name: String
        auth.currentUser.let {
            name = auth.currentUser!!.displayName!!
        }

        userRef.whereEqualTo("authID", authID).get().addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                isNewUser = true
                Toast.makeText(this, getString(R.string.welcome_new_text, name), Toast.LENGTH_SHORT).show()
                user = User()
                launchUserProfileDialog(name)
                Log.d(Constants.TAG, "New User with AUTH UID: ${authID} detected.")
            } else {
                isNewUser = false
                Toast.makeText(this, getString(R.string.welcome_text, name), Toast.LENGTH_SHORT).show()
                Log.d(Constants.TAG, "Existing AUTH UID is: ${authID}")
                for (doc in snapshot.documents) {
                    var docUser = User.fromSnapshot(doc)
                    if (docUser.authID == authID) {
                        user = docUser
                    }
                }
            }
        }

        val bottomMargin = Utils.dpToPx(margin)
        val windowSize = Utils.getDisplaySize(windowManager)
        swipeView!!.builder
            .setDisplayViewCount(3)
            .setIsUndoEnabled(true)
            .setSwipeVerticalThreshold(Utils.dpToPx(50))
            .setSwipeHorizontalThreshold(Utils.dpToPx(50))
            .setHeightSwipeDistFactor(10f)
            .setWidthSwipeDistFactor(5f)
            .setSwipeDecor(
                SwipeDecor()
                    .setViewWidth(windowSize.x)
                    .setViewHeight(windowSize.y - bottomMargin)
                    .setViewGravity(Gravity.TOP)
                    .setPaddingTop(20)
                    .setSwipeAnimTime(animationDuration)
                    .setRelativeScale(0.01f)
                    .setSwipeInMsgLayoutId(R.layout.card_event_swipe_in)
                    .setSwipeOutMsgLayoutId(R.layout.card_event_swipe_out)
            )


        cardViewHolderSize = Point(windowSize.x, windowSize.y - bottomMargin)


//        //TEST: Utils.getSampleEvents()
//        for (event in eventList) {
//            swipeView!!.addView(EventCard(this, event, cardViewHolderSize))
//        }

        rejectBtn.setOnClickListener({ swipeView!!.doSwipe(false) })

        acceptBtn.setOnClickListener({ swipeView!!.doSwipe(true) })

        undoBtn.setOnClickListener({ swipeView!!.undoLastSwipe() })

        swipeView!!.addItemRemoveListener {
            if (isToUndo) {
                isToUndo = false
                swipeView!!.undoLastSwipe()
            }
        }

        fab.hide()

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )

        drawer_layout.addDrawerListener(
            object : DrawerLayout.DrawerListener {
                override fun onDrawerStateChanged(newState: Int) {
                    // do nothing
                }

                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                    // do nothing
                }

                override fun onDrawerOpened(drawerView: View) {
                    drawerView.sign_out_button.setOnClickListener {
                        Log.d(Constants.TAG, "Sign out button clicked")
                        auth.signOut()
                    }
                    drawerView.account_edit_button.setOnClickListener {
                        editProfileDialog()
                    }
                }

                override fun onDrawerClosed(drawerView: View) {
                    //do nothing
                }
            })
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
    }


    fun onSwipeLeft(event: Event) {


        Log.d(Constants.SWIPE, "Current User ${user}")
        user!!.eventMap[event.eventID] = Constants.DENIED
        Log.d(Constants.SWIPE, "Event ID : ${event.eventID}")
        Log.d(Constants.SWIPE, "EventMap : ${user!!.eventMap}")
        userRef.document(user!!.userID).set(user!!)


//        var currentUser: User?
//        userRef.document(user!!.userID).get().addOnSuccessListener { document: DocumentSnapshot ->
//            currentUser = document.toObject(User::class.java)
//            Log.d(Constants.SWIPE, "Current User ${currentUser}")
//
//            currentUser!!.eventMap[event.eventID] = Constants.DENIED
//
//            Log.d(Constants.SWIPE, "Event ID : ${event.eventID}")
//            Log.d(Constants.SWIPE, "EventMap : ${currentUser!!.eventMap}")
//
//            userRef.document(user!!.userID).set(currentUser!!)
//        }

        Log.d(Constants.SWIPE, "Current Event $event")
        event.userMap[user!!.userID] = Constants.DENIED
        Log.d(Constants.SWIPE, "User ID ${user!!.userID}")
        Log.d(Constants.SWIPE, "UserMap ${event.userMap}")
        eventsRef.document(event.eventID).set(event)
        // var currentEvent: Event?
//        eventsRef.document(event.eventID).get().addOnSuccessListener {
//            currentEvent = it.toObject(Event::class.java)
//            Log.d(Constants.SWIPE, "Current Event $currentEvent")
//            currentEvent!!.userMap!![user!!.userID] = Constants.DENIED
//            Log.d(Constants.SWIPE, "User ID ${user!!.userID}")
//            Log.d(Constants.SWIPE, "UserMap ${currentEvent!!.userMap}")
//
//            eventsRef.document(event.eventID).set(currentEvent!!)
//
//        }
    }

    fun onSwipeRight(event: Event) {
        Log.d(Constants.SWIPE, "Current User ${user}")
        user!!.eventMap[event.eventID] = Constants.APPLIED
        Log.d(Constants.SWIPE, "Event ID : ${event.eventID}")
        Log.d(Constants.SWIPE, "EventMap : ${user!!.eventMap}")
        userRef.document(user!!.userID).set(user!!)

        Log.d(Constants.SWIPE, "Current Event $event")
        event.userMap[user!!.userID] = Constants.APPLIED
        Log.d(Constants.SWIPE, "User ID ${user!!.userID}")
        Log.d(Constants.SWIPE, "UserMap ${event.userMap}")
        eventsRef.document(event.eventID).set(event)
    }

    fun launchUserProfileDialog(name: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.user_profile_dialog_title))
        val view = LayoutInflater.from(this).inflate(R.layout.add_user_dialog, null, false)
        builder.setView(view)

        val spinner: Spinner = view.sex_drop_down
        ArrayAdapter.createFromResource(
            this,
            R.array.sex_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
        spinner.onItemSelectedListener

        val sex = spinner.selectedItem.toString()

        var photoUrl: String = ""
        var uri: Uri?
        auth.currentUser.let {
            uri = auth.currentUser!!.photoUrl
        }

        if (uri != null) {
            //setup user picture from that somehow
        } else {
            photoUrl = Utils.getSampleUserUrl()
        }
        val description = view.user_description_edit_text.text.toString()
        var age: Int = 0

        val dateButton = view.select_date_button

        dateButton.setOnClickListener {
            val now = Calendar.getInstance()
            val datePicker = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { picker, mYear, mMonth, mDay ->
                age = now.get(Calendar.YEAR) - mYear
                Log.d(Constants.TAG, "Age: $age")
                view.select_date_button.text = "Age: $age"
            }, now[Calendar.YEAR], now[Calendar.MONTH], now[Calendar.DATE])
            datePicker.show()
        }

        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            //TEST:
            user = User(authID!!, "", name, photoUrl, age, sex, 0, description)

            userRef.add(user!!).addOnSuccessListener {
                Log.d(Constants.TAG, "Dialog is adding user ID: ${it.id}")
                user!!.userID = it.id
                userRef.document(it.id).set(user!!)
            }
        }
        builder.setNegativeButton(android.R.string.cancel, null) // :)
        builder.create().show()
    }


    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authListener)
    }


    private fun initializeListeners() {
        authListener = FirebaseAuth.AuthStateListener { auth: FirebaseAuth ->
            val user = auth.currentUser
            Log.d(Constants.TAG, "User: $user")
            if (user == null) {
                switchToLoginActivity()
            }
        }
    }

    private fun switchToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    override fun onBackPressed() {
        if (backButtonCount >= 1) {
            finish()
        } else if (backButtonCount < 1) {
            Toast.makeText(this, getString(R.string.back_button_exit_toast), Toast.LENGTH_SHORT)
                .show()
            backButtonCount++
        } else if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)

        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        var fragment: android.support.v4.app.Fragment
        when (item.itemId) {
            R.id.bottom_nav_event_org -> {
                // startEventOrgActivity(uid!!)
                val ft = supportFragmentManager.beginTransaction()

                Log.d(Constants.TAG, "EventOrgFragment launching with user: $user with ID: ${user!!.userID}")
                ft.replace(
                    R.id.main_fragment_layout,
                    EventOrgFragment.newInstance(user!!.userID),
                    Constants.EVENT_ORG_FRAGMENT
                )
                ft.commit()
                return@OnNavigationItemSelectedListener true
            }
            R.id.bottom_nav_home -> {
                fragment = supportFragmentManager.findFragmentByTag(Constants.EVENT_ORG_FRAGMENT)
                if (fragment != null) {
                    supportFragmentManager.beginTransaction().detach(fragment).commit()
                    fab.hide()

                }
                return@OnNavigationItemSelectedListener true
            }
            R.id.bottom_nav_messages -> {

                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_hosted_events -> {
            }
            R.id.nav_gallery -> {

            }
            R.id.nav_slideshow -> {

            }
            R.id.nav_manage -> {

            }
            R.id.nav_share -> {

            }
            R.id.nav_send -> {

            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun editProfileDialog() {
        launchUserProfileDialog(auth.currentUser!!.displayName!!)
    }

    override fun onEventOrgFragmentSelected(event: Event) {
        val eventId = event.id
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(
            R.id.main_fragment_layout,
            AttendeeListFragment.newInstance(eventId),
            Constants.ATTENDEE_LIST_FRAGMENT
        )
        ft.addToBackStack(Constants.ATTENDEE_LIST_FRAGMENT)
        ft.commit()

    }

    fun getFab(): FloatingActionButton {
        return fab
    }

}
