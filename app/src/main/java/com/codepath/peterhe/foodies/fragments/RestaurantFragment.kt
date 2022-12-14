package com.codepath.peterhe.foodies.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.location.*
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codepath.peterhe.foodies.*
import com.parse.Parse.getApplicationContext
import com.parse.ParseGeoPoint
import com.parse.ParseUser
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import kotlin.collections.ArrayList

private const val BASE_URL = BuildConfig.BASE_URL
private const val API_KEY = BuildConfig.API_KEY

class RestaurantFragment : Fragment(), LocationListener {
    // declare a global variable of FusedLocationProviderClient
    //private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager
    private lateinit var address: String
    private lateinit var restaurants: ArrayList<YelpRestaurant>
    private lateinit var restaurantAdapter: RestaurantAdapter
    private lateinit var yelpHomeService: YelpHomeService
    private lateinit var yelpHomeSearchService: YelpHomeSearchService
    private var offset = 0
    private var searchQuery = ""

    // Store a member variable for the listener
    private lateinit var scrollListener: EndlessRecyclerViewScrollListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_restaurant, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        //handle logic
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }
        setHasOptionsMenu(true)
        //getView()?.setBackgroundColor(Color.WHITE)
        restaurants = arrayListOf<YelpRestaurant>()
        restaurantAdapter = RestaurantAdapter(requireContext(), restaurants)
        requireActivity().actionBar?.title = "Discover"
        // val fragmentManager: FragmentManager = supportFragmentManager
        val ft: FragmentTransaction? = getFragmentManager()?.beginTransaction()
        restaurantAdapter.setOnItemClickListner(object : RestaurantAdapter.onItemClickListner {
            override fun onItemClick(position: Int) {
                //final FragmentTransaction ft = getFragmentManager().beginTransaction();
                val bundle = Bundle()
                bundle.putParcelable("RestaurantDetail", restaurants[position])
                val DetailFragment = RestaurantDetailFragment()
                DetailFragment.setArguments(bundle)
                Log.i(TAG, "Restaurant ${restaurants[position]}")
                offset = 0
                scrollListener.resetState()
                ft?.replace(R.id.flContainer, DetailFragment)?.commit()
                requireActivity().setTitle("${restaurants[position].name}")
                ft?.addToBackStack(null)
            }
        })
        view.findViewById<RecyclerView>(R.id.rvRestaurants).adapter = restaurantAdapter
        val layoutManager = LinearLayoutManager(requireContext())
        view.findViewById<RecyclerView>(R.id.rvRestaurants).layoutManager = layoutManager
        view.findViewById<RecyclerView>(R.id.rvRestaurants).itemAnimator = SlideInUpAnimator()

        // Retain an instance so that you can call `resetState()` for fresh searches
        scrollListener = object : EndlessRecyclerViewScrollListener(layoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView) {
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to the bottom of the list
                loadNextDataFromApi()
            }
        }
        // Adds the scroll listener to RecyclerView
        view.findViewById<RecyclerView>(R.id.rvRestaurants).addOnScrollListener(scrollListener)

        val retrofit =
            Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create())
                .build()
        yelpHomeService = retrofit.create(YelpHomeService::class.java)
        yelpHomeSearchService = retrofit.create(YelpHomeSearchService::class.java)
        getLocation()

    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        try {
            locationManager =
                getApplicationContext().getSystemService(LOCATION_SERVICE) as LocationManager
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000,
                5.toFloat(),
                this
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    companion object {
        const val TAG = "RestaurentFragment"
        //private const val PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 100
    }

    override fun onLocationChanged(location: Location) {
        try {
            var geocoder: Geocoder = Geocoder(requireContext(), Locale.getDefault())
            var addresses: List<Address> =
                geocoder.getFromLocation(location.latitude, location.longitude, 1)
            address = addresses.get(0).getAddressLine(0)
            Log.i(TAG, address)
            yelpHomeService.searchRestaurants("Bearer $API_KEY", address, offset)
                .enqueue(object : Callback<YelpSearchResult> {
                    override fun onResponse(
                        call: Call<YelpSearchResult>,
                        response: Response<YelpSearchResult>
                    ) {
                        Log.i(TAG, "onResponse $response")
                        val body = response.body()
                        if (body == null) {
                            Log.w(TAG, "Did not receive valid response body.")
                            return
                        }
                        restaurants.addAll(body.restaurants)
                        offset += 20
                        restaurantAdapter.notifyDataSetChanged()
                    }

                    override fun onFailure(call: Call<YelpSearchResult>, t: Throwable) {
                        Log.i(TAG, "onFailure $t")
                    }
                })
            saveCurrentUserLocation()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // var user = ParseUser.getCurrentUser()

    }

    // Append the next page of data into the adapter
    // This method probably sends out a network request and appends new data items to your adapter.
    public fun loadNextDataFromApi() {
        // Send an API request to retrieve appropriate paginated data
        //  --> Send the request including an offset value (i.e `page`) as a query parameter.
        //  --> Deserialize and construct new model objects from the API response
        //  --> Append the new data objects to the existing set of items inside the array of items
        //  --> Notify the adapter of the new items made with `notifyItemRangeInserted()`
        if (searchQuery == "") {
            yelpHomeService.searchRestaurants("Bearer $API_KEY", address, offset)
                .enqueue(object : Callback<YelpSearchResult> {
                    override fun onResponse(
                        call: Call<YelpSearchResult>,
                        response: Response<YelpSearchResult>
                    ) {
                        Log.i(TAG, "onResponse $response")
                        val body = response.body()
                        if (body == null) {
                            Log.w(TAG, "Did not receive valid response body.")
                            return
                        }
                        restaurants.addAll(body.restaurants)
                        offset += 20
                        restaurantAdapter.notifyDataSetChanged()
                    }

                    override fun onFailure(call: Call<YelpSearchResult>, t: Throwable) {
                        Log.i(TAG, "onFailure $t")
                    }
                })
        } else {
            yelpHomeSearchService.searchRestaurants("Bearer $API_KEY", searchQuery, address, offset)
                .enqueue(object : Callback<YelpSearchResult> {
                    override fun onResponse(
                        call: Call<YelpSearchResult>,
                        response: Response<YelpSearchResult>
                    ) {
                        Log.i(TAG, "onResponse $response")
                        val body = response.body()
                        if (body == null) {
                            Log.w(TAG, "Did not receive valid response body.")
                            return
                        }
                        restaurants.addAll(body.restaurants)
                        offset += 20
                        restaurantAdapter.notifyDataSetChanged()
                    }

                    override fun onFailure(call: Call<YelpSearchResult>, t: Throwable) {
                        Log.i(TAG, "onFailure $t")
                    }
                })
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // super.onCreateOptionsMenu(menu, inflater)
        //inflater = MenuInflater(requireContext())
        inflater.inflate(R.menu.appbar, menu)
        menu.findItem(R.id.action_map_restaurantList).setOnMenuItemClickListener { item ->
            val bundle = Bundle()
            // val sublist:ArrayList<YelpRestaurant> = arrayListOf()
            //sublist.addAll(restaurants.subList(0,10))
            bundle.putParcelableArrayList("RestaurantList", restaurants)
            val DetailFragment = RestaurantListMapsFragment()
            DetailFragment.setArguments(bundle)
            offset = 0
            scrollListener.resetState()
            val ft: FragmentTransaction? = getFragmentManager()?.beginTransaction()
            ft?.replace(R.id.flContainer, DetailFragment)?.commit()
            ft?.addToBackStack(null)
            true
        }
        menu.findItem(R.id.action_cancel).setOnMenuItemClickListener { item ->
            // 1. First, clear the array of data
            restaurants.clear()
            // 2. Notify the adapter of the update
            restaurantAdapter.notifyDataSetChanged() // or notifyItemRangeRemoved
            // 3. Reset endless scroll listener when performing a new search
            scrollListener.resetState()
            offset = 0
            yelpHomeService.searchRestaurants("Bearer $API_KEY", address, offset)
                .enqueue(object : Callback<YelpSearchResult> {
                    override fun onResponse(
                        call: Call<YelpSearchResult>,
                        response: Response<YelpSearchResult>
                    ) {
                        Log.i(TAG, "onResponse $response")
                        val body = response.body()
                        if (body == null) {
                            Log.w(TAG, "Did not receive valid response body.")
                            return
                        }
                        restaurants.addAll(body.restaurants)
                        offset += 20
                        restaurantAdapter.notifyDataSetChanged()
                    }

                    override fun onFailure(call: Call<YelpSearchResult>, t: Throwable) {
                        Log.i(TAG, "onFailure $t")
                    }
                })
            menu.findItem(R.id.action_cancel).setVisible(false).setEnabled(false)
            searchQuery = ""
            requireActivity().setTitle("Discover")
            true
        }
        val searchItem = menu.findItem(R.id.action_search)
        val searchView: SearchView = MenuItemCompat.getActionView(searchItem) as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                // Fetch the data remotely
                Log.i(TAG, "Search submit!!!!")
                // 1. First, clear the array of data
                restaurants.clear()
                // 2. Notify the adapter of the update
                restaurantAdapter.notifyDataSetChanged() // or notifyItemRangeRemoved
                // 3. Reset endless scroll listener when performing a new search
                scrollListener.resetState()
                offset = 0
                yelpHomeSearchService.searchRestaurants("Bearer $API_KEY", query, address, offset)
                    .enqueue(object : Callback<YelpSearchResult> {
                        override fun onResponse(
                            call: Call<YelpSearchResult>,
                            response: Response<YelpSearchResult>
                        ) {
                            Log.i(TAG, "onResponse $response")
                            val body = response.body()
                            if (body == null) {
                                Log.w(TAG, "Did not receive valid response body.")
                                return
                            }
                            restaurants.addAll(body.restaurants)
                            offset += 20
                            restaurantAdapter.notifyDataSetChanged()
                        }

                        override fun onFailure(call: Call<YelpSearchResult>, t: Throwable) {
                            Log.i(TAG, "onFailure $t")
                        }
                    })
                menu.findItem(R.id.action_cancel).setVisible(true).setEnabled(true)
                searchQuery = query

                // Reset SearchView
                searchView.clearFocus()
                searchView.setQuery("", false)
                searchView.isIconified = true
                searchItem.collapseActionView()
                // Set activity title to search query
                requireActivity().setTitle(query)
                return true
            }

            override fun onQueryTextChange(s: String): Boolean {
                return false
            }
        })
        true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i(TAG, "Permission Request")
        if (requestCode == 1) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // if (ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                val ft: FragmentTransaction? = getFragmentManager()?.beginTransaction()
                ft?.replace(R.id.flContainer, RestaurantFragment())?.commit()
                requireActivity().setTitle("Discover")

                //}
            }

        }
    }

    private fun saveCurrentUserLocation() {
        // requesting permission to get user's location
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else {
            // getting last know user's location
            val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            // checking if the location is null
            if (location != null) {
                // if it isn't, save it to Back4App Dashboard
                val currentUserLocation = ParseGeoPoint(location.latitude, location.longitude)
                val currentUser = ParseUser.getCurrentUser()
                if (currentUser != null) {
                    currentUser.put("Location", currentUserLocation)
                    currentUser.saveInBackground()
                } else {
                    // do something like coming back to the login activity
                }
            } else {
                // if it is null, do something like displaying error and coming back to the menu activity
            }
        }
    }
}
