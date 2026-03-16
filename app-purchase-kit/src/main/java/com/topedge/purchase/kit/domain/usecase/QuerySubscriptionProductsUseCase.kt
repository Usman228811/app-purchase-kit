package com.topedge.purchase.kit.domain.usecase

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.topedge.purchase.kit.core.utils.init.PurchaseKit.preference
import com.topedge.purchase.kit.data.impl.SubscriptionRepositoryImpl
import com.topedge.purchase.kit.domain.repo.SubscriptionListener
import com.topedge.purchase.kit.domain.repo.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class Products(
    val products: Map<String, ProductDetails>? = null
)

data class PriceModel(
    val price: String,
    val offerPrice: String,
)


class QuerySubscriptionProductsUseCase private constructor(
    private val repository: SubscriptionRepository
) {

    companion object {

        @Volatile
        private var instance: QuerySubscriptionProductsUseCase? = null


        fun getInstance(
            context: Context
        ): QuerySubscriptionProductsUseCase {
            val repo = SubscriptionRepositoryImpl.getInstance(context)
            return instance ?: synchronized(this) {
                instance ?: QuerySubscriptionProductsUseCase(
                    repo
                ).also { instance = it }
            }
        }
    }

    private val _products = MutableStateFlow(Products())
    val products = _products.asStateFlow()

    private val _subscribedId = MutableStateFlow("")
    val subscribedId = _subscribedId.asStateFlow()

    private val _historyFetched = MutableStateFlow(false)
    val historyFetched = _historyFetched.asStateFlow()
    private val _isAppSubscribed = MutableStateFlow(false)
    val isAppSubscribed = _isAppSubscribed.asStateFlow()


    operator fun invoke(activity: Activity, productIds: List<String>) {
        repository.setBillingListener(
            activity = activity,
            object : SubscriptionListener {
                override fun onBillingInitialized() {
                    repository.querySubscriptionProducts(activity, productIds)
                }

                override fun onQueryProductSuccess(skuList: Map<String, ProductDetails>) {
                    _products.update {
                        it.copy(
                            products = skuList
                        )
                    }
                    repository.querySubscriptionHistory(activity)

                }

                override fun subscriptionItemNotFound() {

                }

                override fun checkPurchaseStatus(purchase: Purchase) {
                    try {
                        if (purchase.isAcknowledged) {
                            repository.setSubscribed(activity, purchase)
                            onSubscriptionPurchasedFetched()
                        } else {
                            repository.acknowledgedPurchase(activity, purchase)
                        }
                    } catch (_: Exception) {
                    }
                }

                override fun updatePref(subscribedId: String) {
                    try {
                        _subscribedId.value = subscribedId
                        preference.isAppSubscribed = subscribedId.isNotEmpty()
                        _isAppSubscribed.value = subscribedId.isNotEmpty()
                    } catch (_: Exception) {
                    }
                }

                override fun onSubscriptionPurchasedFetched() {
                    _historyFetched.value = historyFetched.value.not()
                }

            }
        )
    }


    fun isSubscriptionUpdateSupported() = repository.isSubscriptionUpdateSupported()
    fun querySubscriptionProducts(activity: Activity) {
        repository.querySubscriptionHistory(activity)
    }

    fun getBillingPrice(
        productId: String,
        offerId: String,
        billingPeriod: String
    ): PriceModel {

        val product = products.value.products?.get(productId)
            ?: return PriceModel("Empty", "Empty")

        val offers = product.subscriptionOfferDetails
            ?: return PriceModel("Empty", "Empty")

        // 🔹 Offer price (intro / discounted)
        val offerPrice = offers
            .firstOrNull { it.offerId == offerId }
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice
            .orEmpty()

        // 🔹 Billing (recurring) price
        val billingPrice = offers
            .firstOrNull() // base plan
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull { it.billingPeriod == billingPeriod }
            ?.formattedPrice
            ?: "Error fetching Price"

        return PriceModel(
            price = billingPrice,
            offerPrice = offerPrice
        )
    }

}