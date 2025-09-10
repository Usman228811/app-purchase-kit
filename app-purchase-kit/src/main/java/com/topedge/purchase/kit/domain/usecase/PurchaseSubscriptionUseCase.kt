package com.topedge.purchase.kit.domain.usecase

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.ProductDetails
import com.topedge.purchase.kit.data.impl.SubscriptionRepositoryImpl
import com.topedge.purchase.kit.domain.repo.SubscriptionRepository


class PurchaseSubscriptionUseCase private constructor(
    private val repository: SubscriptionRepository
) {

    companion object {
        @Volatile
        private var instance: PurchaseSubscriptionUseCase? = null


        fun getInstance(
            context: Context
        ): PurchaseSubscriptionUseCase {
            val repo = SubscriptionRepositoryImpl.getInstance(context)
            return instance ?: synchronized(this) {
                instance ?: PurchaseSubscriptionUseCase(
                    repo
                ).also { instance = it }
            }
        }
    }


    operator fun invoke(activity: Activity,product: ProductDetails) = repository.purchaseProduct(activity,product)

    fun changeSubscriptionPlan(activity: Activity,product: ProductDetails) {
        repository.changeSubscriptionPlan(activity,product)
    }

    fun viewUrl(activity: Activity, url: String) {
        repository.viewUrl(activity, url)
    }

}