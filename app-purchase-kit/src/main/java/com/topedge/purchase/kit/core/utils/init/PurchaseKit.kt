package com.topedge.purchase.kit.core.utils.init

import android.app.Application
import android.content.Context
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.topedge.purchase.kit.core.utils.InternetHelper
import com.topedge.purchase.kit.core.utils.PurchasePref
import com.topedge.purchase.kit.core.utils.purchase.PurchaseKitPremiumHelper
import com.topedge.purchase.kit.data.impl.RevenueCatBuilder

object PurchaseKit {

    private val logLevel: LogLevel = LogLevel.DEBUG

    private lateinit var mContext: Application
    private var revenueCatBuilder: RevenueCatBuilder? = null

    internal fun getRevenueCatKey(): String {
        return revenueCatBuilder?.revenueCatKey ?: ""
    }

    internal fun getRevenueCatOfferingKey(): String {
        return revenueCatBuilder?.offeringKey ?: ""
    }

    val preference: PurchasePref by lazy {
        PurchasePref.getInstance(mContext)
    }


    val  internetHelper: InternetHelper by lazy {
        InternetHelper.getInstance(mContext)
    }


    val premiumHelper: PurchaseKitPremiumHelper by lazy {
        PurchaseKitPremiumHelper.getInstance(mContext)
    }


    private fun configureRevenueCat(context: Context) {
        Purchases.logLevel = logLevel
        val configuration = PurchasesConfiguration.Builder(
            context,
            getRevenueCatKey()
        ).build()
        Purchases.configure(configuration)
    }


    /**
     * @param revenueCatBuilder pass it to route all billing through RevenueCat,
     * leave it null to use Google Play Billing directly.
     */
    fun init(
        context: Application,
        revenueCatBuilder: RevenueCatBuilder? = null,
    ) {
        mContext = context
        this.revenueCatBuilder = revenueCatBuilder

        if (getRevenueCatKey().isNotEmpty()) {
            configureRevenueCat(context.applicationContext)
        }

//        preference = PurchasePref.getInstance(context)
//        internetHelper = InternetHelper.getInstance(context)
//        oneTimePurchaseHelper = AdKitPurchaseHelper.getInstance(context)
//        subscriptionHelper = AdKitSubscriptionHelper.getInstance(context)

    }
}
