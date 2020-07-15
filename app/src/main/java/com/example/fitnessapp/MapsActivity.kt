package com.example.fitnessapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

//TODO #1.1: Stop the counter and location updates when pause is pressed and vice versa

data class LogDTO(var ID: Int, var ActivityName: String, var TotalTime: String, var TotalDistance: Float, var PathLog: String)

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var path: MutableList<LatLng>
    private lateinit var locationManager: LocationManager
    private var distanceTraversed: Float = 0f
    private var isPaused = false
    private var currentChronoValue: Long = 0
    private var activityName: String = ""

    inner class GListener : LocationListener {
        override fun onLocationChanged(location: Location?) {
            val newLocation = LatLng(location!!.latitude, location!!.longitude)
            mMap.clear()
            mMap.addMarker(MarkerOptions().position(newLocation).title("Trenutna lokacija").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(newLocation))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newLocation, mMap.maxZoomLevel))
            path.add(newLocation)
            var results:FloatArray = floatArrayOf(1f)
            Location.distanceBetween(
                path[path.size - 1].latitude,
                path[path.size - 1].longitude,
                path[path.size - 2].latitude,
                path[path.size - 2].longitude,
                results
            )
            distanceTraversed += results[0]
            var polylineOptions = PolylineOptions().addAll(path).color(Color.MAGENTA).width(5f)
            mMap.addPolyline(polylineOptions)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String?) {}

        override fun onProviderDisabled(provider: String?) {}
    }

    override fun onBackPressed() {
        displaySaveMessage()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //receive activity name from intent
        activityName = intent.getStringExtra("ACTIVITY_NAME")
        this.title = activityName

        //start time counter
        this.chronometer.start()

        //initialize path container
        path = mutableListOf<LatLng>()

        //check for necessary permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ){
            val permissions = arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                android.Manifest.permission.INTERNET
            )
            ActivityCompat.requestPermissions(this, permissions, 0)
        }

        //retrieve the best provider: GPS | Network
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val criteria = Criteria()
        val provider = locationManager.getBestProvider(criteria, false)
        val listener = GListener()

        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Toast.makeText(this, "Molimo uključite lokaciju kako bi mogli započeti aktivnost.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        else {
            locationManager.requestLocationUpdates(provider, 5000, 0f, listener)
        }

        this.buttonPause.setOnClickListener {
            if(isPaused)
            {
                this.buttonPause.text = "Pauza"
                locationManager.requestLocationUpdates(provider, 1000, 1f, listener)
                isPaused = false
                this.chronometer.base = SystemClock.elapsedRealtime() + currentChronoValue
                this.chronometer.start()
            }
            else {
                locationManager.removeUpdates(listener)
                this.buttonPause.text = "Nastavak"
                isPaused = true
                currentChronoValue = this.chronometer.base - SystemClock.elapsedRealtime()
                this.chronometer.stop()
            }
        }

        this.buttonStop.setOnClickListener {
            displaySaveMessage()
        }
    }

    private fun displaySaveMessage(){
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setMessage("Želite li spremiti aktivnost? Pritisnite tipku nazad za prekid.").setCancelable(true)
            .setPositiveButton("Da") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    try{
                        sendSaveRequest()
                    }catch(exception: Exception)
                    {
                        withContext(Dispatchers.Main){
                            Toast.makeText(applicationContext, "Došlo je do pogreške prilikom spremanja aktivnosti.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    val intent = Intent(applicationContext, MainActivity::class.java)
                    startActivity(intent)
                }
            }
            .setNegativeButton("Ne") { dialog, _ ->
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        val alert = alertDialog.create()
        alert.setTitle("Završetak")
        alert.show()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        //check necessary permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val permissions = arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                android.Manifest.permission.INTERNET
            )
            ActivityCompat.requestPermissions(this, permissions, 0)
        }
        //pinpoint current location of the user's device
        val currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val latlngLocation = LatLng(currentLocation.latitude, currentLocation.longitude)
        path.add(latlngLocation)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlngLocation, 15f))
        mMap.addMarker(MarkerOptions().position(latlngLocation).title("Trenutna lokacija").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)))
    }

    private fun pathToString(): String{
        var pathString = ""
        for(latlng in path){
            pathString += "${latlng.latitude} ${latlng.longitude}\n"
        }
        return pathString
    }

    private fun sendSaveRequest() {
        val time = this.chronometer.text.toString()
        val pathLog = pathToString()
        val logDTO = LogDTO(0, activityName, time, distanceTraversed, pathLog)
        val gson = GsonBuilder().create()
        val logJSON = gson.toJson(logDTO)

        val url: URL = URL("https://hk-iot-team-02.azurewebsites.net/api/Logs")
        with(url.openConnection() as HttpURLConnection){
            requestMethod = "POST"
            this.setRequestProperty("content-type", "application/json")
            val stream = OutputStreamWriter(outputStream)
            stream.write(logJSON)
            stream.flush()
            BufferedReader(InputStreamReader(inputStream)).use {
                val response = StringBuffer()
                var inputLine = it.readLine()
                while (inputLine != null) {
                    response.append(inputLine)
                    inputLine = it.readLine()
                    println(response)
                }
            }
        }
    }

}
