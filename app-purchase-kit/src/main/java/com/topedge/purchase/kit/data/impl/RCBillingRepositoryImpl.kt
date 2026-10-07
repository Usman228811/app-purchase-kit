package com.topedge.purchase.kit.data.impl

import android.app.Activity
import android.content.Context
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.purchaseWith
import com.topedge.purchase.kit.core.utils.init.PurchaseKit
import com.topedge.purchase.kit.domain.repo.BillingRepository
import com.topedge.purchase.kit.domain.repo.PurchasePriceModel
import com.topedge.purchase.kit.domain.repo.RevenueCatBillingQueryResult
import com.topedge.purchase.kit.domain.repo.SubscriptionListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One-time (lifetime) purchases through RevenueCat.
 * Purchases are reported as active entitlement identifiers.
 */
class RCBillingRepositoryImpl private constructor(
    private val context: Context,
) : BillingRepository {

    private var onUserDismissedPaywall: (() -> Unit)? = null
    private val purchasesList = mutableListOf<String>()

    companion object {
        @Volatile
        private var instance: RCBillingRepositoryImpl? = null

        fun getInstance(
            context: Context,
        ): RCBillingRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: RCBillingRepositoryImpl(context).also { instance = it }
            }
        }
    }

    private val productPriceFlow = MutableStateFlow(PurchasePriceModel())

    private var skuMap: Map<String, Package> = emptyMap()
    private var productIds: List<String> = emptyList()
    private var subscriptionListener: SubscriptionListener? = null

    override fun initBilling(
        removeAdsIds: List<String>,
        featureIds: List<String>,
        subscriptionListener: SubscriptionListener,
    ) {
        this.subscriptionListener = subscriptionListener
        productIds = (removeAdsIds + featureIds).distinct()
        try {
            queryPackageDetails(productIds)
        } catch (_: Exception) {
            subscriptionListener.subscriptionItemNotFound()
        }
    }

    private fun queryPackageDetails(packageIds: List<String>) {
        Purchases.sharedInstance.getOfferingsWith(
            onSuccess = { offerings ->
                val packages = offerings[PurchaseKit.getRevenueCatOfferingKey()]
                    ?.availablePackages
                    ?.filter { it.identifier in packageIds }
                    .orEmpty()

                skuMap = packages.toPackageMap()
                subscriptionListener?.onQueryProductSuccess(
                    RevenueCatBillingQueryResult(
                        skuList = skuMap,
                        productList = packages
                    )
                )
            },
            onError = { _ ->
                subscriptionListener?.subscriptionItemNotFound()
            }
        )
    }

    override fun productPriceFlow(): StateFlow<PurchasePriceModel> = productPriceFlow.asStateFlow()

    override fun purchaseProduct(
        activity: Activity?,
        productId: String,
        onUserDismissedPaywall: (() -> Unit)?,
    ) {
        if (activity == null) return
        try {
            this.onUserDismissedPaywall = onUserDismissedPaywall
            if (PurchaseKit.internetHelper.isConnected.not()) {
                context.showNoInternet(activity)
                return
            }
            if (!activity.canLaunchBillingFlow()) {
                context.showTryAgain(activity)
                return
            }

            val details = skuMap[productId]
            if (details == null) {
                context.showTryAgain(activity)
                return
            }

            val params = PurchaseParams.Builder(activity, details).build()
            Purchases.sharedInstance.purchaseWith(
                purchaseParams = params,
                onSuccess = { _, info ->
                    val updatedPurchases = purchasesList.replaceWithDistinct(info.activeEntitlementIds())
                    if (updatedPurchases.isNotEmpty()) {
                        activity.runOnUiThread {
                            subscriptionListener.dispatchPurchases(updatedPurchases)
                        }
                    } else {
                        context.showTryAgain(activity)
                    }
                },
                onError = { _, userCancelled ->
                    if (userCancelled) {
                        onUserDismissedPaywall?.invoke()
                    } else {
                        context.showTryAgain(activity)
                    }
                }
            )
        } catch (_: Exception) {
            context.showTryAgain(activity)
        }
    }

    override fun checkProductPurchaseHistory() {
        Purchases.sharedInstance.getCustomerInfoWith(
            onSuccess = { info ->
                subscriptionListener.dispatchPurchases(
                    purchasesList.replaceWithDistinct(info.activeEntitlementIds())
                )
            },
            onError = { _ ->
                purchasesList.clear()
                subscriptionListener.dispatchPurchases(emptyList())
            }
        )
    }
}
