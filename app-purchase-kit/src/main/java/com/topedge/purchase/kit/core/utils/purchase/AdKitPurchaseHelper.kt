package com.topedge.purchase.kit.core.utils.purchase

import android.app.Activity
import android.app.Application
import com.topedge.purchase.kit.data.impl.BillingRepositoryImpl
import com.topedge.purchase.kit.domain.repo.BillingRepository
import com.topedge.purchase.kit.domain.usecase.InitBillingUseCase
import com.topedge.purchase.kit.domain.usecase.PurchaseProductUseCase

class AdKitPurchaseHelper private constructor(
    private val init: InitBillingUseCase,
    private val purchase: PurchaseProductUseCase,
    private val billingRepository: BillingRepository
) {

    val productPriceFlow = billingRepository.productPriceFlow()
    val appPurchased = billingRepository.appPurchased()

    fun initBilling(productId: String) = init(productId)

    fun purchaseProduct(activity: Activity?,onUserDismissedPaywall :(()->Unit) ?= null) = purchase(activity, onUserDismissedPaywall)

    companion object {
        @Volatile
        private var instance: AdKitPurchaseHelper? = null

        internal fun getInstance(
            context: Application,
        ): AdKitPurchaseHelper {
            val billingRepo = BillingRepositoryImpl.getInstance(
                context.applicationContext,
            )

            return instance ?: synchronized(this) {
                instance ?: AdKitPurchaseHelper(
                    init = InitBillingUseCase.getInstance(billingRepo),
                    purchase = PurchaseProductUseCase.getInstance(billingRepo),
                    billingRepository = billingRepo
                ).also { instance = it }
            }
        }
    }
}