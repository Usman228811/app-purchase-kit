package com.topedge.purchase.kit.domain.repo

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase

interface SubscriptionRepository {
    fun setBillingListener(activity: Activity, listener: SubscriptionListener?)
    fun querySubscriptionProducts(activity: Activity,productIds: List<String>)
    fun querySubscriptionHistory(activity: Activity)
    fun purchaseProduct(activity: Activity,skuDetails: ProductDetails)
    fun changeSubscriptionPlan(activity: Activity,skuDetails: ProductDetails)
    fun getSelectedSubscriptionId(selectedPosition: Int): String
    fun isSubscriptionSupported(): Boolean
    fun isSubscriptionUpdateSupported(): Boolean
    fun setSubscribed(activity: Activity,purchase: Purchase)
    fun acknowledgedPurchase(activity: Activity,purchase: Purchase)
    fun viewUrl( activity: Activity, url: String)


}

interface SubscriptionListener {
    fun onBillingInitialized()
    fun onQueryProductSuccess(skuList: Map<String, ProductDetails>)
    fun subscriptionItemNotFound()
    fun checkPurchaseStatus(purchase: Purchase)
    fun updatePref( subscribedId:String)
    fun onSubscriptionPurchasedFetched()
}