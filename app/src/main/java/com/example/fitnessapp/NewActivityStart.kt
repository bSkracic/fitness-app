package com.example.fitnessapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_new_start.*

class NewActivityStart : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_start)

        this.buttonCreateNewActivity.setOnClickListener {
            val activityName = editActivityName.text.toString()
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("ACTIVITY_NAME", activityName)
            startActivity(intent)
        }
    }
}
