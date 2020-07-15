package com.example.fitnessapp

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import java.net.URL

class ActivityMapsView : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var logID: Int = 0
    private var pathLog: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps_view)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        this.title = "Pregled aktivnosti"

        //receive log id from intent
        logID = intent.getIntExtra("ACTIVITY_ID", 0)

        //send get request for activity log
        CoroutineScope(Dispatchers.IO).launch{
            try{
                sendGetRequest()
            }catch(exception: Exception){
                withContext(Dispatchers.Main){
                    Toast.makeText(applicationContext, "Došlo je do pogreške prilikom učitavnja aktivnosti.", Toast.LENGTH_SHORT).show()
                }
            }
            var path: MutableList<LatLng> = mutableListOf()
            val pathStringList = pathLog.split("\n")
            for(i in 0 until pathStringList.size - 1){
                val latlngPair = pathStringList[i].split(" ")
                val lat: Double = latlngPair[0].toDouble()
                val lng: Double = latlngPair[1].toDouble()
                val location = LatLng(lat, lng)
                path.add(location)
            }
            withContext(Dispatchers.Main)
            {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(path[path.size-1]))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(path[path.size-1], 15f))
                mMap.addMarker(MarkerOptions()
                    .position(path[0])
                    .title("Početak")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)))
                mMap.addMarker(MarkerOptions()
                    .position(path[path.size-1])
                    .title("Kraj")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)))
                val polylineOptions = PolylineOptions().addAll(path).color(Color.MAGENTA).width(5f)
                mMap.addPolyline(polylineOptions)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    private suspend fun sendGetRequest(){
        val url = URL("https://hk-iot-team-02.azurewebsites.net/api/Logs/$logID")
        val logJSON = JSONObject(url.readText())
        pathLog = logJSON.getString("PathLog")
        println(pathLog)
    }

}
