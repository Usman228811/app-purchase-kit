package com.topedge.purchase.kit.core.utils.purchase

import android.app.Activity
import android.app.Application
import com.topedge.purchase.kit.domain.model.OfferTexts
import com.topedge.purchase.kit.domain.model.OfferType
import com.topedge.purchase.kit.domain.model.PremiumOffer
import com.topedge.purchase.kit.domain.usecase.OneTimePurchaseState
import com.topedge.purchase.kit.domain.usecase.SubscriptionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PremiumAccessState(
    val isPremium: Boolean = false,
    val oneTimePurchases: List<String> = emptyList(),
    val subscriptionPurchases: List<String> = emptyList(),
    val allPurchases: List<String> = emptyList(),
    val oneTimeOffers: List<PremiumOffer> = emptyList(),
    val subscriptionOffers: List<PremiumOffer> = emptyList(),
)

enum class PremiumProductType {
    ONE_TIME,
    SUBSCRIPTION,
    UNKNOWN
}

class PurchaseKitPremiumHelper private constructor(
    private val purchaseHelper: AdKitPurchaseHelper,
    private val subscriptionHelper: AdKitSubscriptionHelper
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val premiumState: StateFlow<PremiumAccessState> = combine(
        purchaseHelper.oneTimePurchaseState,
        subscriptionHelper.subscriptionState
    ) { oneTimeState: OneTimePurchaseState, subscriptionState: SubscriptionState ->
        val oneTimePurchases = oneTimeState.purchasesList.distinct()
        val subscriptionPurchases = subscriptionState.purchasesList.distinct()
        val allPurchases = (oneTimePurchases + subscriptionPurchases).distinct()

        PremiumAccessState(
            isPremium = allPurchases.isNotEmpty(),
            oneTimePurchases = oneTimePurchases,
            subscriptionPurchases = subscriptionPurchases,
            allPurchases = allPurchases,
            oneTimeOffers = oneTimeState.offers,
            subscriptionOffers = subscriptionState.offers
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = PremiumAccessState()
    )
    fun initBilling(
        activity: Activity,
        lifetimeProductIds: List<String> = emptyList(),
        lifetimeFeatureIds: List<String> = emptyList(),
        subscriptionProductIds: List<String> = emptyList(),
        subscriptionFeatureIds: List<String> = emptyList()
    ) {
        if (lifetimeProductIds.isNotEmpty() || lifetimeFeatureIds.isNotEmpty()) {
            purchaseHelper.initBilling(
                removeAdsIds = lifetimeProductIds,
                featureIds = lifetimeFeatureIds
            )
        }

        if (subscriptionProductIds.isNotEmpty() || subscriptionFeatureIds.isNotEmpty()) {
            subscriptionHelper.initBilling(
                activity = activity,
                removeAdsIds = subscriptionProductIds,
                featureIds = subscriptionFeatureIds
            )
        }
    }

    fun hasPremiumAccess(): Boolean = premiumState.value.isPremium

    fun hasProduct(productId: String): Boolean = premiumState.value.allPurchases.contains(productId)

    fun getProductType(productId: String): PremiumProductType {
        return when {
            premiumState.value.oneTimeOffers.any { it.id == productId } -> PremiumProductType.ONE_TIME
            premiumState.value.subscriptionOffers.any { it.id == productId } -> PremiumProductType.SUBSCRIPTION
            else -> PremiumProductType.UNKNOWN
        }
    }

    fun getBillingPrice(productId: String): OfferTexts {
        return when (getProductType(productId)) {
            PremiumProductType.ONE_TIME -> {
                val oneTimeOffer = premiumState.value.oneTimeOffers
                    .firstOrNull { it.id == productId } as? PremiumOffer.InAppProduct

                OfferTexts(
                    type = OfferType.STRAIGHT,
                    period = null,
                    freeTrialText = null,
                    paidTrialText = null,
                    mainOfferText = oneTimeOffer?.price?.formattedPrice
                )
            }

            PremiumProductType.SUBSCRIPTION -> subscriptionHelper.getBillingPrice(productId)
            PremiumProductType.UNKNOWN -> OfferTexts(
                type = OfferType.STRAIGHT,
                period = null,
                freeTrialText = null,
                paidTrialText = null,
                mainOfferText = null
            )
        }
    }

    fun purchase(
        activity: Activity,
        productId: String?,
        isForUpdatePlan: Boolean = false,
        onUserDismissedPaywall: (() -> Unit)? = null
    ) {
        when (productId?.let(::getProductType)) {
            PremiumProductType.ONE_TIME -> {
                purchaseHelper.purchaseProduct(
                    activity = activity,
                    productId = productId,
                    onUserDismissedPaywall = onUserDismissedPaywall
                )
            }

            PremiumProductType.SUBSCRIPTION -> {
                subscriptionHelper.purchase(
                    activity = activity,
                    productId = productId,
                    isForUpdatePlan = isForUpdatePlan,
                    onUserDismissedPaywall = onUserDismissedPaywall
                )
            }

            PremiumProductType.UNKNOWN, null -> Unit
        }
    }

    fun isSubscriptionUpdateSupported(): Boolean = subscriptionHelper.isSubscriptionUpdateSupported()

    fun getOfferType(productId: String): OfferType = getBillingPrice(productId).type

    companion object {
        @Volatile
        private var instance: PurchaseKitPremiumHelper? = null

        internal fun getInstance(context: Application): PurchaseKitPremiumHelper {
            return instance ?: synchronized(this) {
                instance ?: PurchaseKitPremiumHelper(
                    purchaseHelper = AdKitPurchaseHelper.getInstance(context),
                    subscriptionHelper = AdKitSubscriptionHelper.getInstance(context)
                ).also { instance = it }
            }
        }
    }
}
