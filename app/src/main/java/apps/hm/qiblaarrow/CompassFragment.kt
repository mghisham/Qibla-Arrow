package apps.hm.qiblaarrow

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import apps.hm.qiblaarrow.databinding.FragmentCompassBinding

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

        //////////////////////////////////////////
        binding.imgQiblaArrow.visibility = INVISIBLE
        binding.imgQiblaArrow.visibility = View.GONE
        prefs = requireContext().getSharedPreferences("", Context.MODE_PRIVATE)
        gps = GPSTracker(context)
        setupCompass()
        binding.btnGps.setOnClickListener { fetchGPS() }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "start compass")
        if (compass != null) {
            compass?.start()
        }

    }

    override fun onPause() {
        super.onPause()
        if (compass != null) {
            compass?.stop()
        }
    }

    override fun onResume() {
        super.onResume()
        if (compass != null) {
            compass?.start()
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "stop compass")
        if (compass != null) {
            compass?.stop()
        }
    }

    private fun setupCompass() {
        val permissionGranted = GetBoolean("permission_granted")
        if (permissionGranted!!) {
            getBearing()
        } else {
            binding.textKaabaDir.text = resources.getString(R.string.msg_permission_not_granted_yet)
            binding.textCurrentLoc.text =
                resources.getString(R.string.msg_permission_not_granted_yet)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    1
                )
            }
        }

        compass = Compass(context)
        val cl = Compass.CompassListener { azimuth ->
            // adjustArrow(azimuth);
            adjustGambarDial(azimuth)
            adjustArrowQiblat(azimuth)
        }
        compass?.setListener(cl)
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

        val kiblat_derajat = GetFloat("kiblat_derajat")!!
        val an = RotateAnimation(
            -currentAzimuth + kiblat_derajat, -azimuth,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
            0.5f
        )
        currentAzimuth = azimuth
        an.duration = 500
        an.repeatCount = 0
        an.fillAfter = true
        binding.imgQiblaArrow.startAnimation(an)
        if (kiblat_derajat > 0) {
            binding.imgQiblaArrow.visibility = View.VISIBLE
        } else {
            binding.imgQiblaArrow.visibility = INVISIBLE
            binding.imgQiblaArrow.visibility = View.GONE
        }
    }

    @SuppressLint("MissingPermission")
    fun getBearing() {
        // Get the location manager

        val kiblat_derajat = GetFloat("kiblat_derajat")!!
        if (kiblat_derajat > 0.0001) {
            binding.textCurrentLoc.text =
                resources.getString(R.string.your_location) + " " + resources.getString(R.string.using_last_location)
            binding.textKaabaDir.text =
                resources.getString(R.string.qibla_direction) + " " + kiblat_derajat + " " + resources.getString(
                    R.string.degree_from_north
                )
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
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    SaveBoolean("permission_granted", true)
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
//                    finish()
                }
                return
            }
        }// other 'case' lines to check for other
        // permissions this app might request
    }


    fun SaveString(Judul: String, tex: String) {
        val edit = prefs.edit()
        edit.putString(Judul, tex)
        edit.apply()
    }

    fun GetString(Judul: String): String? {
        return prefs.getString(Judul, "")
    }

    fun SaveBoolean(Judul: String, bbb: Boolean?) {
        val edit = prefs.edit()
        edit.putBoolean(Judul, bbb!!)
        edit.apply()
    }

    fun GetBoolean(Judul: String): Boolean? {
        return prefs.getBoolean(Judul, false)
    }

    fun Savelong(Judul: String, bbb: Long?) {
        val edit = prefs.edit()
        edit.putLong(Judul, bbb!!)
        edit.apply()
    }

    fun Getlong(Judul: String): Long? {
        return prefs.getLong(Judul, 0)
    }

    fun SaveFloat(Judul: String, bbb: Float?) {
        val edit = prefs.edit()
        edit.putFloat(Judul, bbb!!)
        edit.apply()
    }

    fun GetFloat(Judul: String): Float? {
        return prefs.getFloat(Judul, 0f)
    }

    private fun fetchGPS() {
        var result = 0.0
        gps = GPSTracker(context)
        if (gps.canGetLocation()) {
            val latitude = gps.getLatitude()
            val longitude = gps.getLongitude()
            // \n is for new line
            binding.textCurrentLoc.text =
                resources.getString(R.string.your_location) + "\nLat: " + latitude + " Long: " + longitude
            // Toast.makeText(getApplicationContext(), "Lokasi anda: - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
            Log.e("TAG", "GPS is on")
            val lat_saya = gps.getLatitude()
            val lon_saya = gps.getLongitude()
            if (lat_saya < 0.001 && lon_saya < 0.001) {
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
                    39.826206 // ka'bah Position https://www.latlong.net/place/kaaba-mecca-saudi-arabia-12639.html
                val latitude2 =
                    Math.toRadians(21.422487) // ka'bah Position https://www.latlong.net/place/kaaba-mecca-saudi-arabia-12639.html
                val latitude1 = Math.toRadians(lat_saya)
                val longDiff = Math.toRadians(longitude2 - lon_saya)
                val y = Math.sin(longDiff) * Math.cos(latitude2)
                val x = Math.cos(latitude1) * Math.sin(latitude2) - Math.sin(latitude1) * Math.cos(
                    latitude2
                ) * Math.cos(longDiff)
                result = (Math.toDegrees(Math.atan2(y, x)) + 360) % 360
                val result2 = result.toFloat()
                SaveFloat("kiblat_derajat", result2)
                binding.textKaabaDir.text =
                    resources.getString(R.string.qibla_direction) + " " + result2 + " " + resources.getString(
                        R.string.degree_from_north
                    )
                Toast.makeText(
                    context,
                    resources.getString(R.string.qibla_direction) + " " + result2 + " " + resources.getString(
                        R.string.degree_from_north
                    ),
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
        private val TAG = CompassFragment.javaClass.simpleName
    }
}