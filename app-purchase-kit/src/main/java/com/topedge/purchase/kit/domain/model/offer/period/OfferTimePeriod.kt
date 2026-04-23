package com.topedge.purchase.kit.domain.model.offer.period
import com.topedge.purchase.kit.domain.model.offer.period.OfferPeriod

sealed interface OfferTimePeriod {

    data object Lifetime : OfferTimePeriod
    data class Timed(val period: OfferPeriod)
}