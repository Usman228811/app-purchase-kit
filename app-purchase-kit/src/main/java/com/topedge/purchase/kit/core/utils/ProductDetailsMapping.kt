package com.topedge.purchase.kit.core.utils

import android.util.Log
import com.android.billingclient.api.ProductDetails
import com.topedge.purchase.kit.domain.model.PremiumOffer
import com.topedge.purchase.kit.domain.model.offer.period.OfferPeriod
import com.topedge.purchase.kit.domain.model.offer.period.OfferTimePeriod
import com.topedge.purchase.kit.domain.model.offer.period.Period
import com.topedge.purchase.kit.domain.model.offer.period.PremiumOfferPhase
import com.topedge.purchase.kit.domain.model.offer.price.OfferPrice


private const val TAG = "ProductDetailsMappingTAG"
const val TRIAL_PRICE = "Free"

fun ProductDetails.toInApp(): PremiumOffer? {
    return oneTimePurchaseOfferDetailsList?.filterNotNull()?.first()?.let { oneTime ->
        val offerPrice = OfferPrice(
            currency = oneTime.priceCurrencyCode,
            priceMicros = oneTime.priceAmountMicros,
            formattedPrice = oneTime.formattedPrice
        )
        val offerPeriod = OfferTimePeriod.Lifetime
        PremiumOffer.InAppProduct(
            id = productId,
            name = name,
            price = offerPrice,
            period = offerPeriod
        )
    }
}

fun ProductDetails.toSubscription(): PremiumOffer? {
    return subscriptionOfferDetails?.filterNotNull()?.first()?.let { subs ->
        val phases: List<ProductDetails.PricingPhase> = subs.pricingPhases.pricingPhaseList
        Log.d(TAG, "toSubscription: phases lists size ${phases.size}")
        phases.forEach {
            Log.d(TAG, "toSubscription: ${it.formattedPrice} ${it.priceAmountMicros}")
        }
        val paidPhases = phases.filter { it.priceAmountMicros > 0L }
        val trialPhase =
            phases.firstOrNull { it.priceAmountMicros == 0L || it.formattedPrice == TRIAL_PRICE }

        val paidOfferPhases = paidPhases.map { it.toPremiumOfferPhase() }
        val trialOfferPhase = trialPhase?.toPremiumOfferPhase()

        PremiumOffer.Subscription(
            id = productId,
            name = name,
            paidPhases = paidOfferPhases,
            trialPhase = trialOfferPhase
        )
    }
}

fun ProductDetails.PricingPhase.toOfferPrice(): OfferPrice {
    return OfferPrice(
        currency = this.priceCurrencyCode,
        priceMicros = this.priceAmountMicros,
        formattedPrice = this.formattedPrice
    )
}

fun ProductDetails.PricingPhase.toPremiumOfferPhase(): PremiumOfferPhase {
    return PremiumOfferPhase(
        price = this.toOfferPrice(),
        period = this.toOfferPeriod()
    )
}

fun ProductDetails.PricingPhase.toOfferPeriod(): OfferPeriod {
    val period = this.billingPeriod.toPeriod()
    val count = this.billingPeriod.toCount()
    return OfferPeriod(period, count)
}

fun String.toPeriod(): Period {
    val periodString = this.last()
    return when (periodString) {
        'D' -> Period.DAY
        'W' -> Period.WEEK
        'M' -> Period.MONTH
        'Y' -> Period.YEAR
        else -> Period.UNKNOWN
    }
}

fun String.toCount(): Int {
    return if (this.length > 2) {
        val modifiedString = this.drop(1).dropLast(1)
        modifiedString.toInt()
    } else {
        -1
    }
}