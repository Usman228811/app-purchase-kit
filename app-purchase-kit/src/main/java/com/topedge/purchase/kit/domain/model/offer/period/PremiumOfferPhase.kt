package com.topedge.purchase.kit.domain.model.offer.period
import com.topedge.purchase.kit.domain.model.offer.price.OfferPrice

data class PremiumOfferPhase(
    val price: OfferPrice,
    val period: OfferPeriod
)