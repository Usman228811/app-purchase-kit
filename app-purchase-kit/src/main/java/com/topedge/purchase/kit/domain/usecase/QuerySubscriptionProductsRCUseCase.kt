package com.topedge.purchase.kit.domain.usecase

import android.app.Activity
import android.content.Context
import com.revenuecat.purchases.Package
import com.topedge.purchase.kit.core.utils.init.PurchaseKit.preference
import com.topedge.purchase.kit.core.utils.toSubscription
import com.topedge.purchase.kit.data.impl.RCSubscriptionRepositoryImpl
import com.topedge.purchase.kit.domain.model.OfferTexts
import com.topedge.purchase.kit.domain.repo.BillingQueryResult
import com.topedge.purchase.kit.domain.repo.RevenueCatBillingQueryResult
import com.topedge.purchase.kit.domain.repo.SubscriptionListener
import com.topedge.purchase.kit.domain.repo.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Subscription products through RevenueCat.
 */
class QuerySubscriptionProductsRCUseCase private constructor(
    private val repository: SubscriptionRepository
) {

    companion object {
        @Volatile
        private var instance: QuerySubscriptionProductsRCUseCase? = null

        fun getInstance(context: Context): QuerySubscriptionProductsRCUseCase {
            val repo = RCSubscriptionRepositoryImpl.getInstance(context)
            return instance ?: synchronized(this) {
                instance ?: QuerySubscriptionProductsRCUseCase(repo).also { instance = it }
            }
        }
    }

    private var productsMap: Map<String, Package>? = null

    private val _ucState = MutableStateFlow(SubscriptionState())
    val ucState = _ucState.asStateFlow()

    fun getProducts(): Map<String, Package>? = productsMap

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
                    val revenueCatResult = result as? RevenueCatBillingQueryResult ?: return
                    productsMap = revenueCatResult.skuList
                    val offers = revenueCatResult.productList.mapNotNull { it.toSubscription() }
                    _ucState.updateSubscriptionOffers(offers)
                    repository.querySubscriptionHistory(activity)
                }

                override fun onSubscriptionPurchasedFetched(purchasesList: List<String>) {
                    val uniquePurchases = purchasesList.distinctPurchases()
                    preference.isAppSubscribed =
                        uniquePurchases.any { it in this@QuerySubscriptionProductsRCUseCase.removeAdsIds }
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
