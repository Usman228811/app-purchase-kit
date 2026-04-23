package com.topedge.purchase.kit.domain.repo

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase

interface SubscriptionRepository {
    fun setBillingListener(activity: Activity,removeAdsIds: List<String>,
                           featureIds: List<String>, listener: SubscriptionListener?)
    fun querySubscriptionHistory(activity: Activity)
    fun purchaseProduct(activity: Activity,skuDetails: ProductDetails,onUserDismissedPaywall :(()->Unit) ?= null)
    fun changeSubscriptionPlan(activity: Activity,skuDetails: ProductDetails)
    fun getSelectedSubscriptionId(selectedPosition: Int): String
    fun isSubscriptionSupported(): Boolean
    fun isSubscriptionUpdateSupported(): Boolean
    fun setSubscribed(activity: Activity,purchase: Purchase)
    fun acknowledgedPurchase(activity: Activity,purchase: Purchase)
    fun viewUrl( activity: Activity, url: String)

}

interface SubscriptionListener {
    fun onQueryProductSuccess(skuList: Map<String, ProductDetails>, productList: List<ProductDetails>)
    fun subscriptionItemNotFound()
    fun onSubscriptionPurchasedFetched(purchasesList:List<String>)
}