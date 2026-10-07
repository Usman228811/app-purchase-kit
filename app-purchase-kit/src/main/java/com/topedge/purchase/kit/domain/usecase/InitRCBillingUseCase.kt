package com.topedge.purchase.kit.domain.usecase

import com.topedge.purchase.kit.core.utils.init.PurchaseKit
import com.topedge.purchase.kit.core.utils.toInApp
import com.topedge.purchase.kit.domain.repo.BillingQueryResult
import com.topedge.purchase.kit.domain.repo.BillingRepository
import com.topedge.purchase.kit.domain.repo.RevenueCatBillingQueryResult
import com.topedge.purchase.kit.domain.repo.SubscriptionListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One-time (lifetime) products through RevenueCat.
 */
class InitRCBillingUseCase private constructor(
    private val billingRepository: BillingRepository
) {

    private val _ucState = MutableStateFlow(OneTimePurchaseState())
    val ucState = _ucState.asStateFlow()

    private var removeAdsIds = emptyList<String>()

    operator fun invoke(
        removeAdsIds: List<String>,
        featureIds: List<String>
    ) {
        this.removeAdsIds = removeAdsIds
        billingRepository.initBilling(removeAdsIds, featureIds, object : SubscriptionListener {
            override fun onQueryProductSuccess(result: BillingQueryResult) {
                val productList = (result as? RevenueCatBillingQueryResult)?.productList ?: return
                val offers = productList.map { it.toInApp() }
                _ucState.updateOneTimeOffers(offers)
                billingRepository.checkProductPurchaseHistory()
            }

            override fun subscriptionItemNotFound() = Unit

            override fun onSubscriptionPurchasedFetched(purchasesList: List<String>) {
                val uniquePurchases = purchasesList.distinctPurchases()
                PurchaseKit.preference.isLifeTimePurchased =
                    uniquePurchases.any { it in this@InitRCBillingUseCase.removeAdsIds }
                _ucState.updateOneTimePurchases(uniquePurchases)
            }
        })
    }

    companion object {
        @Volatile
        private var instance: InitRCBillingUseCase? = null

        fun getInstance(billingRepository: BillingRepository): InitRCBillingUseCase {
            return instance ?: synchronized(this) {
                instance ?: InitRCBillingUseCase(billingRepository).also { instance = it }
            }
        }
    }
}
