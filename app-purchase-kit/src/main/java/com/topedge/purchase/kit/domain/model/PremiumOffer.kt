package com.topedge.purchase.kit.domain.model

import com.topedge.purchase.kit.domain.model.offer.period.OfferTimePeriod
import com.topedge.purchase.kit.domain.model.offer.period.PremiumOfferPhase
import com.topedge.purchase.kit.domain.model.offer.price.OfferPrice

sealed class PremiumOffer(
    open val id: String,
    open val name: String
) {
    data class InAppProduct(
        override val id: String,
        override val name: String,
        val price: OfferPrice,
        val period: OfferTimePeriod
    ) : PremiumOffer(id, name)

    data class Subscription(
        override val id: String,
        override val name: String,
        val paidPhases: List<PremiumOfferPhase>,
        val trialPhase: PremiumOfferPhase? = null
    ) : PremiumOffer(id, name)
}

enum class OfferType {
    FREE_TRIAL,
    PAID_TRIAL,
    STRAIGHT
}

data class OfferTexts(
    val type: OfferType,
    val period:String?,
    val freeTrialText: String?,
    val paidTrialText: String?,
    val mainOfferText: String?
)