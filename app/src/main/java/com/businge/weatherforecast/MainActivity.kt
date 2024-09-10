package com.businge.weatherforecast

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.businge.weatherforecast.models.WeatherResponse
import com.businge.weatherforecast.utils.Constants
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val REQUEST_LOCATION_CODE = 111
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var dateTextView: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        dateTextView = findViewById(R.id.text_view_updated_at)
        val currentDate = getCurrentDate()
        dateTextView.text = currentDate

        mFusedLocationClient =

            LocationServices.getFusedLocationProviderClient(this) // get lat and long coordinates

        if (!isLocationEnabled()) {
            Toast.makeText(this@MainActivity, "The Location is not turned on", Toast.LENGTH_SHORT)
                .show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            requestPermissions()
        }
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("dd-mm-yyy, HH:mm a", Locale.getDefault())
        return sdf.format(Date())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_CODE && grantResults.isNotEmpty()) {
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            requestLocationData()
        } else {
            Toast.makeText(this, "Permission Not Granted", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()

        mFusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                getLocationWeatherDetails(
                    locationResult.lastLocation?.latitude!!,
                    locationResult.lastLocation?.longitude!!
                )
            }
        }, Looper.myLooper())
    }

    private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {
        if (Constants.isNetworkAvailable(this)) {
            val retrofit = Retrofit.Builder().baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build()

            val serviceAPI = retrofit.create(WeatherServiceAPI::class.java)
            val call = serviceAPI.getWeatherDetails(
                latitude,
                longitude,
                Constants.APP_ID,
                Constants.METRIC_UNIT
            )

            call.enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if (response.isSuccessful) {
                        val weather = response.body()
                        for (i in weather!!.weather.indices) {
                            findViewById<TextView>(R.id.text_view_sunset).text = convertTime(weather.sys.sunset.toLong())
                            findViewById<TextView>(R.id.text_view_sunrise).text = convertTime(weather.sys.sunrise.toLong())
                            findViewById<TextView>(R.id.text_view_status).text = weather.weather[i].description
                            findViewById<TextView>(R.id.text_view_address).text = weather.name
                            findViewById<TextView>(R.id.text_view_temp_max).text =
                                buildString {
                                    append(weather.main.temp_max.toString())
                                    append(" ")
                                    append(getString(R.string.max))
                                }
                            findViewById<TextView>(R.id.text_view_temp_min).text =
                                buildString {
                                    append(weather.main.temp_min.toString())
                                    append(" ")
                                    append(getString(R.string.min))
                                }
                            findViewById<TextView>(R.id.text_view_temp).text =
                                buildString {
                                    append(weather.main.temp.toString())
                                    append("°C")
                                }
                            findViewById<TextView>(R.id.text_view_humidity).text = weather.main.humidity.toString()
                            findViewById<TextView>(R.id.text_view_pressure).text = weather.main.pressure.toString()
                            findViewById<TextView>(R.id.text_view_wind).text = weather.wind.speed.toString()
                        }
                        Log.d("WEATHER", weather.toString())

                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "something went wrong",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, error: Throwable) {
                    Toast.makeText(this@MainActivity, "Sorry we have Internet Disruption at the moment", Toast.LENGTH_SHORT).show()
                }

            })
        } else {
            Toast.makeText(this, "There is no internet connection", Toast.LENGTH_SHORT).show()
        }
    }
     /* formatting time to 24hrs */
     private fun convertTime(time: Long): String {
        val date = Date(time * 1000L)
        val timeFormatted = SimpleDateFormat("HH:mm", Locale.UK)

        timeFormatted.timeZone = TimeZone.getDefault()
        return timeFormatted.format(date)
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun requestPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            showRequestDialog()
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            requestPermissions()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ),
                REQUEST_LOCATION_CODE
            )
        }
    }

    private fun showRequestDialog() {
        AlertDialog.Builder(this).setPositiveButton("GO TO SETTINGS") { _, _ ->
            try { // directs the user to the app's setting
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }
        }.setNegativeButton("CLOSE") { dialog, _ ->
            dialog.cancel()
        }.setTitle("Location Permission Needed")
            .setMessage("This permission is needed for accessing the location. It can be enabled under Application settings. ")
            .show()
    }
}