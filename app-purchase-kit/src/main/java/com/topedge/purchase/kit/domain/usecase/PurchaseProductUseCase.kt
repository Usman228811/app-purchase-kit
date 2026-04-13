package com.topedge.purchase.kit.domain.usecase

import android.app.Activity
import com.topedge.purchase.kit.domain.repo.BillingRepository

class PurchaseProductUseCase private constructor(
    private val billingRepository: BillingRepository
) {
    operator fun invoke(activity: Activity?,onUserDismissedPaywall :(()->Unit) ?= null) = billingRepository.purchaseProduct(activity,onUserDismissedPaywall)

    companion object {
        @Volatile
        private var instance: PurchaseProductUseCase? = null

        fun getInstance(billingRepository: BillingRepository): PurchaseProductUseCase {

            return instance ?: synchronized(this) {
                instance ?: PurchaseProductUseCase(billingRepository).also { instance = it }
            }
        }
    }
}


//class PurchaseProductUseCase(private val billingRepository: BillingRepository) {
//    operator fun invoke(activity: Activity?) = billingRepository.purchaseProduct(activity)
//}