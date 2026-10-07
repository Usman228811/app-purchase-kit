package com.topedge.purchase.kit.core.utils.purchase

import android.app.Activity
import android.app.Application
import com.topedge.purchase.kit.data.impl.BillingRepositoryImpl
import com.topedge.purchase.kit.data.impl.RCBillingRepositoryImpl
import com.topedge.purchase.kit.domain.usecase.InitBillingUseCase
import com.topedge.purchase.kit.domain.usecase.InitRCBillingUseCase
import com.topedge.purchase.kit.domain.usecase.OneTimePurchaseState
import com.topedge.purchase.kit.domain.usecase.PurchaseProductUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class AdKitPurchaseHelper private constructor(
    private val init: InitBillingUseCase,
    private val initRc: InitRCBillingUseCase,
    private val purchase: PurchaseProductUseCase,
) {

    private val billingProvider = MutableStateFlow(PremiumBillingProvider.PLAY)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    internal fun initBilling(
        removeAdsIds: List<String>,
        featureIds: List<String>,
        provider: PremiumBillingProvider
    ) {
        billingProvider.value = provider
        when (provider) {
            PremiumBillingProvider.REVENUE_CAT -> initRc(removeAdsIds = removeAdsIds, featureIds = featureIds)
            PremiumBillingProvider.PLAY -> init(removeAdsIds = removeAdsIds, featureIds = featureIds)
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    val oneTimePurchaseState: StateFlow<OneTimePurchaseState> =
        billingProvider.flatMapLatest { provider ->
            when (provider) {
                PremiumBillingProvider.REVENUE_CAT -> initRc.ucState
                PremiumBillingProvider.PLAY -> init.ucState
            }
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = OneTimePurchaseState()
        )


    fun purchaseProduct(
        activity: Activity?,
        productId: String,
        onUserDismissedPaywall: (() -> Unit)? = null
    ) {
        when (billingProvider.value) {
            PremiumBillingProvider.REVENUE_CAT ->
                purchase.purchaseRcProduct(activity, productId, onUserDismissedPaywall)

            PremiumBillingProvider.PLAY ->
                purchase.purchasePlayProduct(activity, productId, onUserDismissedPaywall)
        }
    }

    companion object {
        @Volatile
        private var instance: AdKitPurchaseHelper? = null

        internal fun getInstance(
            context: Application,
        ): AdKitPurchaseHelper {
            val billingRepo = BillingRepositoryImpl.getInstance(context.applicationContext)
            val billingRepoRc = RCBillingRepositoryImpl.getInstance(context.applicationContext)

            return instance ?: synchronized(this) {
                instance ?: AdKitPurchaseHelper(
                    init = InitBillingUseCase.getInstance(billingRepo),
                    initRc = InitRCBillingUseCase.getInstance(billingRepoRc),
                    purchase = PurchaseProductUseCase.getInstance(billingRepo, billingRepoRc),
                ).also { instance = it }
            }
        }
    }
}
