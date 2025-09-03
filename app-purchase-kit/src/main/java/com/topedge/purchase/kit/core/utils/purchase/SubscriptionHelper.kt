package com.topedge.purchase.kit.core.utils.purchase

import android.app.Activity
import android.content.Context
import com.topedge.purchase.kit.core.utils.init.PurchaseKit
import com.topedge.purchase.kit.domain.usecase.PurchaseSubscriptionUseCase
import com.topedge.purchase.kit.domain.usecase.QuerySubscriptionProductsUseCase

class SubscriptionHelper private constructor(
    private val queryProducts: QuerySubscriptionProductsUseCase,
    private val purchaseProduct: PurchaseSubscriptionUseCase
) {

    companion object {
        @Volatile
        private var instance: SubscriptionHelper? = null


        internal fun getInstance(
            context: Context
        ): SubscriptionHelper {
            return instance ?: synchronized(this) {
                instance ?: SubscriptionHelper(
                    QuerySubscriptionProductsUseCase.getInstance(context),
                    PurchaseSubscriptionUseCase.getInstance(context),

                    ).also { instance = it }
            }
        }
    }


    val subscriptionProducts = queryProducts.products
    val historyFetched = queryProducts.historyFetched
    val subscribedId = queryProducts.subscribedId

    fun loadProducts(activity: Activity, productIds: List<String>) {
        queryProducts(activity, productIds)
    }

    fun querySubscriptionProducts() {
        queryProducts.querySubscriptionProducts()
    }

    fun isSubscriptionUpdateSupported() = queryProducts.isSubscriptionUpdateSupported()

    fun getBillingPrice(productId: String, billingPeriod: String): String {
        return queryProducts.getBillingPrice(productId, billingPeriod)
    }

    fun purchase(
        activity: Activity,
        productId: String?
    ) {

        when {
            PurchaseKit.internetHelper.isConnected.not() || productId == null -> {

            }

            subscribedId.value == productId -> {
                purchaseProduct.viewUrl(activity,"https://play.google.com/store/account/subscriptions?sku=${productId}&package=${activity.packageName}")
            }

            subscribedId.value == "" -> {

                subscriptionProducts.value.products?.let { products ->
                    products[productId]?.let {
                        purchaseProduct(it)
                    }
                }

            }

            isSubscriptionUpdateSupported() -> {

                subscriptionProducts.value.products?.let { products ->
                    products[productId]?.let {
                        purchaseProduct.changeSubscriptionPlan(it)
                    }
                }
            }
        }

    }
}