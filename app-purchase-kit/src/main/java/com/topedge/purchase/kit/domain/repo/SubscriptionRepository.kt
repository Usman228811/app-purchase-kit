package com.topedge.purchase.kit.domain.repo

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.Package

sealed interface BillingQueryResult

data class PlayBillingQueryResult(
    val skuList: Map<String, ProductDetails>,
    val productList: List<ProductDetails>
) : BillingQueryResult

data class RevenueCatBillingQueryResult(
    val skuList: Map<String, Package>,
    val productList: List<Package>
) : BillingQueryResult

interface SubscriptionRepository {
    fun setBillingListener(activity: Activity,removeAdsIds: List<String>,
                           featureIds: List<String>, listener: SubscriptionListener?)
    fun querySubscriptionHistory(activity: Activity)
    fun purchaseProduct(activity: Activity,skuDetails: ProductDetails,onUserDismissedPaywall :(()->Unit) ?= null)
    fun purchaseProduct(activity: Activity,skuDetails: Package,onUserDismissedPaywall :(()->Unit) ?= null)
    fun changeSubscriptionPlan(activity: Activity,skuDetails: ProductDetails)
    fun changeSubscriptionPlan(activity: Activity,skuDetails: Package)
    fun getSelectedSubscriptionId(selectedPosition: Int): String
    fun isSubscriptionSupported(): Boolean
    fun isSubscriptionUpdateSupported(): Boolean
    fun setSubscribed(activity: Activity,purchase: Purchase)
    fun acknowledgedPurchase(activity: Activity,purchase: Purchase)
    fun viewUrl( activity: Activity, url: String)

}

interface SubscriptionListener {
    fun onQueryProductSuccess(result: BillingQueryResult)
    fun subscriptionItemNotFound()
    fun onSubscriptionPurchasedFetched(purchasesList:List<String>)
}
