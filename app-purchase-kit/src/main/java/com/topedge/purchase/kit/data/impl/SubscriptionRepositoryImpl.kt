package com.topedge.purchase.kit.data.impl

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.topedge.purchase.kit.core.utils.init.PurchaseKit
import com.topedge.purchase.kit.domain.repo.PlayBillingQueryResult
import com.topedge.purchase.kit.domain.repo.SubscriptionListener
import com.topedge.purchase.kit.domain.repo.SubscriptionRepository

/**
 * Subscriptions through Google Play Billing.
 */
class SubscriptionRepositoryImpl private constructor(
    private val context: Context
) : SubscriptionRepository, PurchasesUpdatedListener {

    private val purchasesList = mutableListOf<String>()
    private var onUserDismissedPaywall: (() -> Unit)? = null
    private var mActivity: Activity? = null

    companion object {
        const val TAG = "SubscriptionRepositoryImpl"

        @Volatile
        private var instance: SubscriptionRepositoryImpl? = null

        fun getInstance(
            context: Context,
        ): SubscriptionRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: SubscriptionRepositoryImpl(context).also { instance = it }
            }
        }
    }

    private var isBillingReady: Boolean = false
    private lateinit var subscriptionClient: BillingClient
    private var subscriptionListener: SubscriptionListener? = null
    private var productIds: List<String> = emptyList()
    private var subscribeProductToken = ""

    private val isBillingClientDead: Boolean
        get() = !::subscriptionClient.isInitialized

    val isBillingClientReady: Boolean
        get() = !isBillingClientDead && subscriptionClient.isReady

    override fun purchaseProduct(
        activity: Activity,
        skuDetails: ProductDetails,
        onUserDismissedPaywall: (() -> Unit)?,
    ) {
        try {
            this.onUserDismissedPaywall = onUserDismissedPaywall
            if (PurchaseKit.internetHelper.isConnected.not()) {
                context.showNoInternet(activity)
                return
            }
            if (!isBillingClientReady) {
                context.showTryAgain(activity)
                return
            }

            val billingParams = skuDetails.toBillingFlowParams()
            if (billingParams == null) {
                context.showTryAgain(activity)
                return
            }

            val billingResult = subscriptionClient.launchBillingFlow(activity, billingParams)
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                context.showTryAgain(activity)
            }
        } catch (_: IntentSender.SendIntentException) {
            context.showTryAgain(activity)
        } catch (_: Exception) {
            context.showTryAgain(activity)
        }
    }

    override fun changeSubscriptionPlan(
        activity: Activity,
        skuDetails: ProductDetails
    ) {
        try {
            if (PurchaseKit.internetHelper.isConnected.not()) {
                context.showNoInternet(activity)
                return
            }

            if (!isBillingClientReady) {
                context.showTryAgain(activity)
                return
            }

            val newOfferToken = skuDetails.firstOfferToken()
            if (newOfferToken.isNullOrBlank()) {
                context.showTryAgain(activity)
                return
            }

            queryActiveSubscriptionPurchase(activity) { activePurchase ->
                try {
                    if (activePurchase == null) {
                        context.showTryAgain(activity)
                        return@queryActiveSubscriptionPurchase
                    }

                    val oldPurchaseToken = activePurchase.purchaseToken
                    val oldProductId = activePurchase.primaryProductId()

                    if (oldPurchaseToken.isBlank() || oldProductId.isBlank()) {
                        context.showTryAgain(activity)
                        return@queryActiveSubscriptionPurchase
                    }

                    if (oldProductId == skuDetails.productId) {
                        context.showTryAgain(activity)
                        return@queryActiveSubscriptionPurchase
                    }

                    val replacementParams =
                        BillingFlowParams.ProductDetailsParams.SubscriptionProductReplacementParams
                            .newBuilder()
                            .setOldProductId(oldProductId)
                            .setReplacementMode(
                                BillingFlowParams.ProductDetailsParams
                                    .SubscriptionProductReplacementParams
                                    .ReplacementMode
                                    .WITH_TIME_PRORATION
                            )
                            .build()

                    val productDetailsParams =
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(skuDetails)
                            .setOfferToken(newOfferToken)
                            .setSubscriptionProductReplacementParams(replacementParams)
                            .build()

                    val flowParams = BillingFlowParams.newBuilder()
                        .setSubscriptionUpdateParams(
                            BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                                .setOldPurchaseToken(oldPurchaseToken)
                                .build()
                        )
                        .setProductDetailsParamsList(listOf(productDetailsParams))
                        .build()

                    val billingResult = subscriptionClient.launchBillingFlow(activity, flowParams)

                    if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        context.showTryAgain(activity)
                    }

                } catch (_: LinkageError) {
                    context.showTryAgain(activity)
                } catch (_: Exception) {
                    context.showTryAgain(activity)
                }
            }

        } catch (_: LinkageError) {
            context.showTryAgain(activity)
        } catch (_: Exception) {
            context.showTryAgain(activity)
        }
    }

    private fun buildSubscriptionProductList(productIds: List<String>): List<QueryProductDetailsParams.Product> {
        return productIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
    }

    private fun querySubscriptionProducts(activity: Activity) {
        if (isBillingClientDead || productIds.isEmpty() || !isSubscriptionSupported()) {
            return
        }

        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(buildSubscriptionProductList(productIds))
            .build()
        subscriptionClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, details ->
            activity.runOnUiThread {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                    details.productDetailsList.isNotEmpty()
                ) {
                    subscriptionListener?.onQueryProductSuccess(
                        PlayBillingQueryResult(
                            skuList = details.productDetailsList.toProductDetailsMap(),
                            productList = details.productDetailsList
                        )
                    )
                } else {
                    subscriptionListener?.subscriptionItemNotFound()
                }
            }
        }
    }

    private fun resetAllPurchases() {
        subscribeProductToken = ""
        purchasesList.clear()
    }

    private fun getSku(skuList: MutableList<String>): String = skuList.firstOrNull().orEmpty()

    override fun querySubscriptionHistory(activity: Activity) {
        try {
            purchasesList.clear()
            if (isBillingClientDead) {
                return
            }

            if (subscriptionClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS).responseCode !=
                BillingClient.BillingResponseCode.OK
            ) {
                resetAllPurchases()
                activity.runOnUiThread { subscriptionListener.dispatchPurchases(emptyList()) }
                return
            }

            subscriptionClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
                object : PurchasesResponseListener {
                    override fun onQueryPurchasesResponse(
                        billingResult: BillingResult,
                        purchases: MutableList<Purchase>
                    ) {
                        var purchasesFound = false
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases.isNotEmpty()) {
                            for (purchase in purchases) {
                                if (processSubscriptionPurchase(activity, purchase)) {
                                    purchasesFound = true
                                }
                            }
                        }

                        if (!purchasesFound) {
                            resetAllPurchases()
                            activity.runOnUiThread { subscriptionListener.dispatchPurchases(emptyList()) }
                        }
                    }
                }
            )
        } catch (_: LinkageError) {
            activity.runOnUiThread { subscriptionListener.dispatchPurchases(emptyList()) }
        } catch (_: Exception) {
            activity.runOnUiThread { subscriptionListener.dispatchPurchases(emptyList()) }
        }
    }

    private fun processSubscriptionPurchase(activity: Activity, purchase: Purchase): Boolean {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            return false
        }
        if (!checkSubscriptionsId(getSku(purchase.products))) {
            return false
        }
        if (purchase.isAcknowledged) {
            notifyPurchase(activity, purchase)
        } else {
            acknowledgedPurchase(activity, purchase)
        }
        return true
    }

    private fun notifyPurchase(activity: Activity, purchase: Purchase) {
        setSubscribed(activity, purchase)
        val updatedPurchases = purchasesList.addDistinct(purchase.primaryProductId())
        activity.runOnUiThread {
            subscriptionListener.dispatchPurchases(updatedPurchases)
        }
    }

    override fun setSubscribed(activity: Activity, purchase: Purchase) {
        subscribeProductToken = purchase.purchaseToken
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, list: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                mActivity?.let { activity ->
                    if (!list.isNullOrEmpty()) {
                        for (purchase in list) {
                            processSubscriptionPurchase(activity, purchase)
                        }
                    }
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "Subscription: User dismissed the paywall")
                onUserDismissedPaywall?.invoke()
            }
        }
    }

    override fun getSelectedSubscriptionId(selectedPosition: Int): String = ""

    override fun isSubscriptionSupported(): Boolean {
        if (!isBillingClientReady) {
            return false
        }
        return subscriptionClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS).responseCode ==
                BillingClient.BillingResponseCode.OK
    }

    override fun isSubscriptionUpdateSupported(): Boolean {
        if (!isBillingClientReady) {
            return false
        }
        return subscriptionClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS_UPDATE).responseCode ==
                BillingClient.BillingResponseCode.OK
    }

    private fun checkSubscriptionsId(sku: String?): Boolean {
        return sku != null && productIds.isNotEmpty() && productIds.contains(sku)
    }

    override fun acknowledgedPurchase(activity: Activity, purchase: Purchase) {
        if (isBillingClientDead) {
            return
        }
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        subscriptionClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                notifyPurchase(activity, purchase)
            }
        }
    }

    override fun setBillingListener(
        activity: Activity,
        removeAdsIds: List<String>,
        featureIds: List<String>,
        listener: SubscriptionListener?
    ) {
        mActivity = activity
        subscriptionListener = listener
        productIds = (removeAdsIds + featureIds).distinct()
        if (isBillingReady) {
            querySubscriptionProducts(activity)
        } else {
            setupConnection(activity)
        }
    }

    private fun setupConnection(activity: Activity) {
        try {
            if (!::subscriptionClient.isInitialized) {
                subscriptionClient = BillingClient
                    .newBuilder(context)
                    .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
                    )
                    .setListener(this)
                    .build()
            }
            if (isBillingReady || subscriptionClient.isReady) {
                return
            }
            subscriptionClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        isBillingReady = true
                        querySubscriptionProducts(activity)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    isBillingReady = false
                }
            })
        } catch (_: Exception) {
        }
    }

    override fun viewUrl(activity: Activity, url: String) {
        activity.openBrowsableUrl(url)
    }

    private fun ProductDetails.firstOfferToken(): String? {
        return subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken
    }

    private fun queryActiveSubscriptionPurchase(
        activity: Activity,
        onResult: (Purchase?) -> Unit
    ) {
        if (!isBillingClientReady) {
            activity.runOnUiThread { onResult(null) }
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        subscriptionClient.queryPurchasesAsync(params) { billingResult, purchases ->
            val activePurchase =
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases.firstOrNull { purchase ->
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                                purchase.products.any { productId -> checkSubscriptionsId(productId) }
                    }
                } else {
                    null
                }

            activity.runOnUiThread {
                onResult(activePurchase)
            }
        }
    }
}
