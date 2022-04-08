package com.codepath.peterhe.foodies

import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.codepath.peterhe.foodies.fragments.GroupChatListFragment
import com.codepath.peterhe.foodies.fragments.RestaurantFragment
import com.codepath.peterhe.foodies.fragments.UserProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.parse.*

class MainActivity : AppCompatActivity() {
    private lateinit var appbar: Toolbar
    private lateinit var fragmentManager: FragmentManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.my_toolbar))
        //ParseUser.getCurrentUser().getParseFile("profile")
        // val image: ParseFile? = ParseUser.getCurrentUser().getParseFile("profile")
        //Log.i("Main", image?.url.toString())
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;//  set status text dark
        //window.statusBarColor = resources.getColor(R.color.primary_dark)
        getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.light_gray_1))
        // queryUser(ParseUser.getCurrentUser().objectId)
        val image: ParseFile? = ParseUser.getCurrentUser().getParseFile("profile")
        Log.i("Main", image?.url.toString())
        Glide.with(this@MainActivity).load(image?.url).override(32, 32).apply(
            RequestOptions().transforms(
                CenterCrop(), RoundedCorners(10)
            )
        ).listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                p0: GlideException?,
                p1: Any?,
                p2: Target<Drawable>?,
                p3: Boolean
            ): Boolean {
                //Log.e(TAG, "onLoadFailed")
                //do something if error loading
                return false
            }

            override fun onResourceReady(
                p0: Drawable?,
                p1: Any?,
                p2: Target<Drawable>?,
                p3: DataSource?,
                p4: Boolean
            ): Boolean {
                //Log.d(TAG, "OnResourceReady")
                //do something when picture already loaded
                //setColorStateList()
                //findViewById<BottomNavigationView>(R.id.bottom_navigation).setItemIconTintList(null)
                findViewById<BottomNavigationView>(R.id.bottom_navigation).getMenu().getItem(2)
                    .setIconTintMode(PorterDuff.Mode.DST)
                findViewById<BottomNavigationView>(R.id.bottom_navigation).getMenu().getItem(2)
                    .setIcon(p0)
                /*findViewById<BottomNavigationView>(R.id.bottom_navigation).getMenu().getItem(0).getIcon().setColorFilter(
                    getResources().getColor(R.color.app_black), PorterDuff.Mode.SRC_ATOP)
                findViewById<BottomNavigationView>(R.id.bottom_navigation).getMenu().getItem(1).getIcon().setColorFilter(
                    getResources().getColor(R.color.app_black), PorterDuff.Mode.SRC_ATOP)*/
                return false
            }
        }).into(findViewById<ImageView>(R.id.iv_profilePlaceHolder))
        fragmentManager = supportFragmentManager
        findViewById<BottomNavigationView>(R.id.bottom_navigation).setBackgroundColor(
            getResources().getColor(
                R.color.primary
            )
        )
        findViewById<BottomNavigationView>(R.id.bottom_navigation).setOnItemSelectedListener { item ->
            var fragmentToShow: Fragment? = null
            when (item.itemId) {
                R.id.action_home -> {
                    this.supportActionBar?.title = "Discover"
                    fragmentToShow = RestaurantFragment()
                }
                R.id.action_chats -> {
                    this.supportActionBar?.title = "Message"
                    fragmentToShow = GroupChatListFragment()
                }
                R.id.action_profile -> {
                    this.supportActionBar?.title = "Profile"
                    fragmentToShow = UserProfileFragment()
                }
            }
            if (fragmentToShow != null) {
                fragmentManager.beginTransaction().replace(R.id.flContainer, fragmentToShow)
                    .commit()
            }
            true
        }
        // Set default selection
        findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.action_home
    }

    fun queryUser(userId: String) {
        val query: ParseQuery<ParseUser> = ParseQuery.getQuery(ParseUser::class.java)
        query.include(ParseUser.KEY_OBJECT_ID)
        query.limit = 1
        query.whereEqualTo(ParseUser.KEY_OBJECT_ID, userId)
        query.findInBackground(object : FindCallback<ParseUser> {
            override fun done(user: MutableList<ParseUser>?, e: ParseException?) {
                if (e != null) {
                    //Log.e(TAG, "Error getting posts")
                    Toast.makeText(this@MainActivity, "Error getting members", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    if (user != null && user.size == 1) {
                        val image: ParseFile? = user[0].getParseFile("profile")
                        Log.i("Main", image?.url.toString())
                        Glide.with(this@MainActivity).load(image?.url).override(32, 32).apply(
                            RequestOptions().transforms(
                                CenterCrop(), RoundedCorners(10)
                            )
                        ).listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                p0: GlideException?,
                                p1: Any?,
                                p2: Target<Drawable>?,
                                p3: Boolean
                            ): Boolean {
                                //Log.e(TAG, "onLoadFailed")
                                //do something if error loading
                                return false
                            }

                            override fun onResourceReady(
                                p0: Drawable?,
                                p1: Any?,
                                p2: Target<Drawable>?,
                                p3: DataSource?,
                                p4: Boolean
                            ): Boolean {
                                //Log.d(TAG, "OnResourceReady")
                                //do something when picture already loaded
                                findViewById<BottomNavigationView>(R.id.bottom_navigation).getMenu()
                                    .getItem(2).setIconTintMode(PorterDuff.Mode.DST)
                                findViewById<BottomNavigationView>(R.id.bottom_navigation).getMenu()
                                    .getItem(2).setIcon(p0)
                                return false
                            }
                        }).into(findViewById<ImageView>(R.id.iv_profilePlaceHolder))

                    }
                }
            }

        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i(RestaurantFragment.TAG, "Permission Request")
        if (requestCode == 1) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // if (ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
               // val ft: FragmentTransaction? = getFragmentManager()?.beginTransaction()
                fragmentManager.beginTransaction().replace(R.id.flContainer, RestaurantFragment())
                    .commit()

                //}
            }

        }
    }


}