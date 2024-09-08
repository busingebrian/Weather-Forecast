package com.businge.weatherforecast.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Constants {

    val APP_ID = "f519308c8a781d496c9e0275aa1f888e"
    val BASE_URL = "https://api.openweathermap.org/data/"
    const val METRIC_UNIT = "metric"


    // define a func called isNetworkAvailable that takes a context and returns a Boolean value
    fun isNetworkAvailable(context: Context): Boolean {
        // get an instance of connectivityManager using the context parameter
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // check if the device's API is level 23 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //get the currently active network and return false if there is none
            val network = connectivityManager.activeNetwork ?: return false

            //Get the capabilities of the acttive network and return false if it has none
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            //check the type of active network and return true if it is either cellular, wifi or ethernet
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> return true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> return true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> return true
                else -> return false
            }
        } else {
            // for API levels below 23, get the active network info and return true if it's connected
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }
    }
}