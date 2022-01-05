package apps.hm.qiblaarrow

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log

class GPSTracker(private val mContext: Context) : Service(), LocationListener {
    // Declaring a Location Manager
    private lateinit var locationManager: LocationManager

    // flag for GPS status
    private var isGPSEnabled = false

    // flag for network status
    private var isNetworkEnabled = false

    // flag for GPS status
    private var canGetLocation = false
    private var location: Location? = null // location
        set(value) {
            latitude = value?.latitude ?: 0.0
            longitude = value?.longitude ?: 0.0
            field = value
        }
    var latitude = 0.0 // latitude
    var longitude = 0.0 // longitude

    init {
        fetchLocation()
    }

    @SuppressLint("MissingPermission")
    fun fetchLocation(): Location? {
        try {
            locationManager = mContext
                .getSystemService(LOCATION_SERVICE) as LocationManager

            // getting GPS status
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            // getting network status
            isNetworkEnabled = locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            } else {
                canGetLocation = true
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this
                    )
                    Log.d("Network", "Network")
                    location =
                        locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this
                        )
                        Log.d("GPS Enabled", "GPS Enabled")
                        location =
                            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return location
    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     */
    fun stopUsingGPS() {
        locationManager.removeUpdates(this@GPSTracker)
    }

    /**
     * Function to check GPS/wifi enabled
     *
     * @return boolean
     */
    fun canGetLocation(): Boolean {
        return canGetLocation
    }

    /**
     * Function to show settings alert dialog
     * On pressing Settings button will lauch Settings Options
     */
    fun showSettingsAlert() {
        val alertDialog = AlertDialog.Builder(mContext)
        // Setting Dialog Title
        alertDialog.setTitle(mContext.resources.getString(R.string.gps_settings_title))
        // Setting Dialog Message
        alertDialog.setMessage(mContext.resources.getString(R.string.gps_settings_text))
        // On pressing Settings button
        alertDialog.setPositiveButton(mContext.resources.getString(R.string.settings_button_ok)) { dialog, which ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            mContext.startActivity(intent)
        }
        // on pressing cancel button
        alertDialog.setNegativeButton(mContext.resources.getString(R.string.settings_button_cancel)) { dialog, which -> dialog.cancel() }
        // Showing Alert Message
        alertDialog.show()
    }

    override fun onLocationChanged(location: Location) = Unit

    override fun onProviderDisabled(provider: String) = Unit

    override fun onProviderEnabled(provider: String) = Unit

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) = Unit

    override fun onBind(intent: Intent): IBinder? = null

    companion object {
        // The minimum distance to change Updates in meters
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Long = 100 // 10 meters

        // The minimum time between updates in milliseconds
        private const val MIN_TIME_BW_UPDATES = (1000 * 60 * 1).toLong() // 1 minute
    }
}