package com.topedge.purchase.kit.domain.usecase

import android.app.Activity
import com.topedge.purchase.kit.domain.repo.BillingRepository

class PurchaseProductUseCase private constructor(
    private val billingRepositoryPlay: BillingRepository,
    private val billingRepositoryRC: BillingRepository,
) {
    fun purchasePlayProduct(
        activity: Activity?,
        productId: String, onUserDismissedPaywall: (() -> Unit)? = null
    ) = billingRepositoryPlay.purchaseProduct(activity, productId, onUserDismissedPaywall)

    fun purchaseRcProduct(
        activity: Activity?,
        productId: String, onUserDismissedPaywall: (() -> Unit)? = null
    ) = billingRepositoryRC.purchaseProduct(activity, productId, onUserDismissedPaywall)

    companion object {
        @Volatile
        private var instance: PurchaseProductUseCase? = null

        fun getInstance(
            playRepository: BillingRepository,
            rcRepository: BillingRepository
        ): PurchaseProductUseCase {
            return instance ?: synchronized(this) {
                instance ?: PurchaseProductUseCase(playRepository, rcRepository).also { instance = it }
            }
        }
    }
}
