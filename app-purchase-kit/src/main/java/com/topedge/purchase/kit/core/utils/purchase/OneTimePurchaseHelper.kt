package com.topedge.purchase.kit.core.utils.purchase

import android.app.Activity
import android.content.Context
import com.topedge.purchase.kit.data.impl.BillingRepositoryImpl
import com.topedge.purchase.kit.domain.repo.BillingRepository
import com.topedge.purchase.kit.domain.usecase.InitBillingUseCase
import com.topedge.purchase.kit.domain.usecase.PurchaseProductUseCase

class OneTimePurchaseHelper private constructor(
    private val init: InitBillingUseCase,
    private val purchase: PurchaseProductUseCase,
    private val billingRepository: BillingRepository
) {

    val productPriceFlow = billingRepository.productPriceFlow()
    val appPurchased = billingRepository.appPurchased()

    fun initBilling(productId: String) = init(productId)

    fun purchaseProduct(activity: Activity?) = purchase(activity)

    companion object {
        @Volatile
        private var instance: OneTimePurchaseHelper? = null

        internal fun getInstance(
            context: Context,
        ): OneTimePurchaseHelper {
            val billingRepo = BillingRepositoryImpl.getInstance(
                context,
            )

            return instance ?: synchronized(this) {
                instance ?: OneTimePurchaseHelper(
                    init = InitBillingUseCase.getInstance(billingRepo),
                    purchase = PurchaseProductUseCase.getInstance(billingRepo),
                    billingRepository = billingRepo
                ).also { instance = it }
            }
        }
    }
}