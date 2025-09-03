package com.topedge.purchase.kit.core.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class InternetHelper private constructor(context: Context) {


    companion object {
        @Volatile
        private var instance: InternetHelper? = null


        internal fun getInstance(
            context: Context,
        ): InternetHelper {
            return instance ?: synchronized(this) {
                instance ?: InternetHelper(
                    context.applicationContext,
                ).also {
                    instance = it
                }
            }
        }
    }

    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    val isConnected: Boolean
        get() {
            try {
                val network = connectivityManager.activeNetwork
                if (network != null) {
                    return connectivityManager.getNetworkCapabilities(network)
                        .isNetworkCapabilitiesValid()
                }
            } catch (_: Exception) {
            }
            return false
        }

    private fun NetworkCapabilities?.isNetworkCapabilitiesValid(): Boolean = when {
        this == null -> false
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                (hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
                        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) -> true

        else -> false
    }
}