package com.topedge.purchase.kit.core.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.core.content.edit

class PurchasePref private constructor(context: Context) {

    private val pref = context.getSharedPreferences(
        "_PurchasePref", MODE_PRIVATE
    )


    var isLifeTimePurchased: Boolean
        get() = pref.getBoolean("isLifeTimePurchased", false)
        set(value) = pref.edit { putBoolean("isLifeTimePurchased", value) }

    var isAppSubscribed: Boolean
        get() = pref.getBoolean("isAppSubscribed", false)
        set(value) = pref.edit { putBoolean("isAppSubscribed", value) }

    val isAppPurchased: Boolean
        get() = isLifeTimePurchased || isAppSubscribed

    companion object {
        @Volatile
        private var instance: PurchasePref? = null

        internal fun getInstance(context: Context): PurchasePref {
            return instance ?: synchronized(this) {
                instance ?: PurchasePref(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
