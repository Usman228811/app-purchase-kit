package com.topedge.purchase.kit.domain.usecase

import android.app.Activity
import com.topedge.purchase.kit.domain.repo.BillingRepository
class PurchaseProductUseCase private constructor(
    private val billingRepository: BillingRepository
) {
    operator fun invoke(
        activity: Activity?,
        productId: String, onUserDismissedPaywall: (() -> Unit)? = null
    ) = billingRepository.purchaseProduct(activity, productId, onUserDismissedPaywall)

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