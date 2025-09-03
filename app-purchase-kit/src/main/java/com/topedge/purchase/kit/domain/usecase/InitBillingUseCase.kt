package com.topedge.purchase.kit.domain.usecase

import com.topedge.purchase.kit.domain.repo.BillingRepository


class InitBillingUseCase private constructor(
    private val billingRepository: BillingRepository
) {
    operator fun invoke(productId: String) = billingRepository.initBilling(productId)

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
