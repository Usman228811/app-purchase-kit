package com.topedge.purchase.kit.core.utils

import android.util.Log
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.models.PricingPhase
import com.topedge.purchase.kit.domain.model.PremiumOffer
import com.topedge.purchase.kit.domain.model.offer.period.OfferPeriod
import com.topedge.purchase.kit.domain.model.offer.period.OfferTimePeriod
import com.topedge.purchase.kit.domain.model.offer.period.Period
import com.topedge.purchase.kit.domain.model.offer.period.PremiumOfferPhase
import com.topedge.purchase.kit.domain.model.offer.price.OfferPrice
import com.revenuecat.purchases.models.Period.Unit as PeriodUnit


private const val TAG = "RCProductDetailsMappingTAG"

fun Package.toInApp(): PremiumOffer {
    val offerPrice = OfferPrice(
        currency = product.price.currencyCode,
        priceMicros = product.price.amountMicros,
        formattedPrice = product.price.formatted
    )
    val offerPeriod = OfferTimePeriod.Lifetime
    return PremiumOffer.InAppProduct(
        id = identifier,
        name = product.name,
        price = offerPrice,
        period = offerPeriod
    )
}

fun Package.toSubscription(): PremiumOffer? {
    val phases: List<PricingPhase> = product.defaultOption?.pricingPhases ?: return null
    Log.d(TAG, "toSubscription: phases lists size ${phases.size}")
    phases.forEach {
        Log.d(TAG, "toSubscription: ${it.price.formatted} ${it.price.amountMicros}")
    }
    val paidPhases = phases.filter { it.price.amountMicros > 0L }
    val trialPhase =
        phases.firstOrNull { it.price.amountMicros == 0L || it.price.formatted == TRIAL_PRICE }

    val paidOfferPhases = paidPhases.map { it.toPremiumOfferPhase() }
    val trialOfferPhase = trialPhase?.toPremiumOfferPhase()

    return PremiumOffer.Subscription(
        id = identifier,
        name = product.name,
        paidPhases = paidOfferPhases,
        trialPhase = trialOfferPhase
    )
}

fun PricingPhase.toOfferPrice(): OfferPrice {
    return OfferPrice(
        currency = this.price.currencyCode,
        priceMicros = this.price.amountMicros,
        formattedPrice = this.price.formatted
    )
}

fun PricingPhase.toPremiumOfferPhase(): PremiumOfferPhase {
    return PremiumOfferPhase(
        price = this.toOfferPrice(),
        period = this.toOfferPeriod()
    )
}

fun PricingPhase.toOfferPeriod(): OfferPeriod {
    val period = this.billingPeriod.unit.toPeriod()
    val count = this.billingPeriod.value
    return OfferPeriod(period, count)
}

fun PeriodUnit.toPeriod(): Period {
    return when (this) {
        PeriodUnit.DAY -> Period.DAY
        PeriodUnit.WEEK -> Period.WEEK
        PeriodUnit.MONTH -> Period.MONTH
        PeriodUnit.YEAR -> Period.YEAR
        PeriodUnit.UNKNOWN -> Period.UNKNOWN
    }
}
