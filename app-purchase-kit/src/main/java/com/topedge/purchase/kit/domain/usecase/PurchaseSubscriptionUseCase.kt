package com.topedge.purchase.kit.domain.usecase

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.Package
import com.topedge.purchase.kit.data.impl.RCSubscriptionRepositoryImpl
import com.topedge.purchase.kit.data.impl.SubscriptionRepositoryImpl
import com.topedge.purchase.kit.domain.repo.SubscriptionRepository


class PurchaseSubscriptionUseCase private constructor(
    private val repositoryPlay: SubscriptionRepository,
    private val repositoryRc: SubscriptionRepository,
) {

    companion object {
        @Volatile
        private var instance: PurchaseSubscriptionUseCase? = null


        fun getInstance(
            context: Context
        ): PurchaseSubscriptionUseCase {
            val repoPlay = SubscriptionRepositoryImpl.getInstance(context)
            val repoRc = RCSubscriptionRepositoryImpl.getInstance(context)
            return instance ?: synchronized(this) {
                instance ?: PurchaseSubscriptionUseCase(
                    repoPlay, repoRc
                ).also { instance = it }
            }
        }
    }


    fun purchasePlayProduct(
        activity: Activity,
        product: ProductDetails,
        onUserDismissedPaywall: (() -> Unit)? = null
    ) = repositoryPlay.purchaseProduct(activity, product, onUserDismissedPaywall)

    fun purchaseRcProduct(
        activity: Activity,
        product: Package,
        onUserDismissedPaywall: (() -> Unit)? = null
    ) = repositoryRc.purchaseProduct(activity, product, onUserDismissedPaywall)

    fun changeSubscriptionPlan(activity: Activity,product: ProductDetails) {
        repositoryPlay.changeSubscriptionPlan(activity,product)
    }

    fun viewUrl(activity: Activity, url: String) {
        repositoryPlay.viewUrl(activity, url)
    }

}
