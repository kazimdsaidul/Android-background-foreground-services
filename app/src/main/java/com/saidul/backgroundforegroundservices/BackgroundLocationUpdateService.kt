package com.saidul.backgroundforegroundservices

import android.Manifest
import android.app.*
import android.content.Context
import java.lang.Runnable
import androidx.core.app.NotificationCompat
import android.util.Log
import com.saidul.backgroundforegroundservices.BackgroundLocationUpdateService
import java.lang.Exception
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import com.saidul.backgroundforegroundservices.MainActivity
import android.os.Build
import com.saidul.backgroundforegroundservices.R
import android.net.Uri
import android.media.RingtoneManager
import android.os.Handler
import com.google.android.gms.location.*
import java.util.concurrent.TimeUnit
import com.google.firebase.database.DatabaseReference

import com.google.firebase.database.FirebaseDatabase


class BackgroundLocationUpdateService : Service() {
    private var context: Context? = null
    private var client: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var stopService = false
    private var handler: Handler? = null
    private var runnable: Runnable? = null
    private var builder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? = null
    override fun onCreate() {
        Log.e(TAG, "Background Service onCreate :: ")
        super.onCreate()
        context = this
        handler = Handler()
        runnable = object : Runnable {
            override fun run() {
                try {
                    requestLocationUpdates()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    handler!!.postDelayed(this, TimeUnit.SECONDS.toMillis(2))
                }
            }
        }
        if (!stopService) {
            handler!!.postDelayed(runnable as Runnable, TimeUnit.SECONDS.toMillis(2))
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        Log.e(TAG, "onTaskRemoved :: ")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.e(TAG, "onStartCommand :: ")
        StartForeground()
        if (client != null) {
            client!!.removeLocationUpdates(locationCallback)
            Log.e(TAG, "Location Update Callback Removed")
        }


        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.e(TAG, "BackgroundService onDestroy :: ");
        stopService = true;
        if (handler != null) {
            handler!!.removeCallbacks(runnable!!);
        }
        if (client != null) {
            client!!.removeLocationUpdates(locationCallback);
            Log.e(TAG, "Location Update Callback Removed");
        }
    }




    private fun requestLocationUpdates() {
        val request = LocationRequest()
        request.setFastestInterval(100)
            .setInterval(200).priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        client = LocationServices.getFusedLocationProviderClient(this)
        val permission = intArrayOf(
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
        if (permission[0] == PackageManager.PERMISSION_GRANTED) {
            val location = arrayOf<Location?>(Location(LocationManager.GPS_PROVIDER))
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    Log.d(TAG, "onLocationResult ")
                    location[0] = locationResult.lastLocation
                    if (location[0] != null) {
                        setLocationToFCM(location[0])

                        Log.d(TAG, "location update " + location[0])
                        Log.d(
                            TAG, "location Latitude " + location[0]!!
                                .latitude
                        )
                        Log.d(
                            TAG, "location Longitude " + location[0]!!
                                .longitude
                        )
                        Log.d(
                            TAG, "Speed :: " + location[0]!!
                                .speed * 3.6
                        )
                        if (notificationManager != null && client != null && !stopService) {
                            builder!!.setContentText(
                                "Your current location is " + location[0]!!
                                    .latitude + "," + location[0]!!.longitude
                            )
                            notificationManager!!.notify(101, builder!!.build())
                        }
                    }
                }
            }
            client?.requestLocationUpdates(request, locationCallback, null)
        }
    }

    private fun setLocationToFCM(location: Location?) {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("androidbackgroundservicetest-default-rtdb")
        myRef.setValue(location).addOnFailureListener({
            print("")
        }).addOnSuccessListener {
            print("")
        }
        myRef.push()
    }

    /*-------- For notification ----------*/
    private fun StartForeground() {
        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0 /* Request code */,
            intent,
            PendingIntent.FLAG_ONE_SHOT
        )
        val CHANNEL_ID = "channel_location"
        val CHANNEL_NAME = "channel_location"
        notificationManager =
            applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            notificationManager!!.createNotificationChannel(channel)
            builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            builder!!.setColorized(false)
            builder!!.setChannelId(CHANNEL_ID)
            builder!!.color = ContextCompat.getColor(this, R.color.purple_200)
            builder!!.setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
        } else {
            builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
        }
        builder!!.setOnlyAlertOnce(true)
        builder!!.setContentTitle(context!!.resources.getString(R.string.app_name))
        builder!!.setContentText("Your current location is ")
        val notificationSound =
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_NOTIFICATION)
        builder!!.setSound(notificationSound)
        builder!!.setAutoCancel(true)
        builder!!.setSmallIcon(R.mipmap.ic_launcher)
        builder!!.setContentIntent(pendingIntent)
        startForeground(101, builder!!.build())
    }

    companion object {
        /**
         * Author：Hardik Talaviya
         * Date：  2019.08.3 2:30 PM
         * Describe:
         */
        private const val TAG = "BackgroundLocation"
    }
}