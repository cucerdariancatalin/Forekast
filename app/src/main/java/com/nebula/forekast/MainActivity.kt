package com.nebula.forekast

import android.Manifest
import android.app.Dialog
import android.app.DialogFragment
import android.app.SearchManager
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.AsyncTask
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.SearchView
import android.text.Html
import android.text.format.DateFormat
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.nebula.forekast.fragment.DatePickerFragment
import org.json.JSONObject
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DatePickerFragment.OnDateSelectedListener{

    val PERMISSION_REQUEST_LOCATION : Int = 20
    val TAG : String = javaClass.simpleName
    lateinit var mGoogleApiClient : GoogleApiClient
    lateinit var mLastLocation : Location
    lateinit var mContext : Context
    lateinit var weatherFont : Typeface

    // views
    lateinit var coordinatorLayout : CoordinatorLayout
    lateinit var locationView : TextView
    lateinit var weatherIcon : ImageView
    lateinit var summaryView : TextView
    lateinit var tempView : TextView

    lateinit var hourlySummary : TextView
    lateinit var rainView : TextView
    lateinit var windView : TextView
    lateinit var sunriseView : TextView
    lateinit var sunsetView : TextView
    lateinit var progressBar : ProgressBar
    lateinit var dateButton : FloatingActionButton

    // lat, lng
    lateinit var mLatLng : LatLng



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mContext = this
        weatherFont = Typeface.createFromAsset(assets, "weathericons-regular-webfont.ttf")

        // views
        coordinatorLayout = findViewById(R.id.coordinator_layout) as CoordinatorLayout
        locationView = findViewById(R.id.location) as TextView
        weatherIcon = findViewById(R.id.weather_icon) as ImageView
        summaryView = findViewById(R.id.current_summary) as TextView
        tempView = findViewById(R.id.temperature) as TextView

        hourlySummary = findViewById(R.id.weather_description) as TextView
        rainView = findViewById(R.id.rain_chance) as TextView
        windView = findViewById(R.id.wind_speed) as TextView
        sunriseView = findViewById(R.id.sunrise_time) as TextView
        sunsetView = findViewById(R.id.sunset_time) as TextView
        progressBar = findViewById(R.id.progressbar) as ProgressBar
        dateButton = findViewById(R.id.dateButton) as FloatingActionButton

        // listeners
        dateButton.setOnClickListener {
            createCalendarDialog()
        }

        // check play service availability
        if (googleServiceAvailable())
            createGoogleApiClient()

    }

    override fun onStart() {
        super.onStart()
        mGoogleApiClient.connect()
    }


    override fun onResume() {
        super.onResume()
        if (checkLocationPermission()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //googleServiceAvailable()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater : MenuInflater = menuInflater
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_search ->  {
                var searchView : android.support.v7.widget.SearchView = item.actionView as android.support.v7.widget.SearchView
                val searchManager : SearchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
                searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextChange(p0: String?): Boolean {
                        return false
                    }

                    override fun onQueryTextSubmit(p0: String?): Boolean {
                        if (p0 != null && p0.isNotEmpty()) {
                            progressBar.visibility = View.VISIBLE
                            search(p0)
                        }
                        return false

                    }
                })
                return true
            }
            R.id.action_locate -> {
                if (mGoogleApiClient.isConnected)
                    mGoogleApiClient.reconnect()
                else
                    mGoogleApiClient.connect()
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    fun checkLocationPermission() : Boolean {
           if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                   != PackageManager.PERMISSION_GRANTED) {

               // show explanation?
               if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                   val alertDialog = AlertDialog.Builder(this)
                   alertDialog.setTitle(getString(R.string.title_location_permission))
                   alertDialog.setMessage(getString(R.string.text_location_permission))
                   alertDialog.setPositiveButton(R.string.okButton, { _, _ ->
                       ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                               PERMISSION_REQUEST_LOCATION)
                   }).create()
                           .show()
               } else {
                   // no explanation needed, request permission
                   ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                           PERMISSION_REQUEST_LOCATION)
               }
               return false
           } else
               return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode){
            PERMISSION_REQUEST_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        //Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    //Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                    finish()
                }
                return
            }
        }
    }

    fun googleServiceAvailable() : Boolean {
        val api : GoogleApiAvailability = GoogleApiAvailability.getInstance()
        val isAvailable : Int = api.isGooglePlayServicesAvailable(this)
        if (isAvailable == ConnectionResult.SUCCESS)
            return true
        else if (api.isUserResolvableError(isAvailable)) {
            val dialog : Dialog = api.getErrorDialog(this, isAvailable, 0)
            dialog.show()
        } else
            Toast.makeText(this, "Can't connect to Google Play Services", Toast.LENGTH_LONG).show()
        return false
    }

    fun createGoogleApiClient() : Unit {
        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build()
    }

    override fun onConnected(p0: Bundle?) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
        val latLng : LatLng = LatLng(mLastLocation.latitude, mLastLocation.longitude)
        mLatLng = latLng
        progressBar.visibility = View.VISIBLE
        fetchLocationData(latLng)
        fetchWeatherData(latLng)

    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        val snackbar : Snackbar = Snackbar
                .make(coordinatorLayout, "Lost connection to location service", Snackbar.LENGTH_LONG)
                .setAction("RETRY", { _ -> mGoogleApiClient.reconnect()  })
        snackbar.show()
    }

    override fun onConnectionSuspended(p0: Int) {
        val snackbar : Snackbar = Snackbar
                .make(coordinatorLayout, "Location service suspended", Snackbar.LENGTH_LONG)
                .setAction("RETRY", { _ -> mGoogleApiClient.reconnect()  })
        snackbar.show()
    }

    // get city, state
    fun fetchLocationData(latLng : LatLng) {
        object : AsyncTask<Void, Void, String>(){
            override fun doInBackground(vararg p0: Void?): String {
                val geocoder : Geocoder = Geocoder(mContext, Locale.getDefault())
                var addresses : List<Address>? = null
                var errorMessage : String = ""
                var city : String = ""

                try {
                    addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                } catch (ioException : IOException){
                    errorMessage = "Location service not available"
                    Log.e(TAG, errorMessage, ioException)
                } catch (illegalArgumentException : IllegalArgumentException){
                    errorMessage = "Invalid latitude/longitude"
                    Log.e(TAG, errorMessage + ". " +
                            "Latitude = " + latLng.latitude +
                            ", Longitude = " +
                            latLng.longitude, illegalArgumentException)
                }

                // handle case where no address was found
                if (addresses == null || addresses.isEmpty()){
                    if (errorMessage.isEmpty()) {
                        errorMessage = "Location not found :("
                        Log.e(TAG, errorMessage)
                    }
                } else { // success
                    val address : Address = addresses.get(0)
                    city = address.locality + ", " + address.countryName
                }

                return city
            }

            override fun onPostExecute(result: String?) {
                if (result != null && result.isNotEmpty())
                    locationView.text = result
                else
                    locationView.text = resources.getString(R.string.locationUnavailable)
            }
        }.execute()
    }

    fun fetchWeatherData(latLng : LatLng) {
        val basePath: String = resources.getString(R.string.darkSkyUrl)
        val path: String = basePath + latLng.latitude + "," +
                latLng.longitude

        val jsonObjReq = object : JsonObjectRequest(Method.GET, path, null,
                Response.Listener<JSONObject> { response ->
                    parseJson(response)
                },
                Response.ErrorListener { error ->
                    Log.d(TAG, "/Get request fail! Error: ${error.message}")
                    progressBar.visibility = View.GONE
                    val snackbar : Snackbar = Snackbar
                            .make(coordinatorLayout, "Oops! Data retrieval failed", Snackbar.LENGTH_INDEFINITE)
                            .setAction("RETRY", { _ -> mGoogleApiClient.reconnect()  })

                    snackbar.show()
                }) {}

        Volley.newRequestQueue(this).add(jsonObjReq)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun parseJson(response : JSONObject) {

        val currently : JSONObject = response.getJSONObject("currently")
        val data : JSONObject = response.getJSONObject("daily").getJSONArray("data").getJSONObject(0)
        val hourly : JSONObject = response.getJSONObject("hourly").getJSONArray("data").getJSONObject(0)
        val rainProb : Double? = hourly.getDouble("precipProbability")
        val time : Int = currently.getInt("time")
        val summary : String = currently.getString("summary")
        val weatherSummary : String = response.getJSONObject("hourly").getString("summary")
        val icon : String = currently.getString("icon")
        val temp : Double = currently.getDouble("temperature")
        val windSpeed : Double = currently.getDouble("windSpeed")
        val sunset : Long = data.getLong("sunsetTime")
        val sunrise : Long = data.getLong("sunriseTime")

        val iconDrawable : Drawable
        when (icon){
            "rainy" -> {
                if (time in sunrise..(sunset - 1))
                    iconDrawable = resources.getDrawable(R.drawable.ic_rainy_day, null)
                else
                    iconDrawable = resources.getDrawable(R.drawable.ic_rainy_night, null)
            }
            "clear-night" -> {iconDrawable = resources.getDrawable(R.drawable.ic_clear_night, null)}
            "clear-day" -> {iconDrawable = resources.getDrawable(R.drawable.ic_sun, null)}
            "partly-cloudy-day" -> {iconDrawable = resources.getDrawable(R.drawable.ic_cloudy_day, null)}
            "partly-cloudy-night" -> {iconDrawable = resources.getDrawable(R.drawable.ic_cloudy_night, null)}
            "cloudy" -> {iconDrawable = resources.getDrawable(R.drawable.ic_cloudy, null)}
            "sleet" -> {iconDrawable = resources.getDrawable(R.drawable.ic_sleet, null)}
            "snow" -> {iconDrawable = resources.getDrawable(R.drawable.ic_snow, null)}
            "wind" -> {iconDrawable = resources.getDrawable(R.drawable.ic_windy, null)}
            "fog" -> {iconDrawable = resources.getDrawable(R.drawable.ic_fog, null)}
            else -> {
                if (time in sunrise..(sunset - 1))
                    iconDrawable =  resources.getDrawable(R.drawable.ic_sun, null)
                else
                    iconDrawable = resources.getDrawable(R.drawable.ic_clear_night, null)
            }
        }
        // data -> view
        progressBar.visibility = View.GONE
        tempView.text = temp.toString() + " \u2109"
        summaryView.text = summary
        hourlySummary.text = weatherSummary
        weatherIcon.setImageDrawable(iconDrawable)
        rainView.text = "%.1f".format(rainProb?.times(100)) + "%"
        windView.text = windSpeed.toInt().toString() + "m/s"
        sunriseView.text = getDate(sunrise)
        sunsetView.text = getDate(sunset)
    }

    fun createCalendarDialog(){

        val datePickerFrag : DialogFragment = DatePickerFragment.newInstance()
        datePickerFrag.show(fragmentManager, "datePicker")
    }


    // display weather info at alternate location
    fun search(location : String){
        object : AsyncTask<Void, Void, MutableList<String>>(){
            override fun doInBackground(vararg p0: Void?): MutableList<String> {

                val arr: MutableList<String> = mutableListOf<String>()
                if (!Geocoder.isPresent())
                    return arr
                val geocoder : Geocoder = Geocoder(mContext, Locale.getDefault())
                var addresses : List<Address>? = null
                var errorMessage : String = ""
                val city : String


                try {
                    addresses = geocoder.getFromLocationName(location, 1)
                } catch (ioException : IOException){
                    errorMessage = "Location service not available"
                    Log.e(TAG, errorMessage, ioException)
                } catch (illegalArgumentException : IllegalArgumentException){
                    errorMessage = "Invalid location name"
                    Log.e(TAG, errorMessage)
                }

                // handle case where no address was found
                if (addresses == null || addresses.isEmpty()){
                    if (errorMessage.isEmpty()) {
                        errorMessage = "Location not found :("
                        Log.e(TAG, errorMessage)
                    }
                } else { // success
                    val address : Address = addresses[0]
                    city = address.locality + ", " + address.countryName
                    arr.add(city)
                    Log.d("latLng", address.latitude.toString() + "  " + address.longitude)
                    arr.add(address.latitude.toString())
                    arr.add(address.longitude.toString())
                }

                return arr

            }

            override fun onPostExecute(result: MutableList<String>?) {
                if (result != null && result.isNotEmpty()){
                    locationView.text = result[0]
                    val latLng : LatLng = LatLng(result[1].toDouble(), result[2].toDouble())
                    mLatLng = latLng
                    fetchWeatherData(latLng)
                } else {
                    progressBar.visibility = View.GONE
                    val snackbar : Snackbar = Snackbar
                            .make(coordinatorLayout, "Location not found", Snackbar.LENGTH_LONG)
                    snackbar.show()
                }
            }
        }.execute()
    }

    override fun onDateSelected(date: Date) {
        fetchFutureWeatherData(date.time / 1000)

    }

    // get future weather of current city
    //TODO: query excluding "currently" data
    fun fetchFutureWeatherData(time : Long) {

        val basePath: String = resources.getString(R.string.darkSkyUrl)
        val path: String = basePath + mLatLng.latitude + "," +
                mLatLng.longitude + "," + time

        progressBar.visibility = View.VISIBLE
        val jsonObjReq = object : JsonObjectRequest(Method.GET, path, null,
                Response.Listener<JSONObject> { response ->
                    parseJson(response)
                },
                Response.ErrorListener { error ->
                    Log.d(TAG, "/Get request fail! Error: ${error.message}")
                    progressBar.visibility = View.GONE
                    val snackbar : Snackbar = Snackbar
                            .make(coordinatorLayout, "Oops! Data retrieval failed", Snackbar.LENGTH_INDEFINITE)
                            .setAction("RETRY", { _ -> mGoogleApiClient.reconnect()  })

                    snackbar.show()
                }) {}

        Volley.newRequestQueue(this).add(jsonObjReq)
    }


    fun getDate(time : Long) : String {
        val date : String = DateFormat.format("hh:mm a", time * 1000L).toString()
        return date
    }
}
