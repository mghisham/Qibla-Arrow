package apps.hm.qiblaarrow

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import apps.hm.qiblaarrow.databinding.FragmentCompassBinding
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class CompassFragment : Fragment() {
    private lateinit var binding: FragmentCompassBinding

    private var compass: Compass? = null
    private var currentAzimuth: Float = 0.toFloat()
    private lateinit var prefs: SharedPreferences
    private lateinit var gps: GPSTracker

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
        gps = GPSTracker(requireContext())
        setupCompass()
        binding.btnGps.setOnClickListener { fetchGPS() }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "start compass")
//        compass?.start()
    }

    override fun onPause() {
        super.onPause()
        compass?.stop()

    }

    override fun onResume() {
        super.onResume()
        compass?.start()
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "stop compass")
//        compass?.stop()
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
        compass = context?.let { Compass(it) }
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
                    Toast.makeText(
                        context,
                        resources.getString(R.string.toast_permission_required),
                        Toast.LENGTH_LONG
                    ).show()
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
        gps = GPSTracker(requireContext())
        if (gps.canGetLocation()) {
            val latitude = gps.latitude
            val longitude = gps.longitude
            // \n is for new line
            binding.textCurrentLoc.text = getString(R.string.your_location, latitude, longitude)
            // Toast.makeText(getApplicationContext(), "Lokasi anda: - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
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
                // Toast.makeText(getApplicationContext(), "Location not ready, Please Restart Application", Toast.LENGTH_LONG).show();
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
            //  Toast.makeText(getApplicationContext(), "lat_saya: "+lat_saya + "\nlon_saya: "+lon_saya, Toast.LENGTH_LONG).show();
        } else {
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert()

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
            // Toast.makeText(getApplicationContext(), "Please enable Location first and Restart Application", Toast.LENGTH_LONG).show();
        }
    }

    companion object {
        private val TAG = CompassFragment::class.java.simpleName
        private const val KEY_LOC = "SAVED_LOC"
        private const val KA_BA_POSITION_LONGITUDE = 39.826206
        private const val KA_BA_POSITION_LATITUDE = 21.422487
    }
}