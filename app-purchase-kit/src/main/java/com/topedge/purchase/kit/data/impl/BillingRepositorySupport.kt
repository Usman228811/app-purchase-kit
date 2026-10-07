package com.topedge.purchase.kit.data.impl

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import androidx.core.net.toUri
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.topedge.purchase.kit.R
import com.topedge.purchase.kit.core.utils.showToast
import com.topedge.purchase.kit.domain.repo.SubscriptionListener


internal fun Context.showTryAgain(activity: Activity) {
    showToast(activity.getString(R.string.try_again))
}

internal fun Context.showNoInternet(activity: Activity) {
    showToast(activity.getString(R.string.no_internet))
}

internal fun Activity.openBrowsableUrl(url: String) {
    try {
        Intent().apply {
            action = Intent.ACTION_VIEW
            data = url.toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addCategory(Intent.CATEGORY_BROWSABLE)
        }.also { intent ->
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        }
    } catch (_: Exception) {
    }
}

internal fun List<ProductDetails>.toProductDetailsMap(): Map<String, ProductDetails> {
    val skuDetailList = mutableMapOf<String, ProductDetails>()
    forEach { details ->
        val sku = details.productId
        if (!TextUtils.isEmpty(sku)) {
            skuDetailList[sku] = details
        }
    }
    return skuDetailList
}

internal fun ProductDetails.toBillingFlowParams(): BillingFlowParams? {
    val offerToken = subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return null
    return BillingFlowParams.newBuilder()
        .setProductDetailsParamsList(
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(this)
                    .setOfferToken(offerToken)
                    .build()
            )
        )
        .build()
}

internal fun Purchase.primaryProductId(): String = products.firstOrNull().orEmpty()

internal fun MutableList<String>.addDistinct(value: String): List<String> {
    if (value.isNotEmpty() && value !in this) {
        add(value)
    }
    return toList()
}

internal fun SubscriptionListener?.dispatchPurchases(purchases: List<String>) {
    this?.onSubscriptionPurchasedFetched(purchases.distinct())
}
