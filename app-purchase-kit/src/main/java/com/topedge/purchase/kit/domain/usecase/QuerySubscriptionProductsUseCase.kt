package com.topedge.purchase.kit.domain.usecase

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.ProductDetails
import com.topedge.purchase.kit.core.utils.init.PurchaseKit.preference
import com.topedge.purchase.kit.core.utils.toSubscription
import com.topedge.purchase.kit.data.impl.SubscriptionRepositoryImpl
import com.topedge.purchase.kit.domain.model.OfferTexts
import com.topedge.purchase.kit.domain.model.PremiumOffer
import com.topedge.purchase.kit.domain.repo.BillingQueryResult
import com.topedge.purchase.kit.domain.repo.PlayBillingQueryResult
import com.topedge.purchase.kit.domain.repo.SubscriptionListener
import com.topedge.purchase.kit.domain.repo.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SubscriptionState(
    val purchasesList: List<String> = emptyList(),
    val offers: List<PremiumOffer> = emptyList()
)

/**
 * Subscription products through Google Play Billing.
 */
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

    var productsMap: Map<String, ProductDetails>? = null

    private val _ucState = MutableStateFlow(SubscriptionState())
    val ucState = _ucState.asStateFlow()

    fun getProducts(): Map<String, ProductDetails>? = productsMap

    private var removeAdsIds = emptyList<String>()

    operator fun invoke(
        activity: Activity,
        removeAdsIds: List<String>,
        featureIds: List<String>
    ) {
        this.removeAdsIds = removeAdsIds
        repository.setBillingListener(
            activity = activity,
            removeAdsIds = removeAdsIds,
            featureIds = featureIds,
            listener = object : SubscriptionListener {
                override fun onQueryProductSuccess(result: BillingQueryResult) {
                    val playResult = result as? PlayBillingQueryResult ?: return
                    productsMap = playResult.skuList
                    val offers = playResult.productList.mapNotNull { it.toSubscription() }
                    _ucState.updateSubscriptionOffers(offers)
                    repository.querySubscriptionHistory(activity)
                }

                override fun onSubscriptionPurchasedFetched(purchasesList: List<String>) {
                    val uniquePurchases = purchasesList.distinctPurchases()
                    preference.isAppSubscribed =
                        uniquePurchases.any { it in this@QuerySubscriptionProductsUseCase.removeAdsIds }
                    _ucState.updateSubscriptionPurchases(uniquePurchases)
                }

                override fun subscriptionItemNotFound() = Unit
            }
        )
    }


    fun isSubscriptionUpdateSupported() = repository.isSubscriptionUpdateSupported()


    fun buildOfferTexts(offerId: String): OfferTexts {
        return buildSubscriptionOfferTexts(ucState.value.offers, offerId)
    }

}
