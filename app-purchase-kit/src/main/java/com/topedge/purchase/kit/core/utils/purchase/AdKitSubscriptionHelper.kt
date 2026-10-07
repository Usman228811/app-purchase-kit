package com.topedge.purchase.kit.core.utils.purchase

import android.app.Activity
import android.app.Application
import com.topedge.purchase.kit.core.utils.init.PurchaseKit.internetHelper
import com.topedge.purchase.kit.domain.model.OfferTexts
import com.topedge.purchase.kit.domain.usecase.PurchaseSubscriptionUseCase
import com.topedge.purchase.kit.domain.usecase.QuerySubscriptionProductsRCUseCase
import com.topedge.purchase.kit.domain.usecase.QuerySubscriptionProductsUseCase
import com.topedge.purchase.kit.domain.usecase.SubscriptionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class AdKitSubscriptionHelper private constructor(
    private val queryProducts: QuerySubscriptionProductsUseCase,
    private val queryProductsRc: QuerySubscriptionProductsRCUseCase,
    private val purchaseProduct: PurchaseSubscriptionUseCase
) {

    companion object {
        @Volatile
        private var instance: AdKitSubscriptionHelper? = null


        internal fun getInstance(
            context: Application
        ): AdKitSubscriptionHelper {
            return instance ?: synchronized(this) {
                instance ?: AdKitSubscriptionHelper(
                    QuerySubscriptionProductsUseCase.getInstance(context.applicationContext),
                    QuerySubscriptionProductsRCUseCase.getInstance(context.applicationContext),
                    PurchaseSubscriptionUseCase.getInstance(context.applicationContext),
                ).also { instance = it }
            }
        }
    }


    private val billingProvider = MutableStateFlow(PremiumBillingProvider.PLAY)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)


    internal fun initBilling(
        activity: Activity,
        removeAdsIds: List<String>,
        featureIds: List<String>,
        provider: PremiumBillingProvider
    ) {
        billingProvider.value = provider
        when (provider) {
            PremiumBillingProvider.REVENUE_CAT ->
                queryProductsRc(activity = activity, removeAdsIds = removeAdsIds, featureIds = featureIds)

            PremiumBillingProvider.PLAY ->
                queryProducts(activity = activity, removeAdsIds = removeAdsIds, featureIds = featureIds)
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    val subscriptionState: StateFlow<SubscriptionState> = billingProvider.flatMapLatest { provider ->
        when (provider) {
            PremiumBillingProvider.REVENUE_CAT -> queryProductsRc.ucState
            PremiumBillingProvider.PLAY -> queryProducts.ucState
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = SubscriptionState()
    )


    fun isSubscriptionUpdateSupported() = when (billingProvider.value) {
        PremiumBillingProvider.REVENUE_CAT -> queryProductsRc.isSubscriptionUpdateSupported()
        PremiumBillingProvider.PLAY -> queryProducts.isSubscriptionUpdateSupported()
    }

    fun getBillingPrice(
        productId: String,
    ): OfferTexts {
        return when (billingProvider.value) {
            PremiumBillingProvider.REVENUE_CAT -> queryProductsRc.buildOfferTexts(productId)
            PremiumBillingProvider.PLAY -> queryProducts.buildOfferTexts(productId)
        }
    }

    private fun isAlreadySubscribed(productId: String): Boolean {
        return subscriptionState.value.purchasesList.contains(productId)
    }


    fun purchase(
        activity: Activity,
        productId: String?,
        isForUpdatePlan: Boolean, onUserDismissedPaywall: (() -> Unit)? = null,

        ) {

        when {
            internetHelper.isConnected.not() || productId == null -> Unit

            isAlreadySubscribed(productId) -> {
                purchaseProduct.viewUrl(
                    activity,
                    "https://play.google.com/store/account/subscriptions?sku=${productId}&package=${activity.packageName}"
                )
            }

            subscriptionState.value.purchasesList.isEmpty() -> {
                launchPurchase(activity, productId, onUserDismissedPaywall)
            }

            !isForUpdatePlan -> {
                launchPurchase(activity, productId, onUserDismissedPaywall)
            }

            isSubscriptionUpdateSupported() -> {
                queryProducts.getProducts()?.let { products ->
                    products[productId]?.let {
                        purchaseProduct.changeSubscriptionPlan(activity, it)
                    }
                }
            }
        }

    }

    private fun launchPurchase(
        activity: Activity,
        productId: String,
        onUserDismissedPaywall: (() -> Unit)?,
    ) {
        when (billingProvider.value) {
            PremiumBillingProvider.REVENUE_CAT -> {
                queryProductsRc.getProducts()?.get(productId)?.let { productPackage ->
                    purchaseProduct.purchaseRcProduct(activity, productPackage, onUserDismissedPaywall)
                }
            }

            PremiumBillingProvider.PLAY -> {
                queryProducts.getProducts()?.get(productId)?.let { productDetails ->
                    purchaseProduct.purchasePlayProduct(activity, productDetails, onUserDismissedPaywall)
                }
            }
        }
    }
}
