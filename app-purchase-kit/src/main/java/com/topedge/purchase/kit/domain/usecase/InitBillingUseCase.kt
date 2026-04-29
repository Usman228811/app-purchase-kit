package com.topedge.purchase.kit.domain.usecase

import com.android.billingclient.api.ProductDetails
import com.topedge.purchase.kit.core.utils.init.PurchaseKit
import com.topedge.purchase.kit.core.utils.toInApp
import com.topedge.purchase.kit.domain.model.PremiumOffer
import com.topedge.purchase.kit.domain.repo.BillingRepository
import com.topedge.purchase.kit.domain.repo.SubscriptionListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


data class OneTimePurchaseState(
    val purchasesList: List<String> = emptyList(),
    val offers: List<PremiumOffer> = emptyList(),
)

class InitBillingUseCase private constructor(
    private val billingRepository: BillingRepository
) {


    private val _ucState = MutableStateFlow(OneTimePurchaseState())
    val ucState = _ucState.asStateFlow()

    private var removeAdsIds = listOf<String>()

    operator fun invoke(
        removeAdsIds: List<String>,
        featureIds: List<String>
    ) {
        this.removeAdsIds = removeAdsIds
        billingRepository.initBilling(removeAdsIds, featureIds, object : SubscriptionListener {
            override fun onQueryProductSuccess(
                skuList: Map<String, ProductDetails>,
                productList: List<ProductDetails>
            ) {
                val offers = productList
                    .mapNotNull { it.toInApp() }

                _ucState.update {
                    it.copy(offers = offers)
                }
                billingRepository.checkProductPurchaseHistory()
            }

            override fun subscriptionItemNotFound() {

            }

            override fun onSubscriptionPurchasedFetched(purchasesList: List<String>) {
                val uniquePurchases = purchasesList.distinct()
                val hasRemoveAds = uniquePurchases.any { it in this@InitBillingUseCase.removeAdsIds }

                PurchaseKit.preference.isLifeTimePurchased = hasRemoveAds
                _ucState.update {
                    it.copy(purchasesList = uniquePurchases)
                }
            }
        })
    }


    companion object {
        @Volatile
        private var instance: InitBillingUseCase? = null


        fun getInstance(billingRepository: BillingRepository): InitBillingUseCase {

            return instance ?: synchronized(this) {
                instance ?: InitBillingUseCase(billingRepository).also { instance = it }
            }
        }

    }
}