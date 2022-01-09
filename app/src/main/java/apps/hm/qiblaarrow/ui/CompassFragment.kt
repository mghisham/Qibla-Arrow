package apps.hm.qiblaarrow.ui

import android.Manifest
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import apps.hm.qiblaarrow.Compass
import apps.hm.qiblaarrow.GPSTracker
import apps.hm.qiblaarrow.PermissionHelper
import apps.hm.qiblaarrow.R
import apps.hm.qiblaarrow.databinding.FragmentCompassBinding
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class CompassFragment : Fragment() {
    private lateinit var locationManager: LocationManager
    private lateinit var binding: FragmentCompassBinding

    private var compass: Compass? = null
    private var currentAzimuth: Float = 0.toFloat()
    private lateinit var prefs: SharedPreferences
    private lateinit var gps: GPSTracker
    // Initializing permission helper.
    val cameraPermission by lazy {
        PermissionHelper(this@CompassFragment, Manifest.permission.CAMERA)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return if (::binding.isInitialized) {
            binding.root
        } else {
            binding = FragmentCompassBinding.inflate(inflater, container, false)
            with(binding) {
                root
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imgQiblaArrow.visibility = INVISIBLE
        binding.imgQiblaArrow.visibility = View.GONE
        prefs = requireContext().getSharedPreferences("", Context.MODE_PRIVATE)
        locationManager = context?.getSystemService(LOCATION_SERVICE) as LocationManager
        gps = GPSTracker(locationManager)
        setupCompass()
        binding.btnGps.setOnClickListener { fetchGPS() }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "start compass")
        compass?.start()
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "stop compass")
        compass?.stop()
    }

    private fun setupCompass() {
        binding.textKaabaDir.text = resources.getString(R.string.msg_permission_not_granted_yet)
        binding.textCurrentLoc.text =
            resources.getString(R.string.msg_permission_not_granted_yet)
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            1
        )
        compass = context?.let {
            val sensorManager = it.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            Compass(sensorManager)
        }
        compass?.setListener(object : Compass.CompassListener {
            override fun onNewAzimuth(azimuth: Float) {
                adjustGambarDial(azimuth)
                adjustArrowQiblat(azimuth)
            }
        })
    }


    private fun adjustGambarDial(azimuth: Float) {
        // Log.d(TAG, "will set rotation from " + currentAzimuth + " to "                + azimuth);

        val an = RotateAnimation(
            -currentAzimuth, -azimuth,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
            0.5f
        )
        currentAzimuth = azimuth
        an.duration = 500
        an.repeatCount = 0
        an.fillAfter = true
        binding.imgCompass.startAnimation(an)
    }

    private fun adjustArrowQiblat(azimuth: Float) {
        //Log.d(TAG, "will set rotation from " + currentAzimuth + " to "                + azimuth);

        val qiblaDir = retrieveFloat()
        val an = RotateAnimation(
            -currentAzimuth + qiblaDir, -azimuth,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
            0.5f
        )
        currentAzimuth = azimuth
        an.duration = 500
        an.repeatCount = 0
        an.fillAfter = true
        binding.imgQiblaArrow.startAnimation(an)
        if (qiblaDir > 0) {
            binding.imgQiblaArrow.visibility = View.VISIBLE
        } else {
            binding.imgQiblaArrow.visibility = INVISIBLE
            binding.imgQiblaArrow.visibility = View.GONE
        }
    }

    private fun getBearing() {
        // Get the location manager

        val qiblaDir = retrieveFloat()
        if (qiblaDir > 0.0001) {
            binding.textCurrentLoc.text =
                getString(R.string.your_location, gps.latitude, gps.longitude)
            binding.textKaabaDir.text = getString(R.string.qibla_direction, qiblaDir)
            // MenuItem item = menu.findItem(R.id.gps);

            binding.btnGps.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_my_location
                )
            )

            binding.imgQiblaArrow.visibility = View.VISIBLE
        } else {
            fetchGPS()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            1 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    getBearing()
                    binding.textKaabaDir.text = resources.getString(R.string.msg_permission_granted)
                    binding.textCurrentLoc.text =
                        resources.getString(R.string.msg_permission_granted)
                    binding.imgQiblaArrow.visibility = INVISIBLE
                    binding.imgQiblaArrow.visibility = View.GONE

                } else {
                    showToast(getString(R.string.toast_permission_required))
                }
                return
            }
        }// other 'case' lines to check for other
        // permissions this app might request
    }

    private fun saveFloat(value: Float?) {
        val edit = prefs.edit()
        edit.putFloat(KEY_LOC, value ?: 0f)
        edit.apply()
    }

    private fun retrieveFloat(value: String = KEY_LOC): Float {
        return prefs.getFloat(value, 0f)
    }

    private fun fetchGPS() {
        val result: Double
        gps = GPSTracker(locationManager)
        if (gps.canGetLocation()) {
            val latitude = gps.latitude
            val longitude = gps.longitude
            // \n is for new line
            binding.textCurrentLoc.text = getString(R.string.your_location, latitude, longitude)
            showToast(getString(R.string.your_location, latitude, longitude))
            Log.e("TAG", "GPS is on")
            if (latitude < 0.001 && longitude < 0.001) {
                // img_qibla_arrow.isShown(false);
                binding.imgQiblaArrow.visibility = INVISIBLE
                binding.imgQiblaArrow.visibility = View.GONE
                binding.textKaabaDir.text = resources.getString(R.string.location_not_ready)
                binding.textCurrentLoc.text = resources.getString(R.string.location_not_ready)
                binding.btnGps.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_my_location
                    )
                )
                showToast("Location not ready, Please Restart Application")
            } else {
                binding.btnGps.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_my_location
                    )
                )
                val longitude2 =
                    KA_BA_POSITION_LONGITUDE // ka'bah Position https://www.latlong.net/place/kaaba-mecca-saudi-arabia-12639.html
                val latitude2 =
                    Math.toRadians(KA_BA_POSITION_LATITUDE) // ka'bah Position https://www.latlong.net/place/kaaba-mecca-saudi-arabia-12639.html
                val latitude1 = Math.toRadians(latitude)
                val longDiff = Math.toRadians(longitude2 - longitude)
                val y = sin(longDiff) * cos(latitude2)
                val x =
                    cos(latitude1) * sin(latitude2) - sin(latitude1) * cos(latitude2) * cos(longDiff)
                result = (Math.toDegrees(atan2(y, x)) + 360) % 360
                val result2 = result.toFloat()
                saveFloat(value = result2)
                binding.textKaabaDir.text =
                    getString(R.string.qibla_direction, result2)
                Toast.makeText(
                    context,
                    getString(R.string.qibla_direction, result2),
                    Toast.LENGTH_LONG
                ).show()
                binding.imgQiblaArrow.visibility = View.VISIBLE

            }
            showToast(getString(R.string.show_location, latitude, longitude))
        } else {
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            showSettingsAlert()

            // img_qibla_arrow.isShown(false);
            binding.imgQiblaArrow.visibility = INVISIBLE
            binding.imgQiblaArrow.visibility = View.GONE
            binding.textKaabaDir.text = resources.getString(R.string.pls_enable_location)
            binding.textCurrentLoc.text = resources.getString(R.string.pls_enable_location)
            binding.btnGps.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_my_location
                )
            )
            showToast("Please enable Location first and Restart Application")
        }
    }

    /**
     * Function to show settings alert dialog
     * On pressing Settings button will lauch Settings Options
     */
    private fun showSettingsAlert() {
        val alertDialog = context?.let { AlertDialog.Builder(it) }
        // Setting Dialog Title
        alertDialog?.setTitle(getString(R.string.gps_settings_title))
        // Setting Dialog Message
        alertDialog?.setMessage(getString(R.string.gps_settings_text))
        // On pressing Settings button
        alertDialog?.setPositiveButton(getString(R.string.settings_button_ok)) { _, _ ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        // on pressing cancel button
        alertDialog?.setNegativeButton(getString(R.string.settings_button_cancel)) { dialog, _ -> dialog.cancel() }
        // Showing Alert Message
        alertDialog?.show()
    }

    companion object {
        private val TAG = CompassFragment::class.java.simpleName
        private const val KEY_LOC = "SAVED_LOC"
        private const val KA_BA_POSITION_LONGITUDE = 39.826206
        private const val KA_BA_POSITION_LATITUDE = 21.422487
    }
}