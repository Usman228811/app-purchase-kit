package com.topedge.purchase.kit.data.impl
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.text.TextUtils
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.topedge.purchase.kit.R
import com.topedge.purchase.kit.core.utils.init.PurchaseKit
import com.topedge.purchase.kit.core.utils.showToast
import com.topedge.purchase.kit.domain.repo.BillingRepository
import com.topedge.purchase.kit.domain.repo.PurchasePriceModel
import com.topedge.purchase.kit.domain.repo.SubscriptionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BillingRepositoryImpl private constructor(
    mContext: Context,
) : BillingRepository {

    private val context = mContext

    private var onUserDismissedPaywall: (() -> Unit)? = null

    private val purchasesList = mutableListOf<String>()

    companion object {

        const val TAG = "BillingRepositoryImpl"

        @Volatile
        private var instance: BillingRepositoryImpl? = null


        fun getInstance(
            context: Context,
        ): BillingRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: BillingRepositoryImpl(
                    context,
                ).also { instance = it }
            }
        }
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _productPriceFlow = MutableStateFlow(PurchasePriceModel())


    private var skuMap: Map<String, ProductDetails> = emptyMap()
    private lateinit var billingClient: BillingClient
    private var productIds: List<String>? = null
    private var removeAdsIds: List<String>? = null
    private var featureIds: List<String>? = null

    private var isBillingReady: Boolean = false
    private var subscriptionListener: SubscriptionListener? = null


    override fun initBilling(
        removeAdsIds: List<String>,
        featureIds: List<String>, subscriptionListener: SubscriptionListener
    ) {
        this.subscriptionListener = subscriptionListener
        this.removeAdsIds = removeAdsIds
        this.featureIds = featureIds
        this.productIds = (removeAdsIds + featureIds).distinct()
        coroutineScope.launch {
            if (isBillingReady) {
                queryProductSkuForPurchase()
            } else {
                setupBillingClient()
            }
        }
    }

    override fun productPriceFlow(): StateFlow<PurchasePriceModel> {
        return _productPriceFlow.asStateFlow()
    }


    private val isBillingClientInitialized: Boolean
        get() = ::billingClient.isInitialized


    private fun setupBillingClient() {
        if (!isBillingClientInitialized) {
            billingClient = BillingClient.newBuilder(context)
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
                )
                .setListener { result, purchases ->
                    when (result.responseCode) {
                        BillingClient.BillingResponseCode.OK -> {
                            isProductPurchased(purchases)
                        }

                        BillingClient.BillingResponseCode.USER_CANCELED -> {
                            onUserDismissedPaywall?.invoke()
                            Log.d(TAG, "One-Time-Purchase: User dismissed the paywall")
                        }

                    }
                }
                .build()
        }
        if (isBillingReady) {
            return
        }

        if (!billingClient.isReady) {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingServiceDisconnected() {
                    isBillingReady = false
//                    "Service Disconnected".logIt(BILLING_TAG)
                }

                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        isBillingReady = true
                        queryProductSkuForPurchase()
                    } else {
//                        "Setup Failed: ${result.responseCode}".logIt(BILLING_TAG)
                    }
                }
            })
        }
    }


    fun buildProductList(productIds: List<String>): List<QueryProductDetailsParams.Product> {
        return productIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
    }

    private fun queryProductSkuForPurchase() {

        if (!isBillingClientReady()) return

        productIds?.let { productIds->
            val list = buildProductList(productIds)
            val queryParams = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    list
                )
                .build()

            billingClient.queryProductDetailsAsync(queryParams) { result, queryProductDetailsResult ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    val productList = queryProductDetailsResult.productDetailsList
                    if (productList.isNotEmpty()) {
                        skuMap = getSkuFromList(productList)
                        if (productList.isNotEmpty()) {
                            subscriptionListener?.onQueryProductSuccess(
                                skuMap,
                                productList
                            )
                        } else {
                            subscriptionListener?.subscriptionItemNotFound()
                        }
                    }
                } else {
//                "Product Query Failed: ${result.responseCode}".logIt(BILLING_TAG)
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

    override fun purchaseProduct(
        activity: Activity?,
        productId: String, onUserDismissedPaywall: (() -> Unit)?
    ) {
        try {
            this.onUserDismissedPaywall = onUserDismissedPaywall
            if (activity == null) return
            if (!PurchaseKit.internetHelper.isConnected) {
                context.showToast(activity.getString(R.string.no_internet))
                return
            }

            if (!isBillingClientReady()) {
                context.showToast(activity.getString(R.string.try_again))
                return
            }

            skuMap[productId]?.let { details ->

                val billingParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(details)
                                .build()
                        )
                    )
                    .build()
                val billingResult = billingClient.launchBillingFlow(activity, billingParams)
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    context.showToast(activity.getString(R.string.try_again))

                }
            } ?: run {
                context.showToast(activity.getString(R.string.try_again))
            }
        } catch (e: IntentSender.SendIntentException) {
            activity?.let {
                context.showToast(activity.getString(R.string.try_again))
            }

        } catch (e: Exception) {
            activity?.let {
                context.showToast(activity.getString(R.string.try_again))
            }
        }


    }

    override fun checkProductPurchaseHistory() {
        if (!isBillingClientReady()) return

        purchasesList.clear()
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { p0, p1 ->
            var purchasesFound = false
            if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
                if (p1.isNotEmpty()) {
                    for (purchase in p1) {
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            purchasesFound = true
                            if (purchase.isAcknowledged) {
                                purchasesList.add(
                                    purchase.products.firstOrNull().orEmpty()
                                )
                                subscriptionListener?.onSubscriptionPurchasedFetched(
                                    purchasesList
                                )
                            } else {
                                acknowledgePurchase(purchase)
                            }
                        }
                    }
                }
            }
            if (!purchasesFound) {
                subscriptionListener?.onSubscriptionPurchasedFetched(emptyList())
            }

        }
    }


    private fun isProductPurchased(list: List<Purchase>?) {


        if (!list.isNullOrEmpty()) {

            for (purchase in list) {

                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    if (purchase.isAcknowledged) {
                        purchasesList.add(purchase.products.firstOrNull().orEmpty())
                        subscriptionListener?.onSubscriptionPurchasedFetched(
                            purchasesList
                        )
                    } else {
                        acknowledgePurchase(
                            purchase
                        )
                    }
                }
            }
        }


//        val purchase = list?.toList()?.find { it.purchaseState == Purchase.PurchaseState.PURCHASED }
//            ?: return false
//        return if (purchase.products.contains(productId)) {
//            if (purchase.isAcknowledged) updatePurchaseStatus(true) else acknowledgePurchase(
//                purchase
//            )
//            true
//        } else false
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (!isBillingClientReady()) return

        val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        coroutineScope.launch {
            billingClient.acknowledgePurchase(acknowledgeParams) { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchasesList.add(purchase.products.firstOrNull().orEmpty())
                    subscriptionListener?.onSubscriptionPurchasedFetched(
                        purchasesList
                    )
//                    "Acknowledgment Successful".logIt(BILLING_TAG)
                } else {
//                    "Acknowledgment Failed: ${result.responseCode}".logIt(BILLING_TAG)
                }
            }
        }
    }

    private fun isBillingClientReady(): Boolean =
        isBillingClientInitialized && billingClient.isReady
}
