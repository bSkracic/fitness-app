package com.example.fitnessapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class GPSActivity(var ID: Int, var TotalTime: String, var TotalDistance: Double, var ActivityName: String){
}

class MainActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var logs: MutableList<GPSActivity>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            //check if location is enabled
            if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Toast.makeText(this, "Molimo uključite lokaciju kako bi započeli aktivnost.", Toast.LENGTH_SHORT).show()
            }else{
                val intent = Intent(this, NewActivityStart::class.java)
                startActivity(intent)
            }
        }

        //launch async GET request
        logs = mutableListOf()
        CoroutineScope(Dispatchers.IO).launch {
            try{
                getLogsFromAPI()
            }catch(exception: Exception){
                withContext(Dispatchers.Main){
                    Toast.makeText(applicationContext, "Došlo je do pogreške! Provjerite mrežnu vezu.", Toast.LENGTH_SHORT).show()
                }
            }
        }

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
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        super.onBackPressed()
    }

    override fun onItemClickedListener(log: GPSActivity) {
        val intent = Intent(this, ActivityMapsView::class.java)
        intent.putExtra("ACTIVITY_ID", log.ID)
        startActivity(intent)
    }

    override fun onDeleteButtonClickedListener(log: GPSActivity) {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setMessage("Jeste li sigurni da želite izbrisati aktivnost:\n${log.ActivityName}?").setCancelable(false)
            .setPositiveButton("Da") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch{
                    try{
                        sendDeleteRequest(log.ID)
                    }catch(exception: Exception){
                        withContext(Dispatchers.Main){
                            Toast.makeText(applicationContext, "Pogreška prilikom brisanja stavke!", Toast.LENGTH_LONG).show()
                        }
                    }
                    val intent = Intent(applicationContext, MainActivity::class.java)
                    startActivity(intent)
                }
            }
            .setNegativeButton("Ne") { dialog, _ ->
                dialog.cancel()
            }
        val alert = alertDialog.create()
        alert.setTitle("Brisanje aktivnosti")
        alert.show()
    }

    private suspend fun getLogsFromAPI() {
        val url = URL("https://hk-iot-team-02.azurewebsites.net/api/Logs")
        val logsJSON = JSONArray(url.readText())

        for(i in 0 until logsJSON.length()){
            val log: JSONObject = logsJSON.getJSONObject(i)
            logs.add(
                GPSActivity(
                    log.getInt("ID"),
                    log.getString("TotalTime"),
                    log.getDouble("TotalDistance"),
                    log.getString("ActivityName")
                    )
                )
        }

        withContext(Dispatchers.Main){
            populateRecyclerView()
        }
    }

    private fun populateRecyclerView() {
        this.logsRecyclerView.adapter = LogsAdapter(logs, this)
        this.logsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun sendDeleteRequest(id: Int) {
        val url: URL = URL("https://hk-iot-team-02.azurewebsites.net/api/Logs/$id")
        with(url.openConnection() as HttpURLConnection){
            requestMethod = "DELETE"
            BufferedReader(InputStreamReader(inputStream)).use {
                val response = StringBuffer()

                var inputLine = it.readLine()
                while (inputLine != null) {
                    response.append(inputLine)
                    inputLine = it.readLine()
                }
                it.close()
            }
        }
    }


}
