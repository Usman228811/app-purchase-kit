package com.topedge.purchase.kit.data.impl

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.purchaseWith
import com.topedge.purchase.kit.core.utils.init.PurchaseKit
import com.topedge.purchase.kit.domain.repo.RevenueCatBillingQueryResult
import com.topedge.purchase.kit.domain.repo.SubscriptionListener
import com.topedge.purchase.kit.domain.repo.SubscriptionRepository

/**
 * Subscriptions through RevenueCat.
 * Purchases are reported as active entitlement identifiers.
 */
class RCSubscriptionRepositoryImpl private constructor(
    private val context: Context
) : SubscriptionRepository {

    private val purchasesList = mutableListOf<String>()
    private var onUserDismissedPaywall: (() -> Unit)? = null

    companion object {
        @Volatile
        private var instance: RCSubscriptionRepositoryImpl? = null

        fun getInstance(
            context: Context,
        ): RCSubscriptionRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: RCSubscriptionRepositoryImpl(context).also { instance = it }
            }
        }
    }

    private var subscriptionListener: SubscriptionListener? = null
    private var productIds: List<String> = emptyList()
    private var purchasesHistoryUrl: String = ""
    private var packageMap: Map<String, Package> = emptyMap()

    override fun purchaseProduct(
        activity: Activity,
        skuDetails: Package,
        onUserDismissedPaywall: (() -> Unit)?,
    ) {
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

            val params = PurchaseParams.Builder(activity, skuDetails).build()
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

    override fun purchaseProduct(
        activity: Activity,
        skuDetails: ProductDetails,
        onUserDismissedPaywall: (() -> Unit)?,
    ) = Unit

    override fun changeSubscriptionPlan(activity: Activity, skuDetails: ProductDetails) = Unit
    override fun changeSubscriptionPlan(activity: Activity, skuDetails: Package) = Unit

    override fun querySubscriptionHistory(activity: Activity) {
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

    override fun getSelectedSubscriptionId(selectedPosition: Int): String = ""

    override fun setSubscribed(activity: Activity, purchase: Purchase) = Unit

    override fun isSubscriptionSupported(): Boolean = true

    override fun isSubscriptionUpdateSupported(): Boolean = true

    override fun acknowledgedPurchase(activity: Activity, purchase: Purchase) = Unit

    override fun setBillingListener(
        activity: Activity,
        removeAdsIds: List<String>,
        featureIds: List<String>,
        listener: SubscriptionListener?
    ) {
        subscriptionListener = listener
        productIds = (removeAdsIds + featureIds).distinct()
        purchasesHistoryUrl =
            "https://play.google.com/store/account/subscriptions?package=${activity.packageName}"
        try {
            queryPackageDetails(productIds)
        } catch (_: Exception) {
            activity.runOnUiThread {
                subscriptionListener?.subscriptionItemNotFound()
            }
        }
    }

    private fun queryPackageDetails(packageIds: List<String>) {
        Purchases.sharedInstance.getOfferingsWith(
            onSuccess = { offerings ->
                val packages = offerings[PurchaseKit.getRevenueCatOfferingKey()]
                    ?.availablePackages
                    ?.filter { it.identifier in packageIds }
                    .orEmpty()
                packageMap = packages.toPackageMap()

                subscriptionListener?.onQueryProductSuccess(
                    RevenueCatBillingQueryResult(
                        skuList = packageMap,
                        productList = packages
                    )
                )
            },
            onError = { _ ->
                subscriptionListener?.subscriptionItemNotFound()
            }
        )
    }

    override fun viewUrl(activity: Activity, url: String) {
        activity.openBrowsableUrl(url.ifEmpty { purchasesHistoryUrl })
    }
}
