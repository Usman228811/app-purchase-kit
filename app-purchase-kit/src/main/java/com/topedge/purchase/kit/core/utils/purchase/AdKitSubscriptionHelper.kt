package com.topedge.purchase.kit.core.utils.purchase

import android.app.Activity
import android.app.Application
import com.topedge.purchase.kit.core.utils.init.PurchaseKit.internetHelper
import com.topedge.purchase.kit.domain.usecase.PriceModel
import com.topedge.purchase.kit.domain.usecase.PurchaseSubscriptionUseCase
import com.topedge.purchase.kit.domain.usecase.QuerySubscriptionProductsUseCase

class AdKitSubscriptionHelper private constructor(
    private val queryProducts: QuerySubscriptionProductsUseCase,
    private val purchaseProduct: PurchaseSubscriptionUseCase
) {

    companion object {
        @Volatile
        private var instance: AdKitSubscriptionHelper? = null


        internal fun getInstance(
            context: Application
        ): AdKitSubscriptionHelper {
            return instance ?: synchronized(this) {
                instance ?: AdKitSubscriptionHelper(
                    QuerySubscriptionProductsUseCase.getInstance(context.applicationContext),
                    PurchaseSubscriptionUseCase.getInstance(context.applicationContext),

                    ).also { instance = it }
            }
        }
    }


    val subscriptionProducts = queryProducts.products
    val historyFetched = queryProducts.historyFetched
    val subscribedId = queryProducts.subscribedId
    val isAppSubscribed = queryProducts.isAppSubscribed

    fun initBilling(activity: Activity, productIds: List<String>) {
        queryProducts(activity, productIds)
    }

    fun querySubscriptionProducts(activity: Activity) {
        queryProducts.querySubscriptionProducts(activity)
    }

    fun isSubscriptionUpdateSupported() = queryProducts.isSubscriptionUpdateSupported()

    fun getBillingPrice(
        productId: String,
        offerId: String = "",
        billingPeriod: String
    ): PriceModel {
        return queryProducts.getBillingPrice(productId, offerId, billingPeriod)
    }

    fun purchase(
        activity: Activity,
        productId: String?,
        onUserDismissedPaywall :(()->Unit) ?= null
    ) {

        when {
            internetHelper.isConnected.not() || productId == null -> {

            }

            subscribedId.value == productId -> {
                purchaseProduct.viewUrl(
                    activity,
                    "https://play.google.com/store/account/subscriptions?sku=${productId}&package=${activity.packageName}"
                )
            }

            subscribedId.value == "" -> {

                subscriptionProducts.value.products?.let { products ->
                    products[productId]?.let {
                        purchaseProduct(activity, it, onUserDismissedPaywall)
                    }
                }

            }

            isSubscriptionUpdateSupported() -> {

                subscriptionProducts.value.products?.let { products ->
                    products[productId]?.let {
                        purchaseProduct.changeSubscriptionPlan(activity, it)
                    }
                }
            }
        }

    }
}