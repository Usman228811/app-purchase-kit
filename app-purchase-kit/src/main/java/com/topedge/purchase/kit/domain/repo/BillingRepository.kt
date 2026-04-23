package com.topedge.purchase.kit.domain.repo

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class PurchasePriceModel(val price: String = "")

// domain/repository/BillingRepository.kt
interface BillingRepository {
    fun productPriceFlow(): StateFlow<PurchasePriceModel>

    fun initBilling(
        removeAdsIds: List<String>,
        featureIds: List<String>, subscriptionListener: SubscriptionListener
    )

    fun checkProductPurchaseHistory()
    fun purchaseProduct(
        activity: Activity?,
        productId: String,
        onUserDismissedPaywall: (() -> Unit)? = null
    )
}