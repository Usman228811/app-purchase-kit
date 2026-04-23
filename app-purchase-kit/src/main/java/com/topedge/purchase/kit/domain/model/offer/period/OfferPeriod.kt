package com.topedge.purchase.kit.domain.model.offer.period

data class OfferPeriod(
    val period: Period,
    val count: Int
)

enum class Period { DAY, WEEK, MONTH, YEAR, UNKNOWN }