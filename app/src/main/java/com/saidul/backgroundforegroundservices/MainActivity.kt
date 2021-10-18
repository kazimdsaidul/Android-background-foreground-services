package com.saidul.backgroundforegroundservices

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import com.saidul.backgroundforegroundservices.databinding.ActivityMainBinding
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        onUserInputControl(binding)


        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("message")
        myRef.setValue("Hello, World!").addOnFailureListener {

        }

    }

    private fun onUserInputControl(binding: ActivityMainBinding) {

        val btnForgroudLocationService = findViewById<Button>(R.id.btn_forgroud_location_service)

        if (foregroundServiceRunning()) {
            btnForgroudLocationService.text = getString(R.string.stop_service)
        } else {
            btnForgroudLocationService.text = getString(R.string.start_foreground_location_service)
        }

        btnForgroudLocationService.setOnClickListener {
            if (!foregroundServiceRunning()) {
                val serviceIntent = Intent(this, BackgroundLocationUpdateService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                    btnForgroudLocationService.setText("Stop service")
                } else {
                    startService(serviceIntent)
                    btnForgroudLocationService.setText("Stop service")
                }
            } else {
                val serviceIntent = Intent(this, BackgroundLocationUpdateService::class.java)
                val stopService = stopService(serviceIntent)
                if (stopService) {
                    btnForgroudLocationService.setText(getString(R.string.start_foreground_location_service))
                }

            }
        }


    }

    fun foregroundServiceRunning(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (BackgroundLocationUpdateService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }


}