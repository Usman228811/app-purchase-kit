package com.topedge.purchase.kit.data.impl


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import androidx.core.net.toUri
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
import com.topedge.purchase.kit.domain.repo.SubscriptionListener
import com.topedge.purchase.kit.domain.repo.SubscriptionRepository


class SubscriptionRepositoryImpl private constructor(
    mContext: Context
) : SubscriptionRepository, PurchasesUpdatedListener {

    private val context = mContext.applicationContext

    companion object {
        @Volatile
        private var instance: SubscriptionRepositoryImpl? = null


        fun getInstance(
            context: Context,
        ): SubscriptionRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: SubscriptionRepositoryImpl(
                    context
                ).also { instance = it }
            }
        }
    }

    private var isBillingReady: Boolean = false
    private lateinit var subscriptionClient: BillingClient
    private var subscriptionListener: SubscriptionListener? = null
    private var mActivity: Activity? = null
    private var productIds: List<String>? = null
    private var subscribeProductToken = ""

    private val isBillingClientDead: Boolean
        get() = !::subscriptionClient.isInitialized
    val isBillingClientReady: Boolean
        get() = if (isBillingClientDead) {
            false
        } else subscriptionClient.isReady

    override fun purchaseProduct(skuDetails: ProductDetails) {
        try {
            if (isBillingClientDead) {
                return
            }
            if (mActivity == null) {
                return
            }
            val offerToken = skuDetails.subscriptionOfferDetails?.get(0)!!.offerToken
            subscriptionClient.launchBillingFlow(
                mActivity!!,
                BillingFlowParams.newBuilder().setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(skuDetails)
                            .setOfferToken(offerToken)
                            .build()
                    )
                ).build()
            ).responseCode
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun changeSubscriptionPlan(skuDetails: ProductDetails) {
        try {
            if (isBillingClientDead) {
                return
            }
            if (mActivity == null) {
                return
            }
            val offerToken = skuDetails.subscriptionOfferDetails?.get(0)!!.offerToken
            val list: MutableList<BillingFlowParams.ProductDetailsParams> = ArrayList()
            list.add(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(skuDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
            val flowParams = BillingFlowParams.newBuilder()
                .setSubscriptionUpdateParams(
                    BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                        .setOldPurchaseToken(subscribeProductToken)
                        .setSubscriptionReplacementMode(BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION)
                        .build()
                )
                .setProductDetailsParamsList(list)
                .build()
        } catch (ignored: Exception) {
        }
    }

    fun buildSubscriptionProductList(productIds: List<String>): List<QueryProductDetailsParams.Product> {
        return productIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
    }

    override fun querySubscriptionProducts(productIds: List<String>) {
        this.productIds = productIds
        if (isBillingClientDead) {
            return
        }
        if (isSubscriptionSupported()) {

            val list = buildSubscriptionProductList(productIds)

            val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(list)
                .build()
            if (isBillingClientDead) {
                return
            }
            subscriptionClient.queryProductDetailsAsync(queryProductDetailsParams) { p0, details ->
                if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
                    val p1 = details.productDetailsList
                    mActivity?.runOnUiThread {
                        if (p1.isNotEmpty()) {
                            subscriptionListener?.onQueryProductSuccess(getSkuFromList(p1))
                        } else {
                            subscriptionListener?.subscriptionItemNotFound()
                        }
                    }
                } else {
                    mActivity?.runOnUiThread {
                        subscriptionListener?.subscriptionItemNotFound()
                    }
                }
            }
        }
    }

    private fun getSkuFromList(list: MutableList<ProductDetails>): Map<String, ProductDetails> {
        val skuDetailList: MutableMap<String, ProductDetails> = HashMap()
        list.forEach {
            it.productId.let { sku ->
                if (!TextUtils.isEmpty(sku)) {
                    skuDetailList[sku] = it
                }
            }
        }
        return skuDetailList
    }


    private fun resetAllPurchases() {
        subscribeProductToken = ""
        mActivity?.runOnUiThread {
            subscriptionListener?.updatePref("")
        }
    }

    private fun getSku(skuList: MutableList<String>): String {
        return if (skuList.size > 0) {
            skuList[0]
        } else ""
    }

    override fun querySubscriptionHistory() {
        if (isBillingClientDead) {
            return
        }
        subscriptionClient.let {
            if (it.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS).responseCode == BillingClient.BillingResponseCode.OK) {
                it.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(), object : PurchasesResponseListener {
                        override fun onQueryPurchasesResponse(
                            p0: BillingResult, p1: MutableList<Purchase>
                        ) {
                            if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
                                if (p1.isNotEmpty()) {
                                    for (purchase in p1) {
                                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && checkSubscriptionsId(
                                                getSku(purchase.products)
                                            )
                                        ) {
                                            if (purchase.isAcknowledged) {
                                                setSubscribed(purchase)
                                                mActivity?.runOnUiThread {
                                                    subscriptionListener?.onSubscriptionPurchasedFetched()
                                                }
                                            } else {
                                                acknowledgedPurchase(purchase)
                                            }
                                            return
                                        }
                                    }
                                }
                            }
                            resetAllPurchases()
                            mActivity?.runOnUiThread {
                                subscriptionListener?.onSubscriptionPurchasedFetched()
                            }
                        }

                    })
            } else {
                resetAllPurchases()
            }
        }
    }

    override fun setSubscribed(purchase: Purchase) {
        subscribeProductToken = purchase.purchaseToken
        mActivity?.runOnUiThread {
            subscriptionListener?.updatePref(getSku(purchase.products))
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, list: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            if (!list.isNullOrEmpty()) {
                for (purchase in list) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        checkSubscriptionsId(getSku(purchase.products))
                    ) {
                        mActivity?.runOnUiThread {
                            subscriptionListener?.checkPurchaseStatus(purchase)
                        }
                        break
                    }
                }
            }
        }
    }

    override fun getSelectedSubscriptionId(selectedPosition: Int): String {
//        return when (selectedPosition) {
//            0 -> {
//                WEEKLY_SUBSCRIPTION_ID_NEW
//            }
//
//            1 -> {
//                MONTHLY_SUBSCRIPTION_ID_NEW
//            }
//
//            2 -> {
//                YEARLY_SUBSCRIPTION_ID_NEW
//            }
//
//            else -> MONTHLY_SUBSCRIPTION_ID_NEW
//        }
        return ""
    }

    override fun isSubscriptionSupported(): Boolean {
        if (isBillingClientDead) {
            return false
        }
        return if (!subscriptionClient.isReady) {
            false
        } else subscriptionClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS).responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun isSubscriptionUpdateSupported(): Boolean {
        if (isBillingClientDead) {
            return false
        }
        return if (!subscriptionClient.isReady) {
            false
        } else subscriptionClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS_UPDATE).responseCode == BillingClient.BillingResponseCode.OK
    }

    private fun checkSubscriptionsId(sku: String?): Boolean {
        productIds?.let { productIds ->
            return sku != null && productIds.isNotEmpty() && productIds.contains(sku)
        }
        return false
    }

    override fun acknowledgedPurchase(purchase: Purchase) {
        if (isBillingClientDead) {
            return
        }
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        subscriptionClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult: BillingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                setSubscribed(purchase)
                mActivity?.runOnUiThread {
                    subscriptionListener?.onSubscriptionPurchasedFetched()
                }
            }
        }
    }

    override fun setBillingListener(
        activity: Activity,
        listener: SubscriptionListener?
    ) {
        this.mActivity = activity
        this.subscriptionListener = listener
        if (isBillingReady) {
            subscriptionListener?.onBillingInitialized()
        } else {
            setupConnection()
        }
    }


    init {
        setupConnection()
    }

    private fun setupConnection() {
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
            if (isBillingReady) {
                return
            }
            subscriptionClient.let {
                if (!it.isReady) {
                    it.startConnection(object : BillingClientStateListener {
                        override fun onBillingSetupFinished(billingResult: BillingResult) {
                            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                isBillingReady = true
                                mActivity?.runOnUiThread {
                                    subscriptionListener?.onBillingInitialized()
                                }
                            }
                        }

                        override fun onBillingServiceDisconnected() {
                            isBillingReady = false
                        }
                    })
                }
            }
        } catch (ignored: Exception) {
        }
    }

    override fun viewUrl(activity: Activity, url: String) {
        try {
            Intent().apply {
                action = Intent.ACTION_VIEW
                data = url.toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                addCategory(Intent.CATEGORY_BROWSABLE)
            }.also {
                if (it.resolveActivity(activity.packageManager) != null) {
                    activity.startActivity(it)
                }
            }
        } catch (ignored: Exception) {
        }
    }
}