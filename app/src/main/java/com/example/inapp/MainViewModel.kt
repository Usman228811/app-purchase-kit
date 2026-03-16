package com.example.inapp

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.topedge.purchase.kit.core.utils.init.PurchaseKit
import com.topedge.purchase.kit.domain.usecase.PriceModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel() as T
    }
}

data class MainState(
//    val weeklyPrice: String = "",
    val monthlyPrice: String = "",
    val yearlyPrice: String = "",
    val subscribedId: String = "",
    val selectedButtonPos: Int = 0,
    val buttonText: String = "subscribe",
    val oneTimePrice: String = "",
    val lifetimePurchased: Boolean = false,

    )

class MainViewModel() : ViewModel() {

    private var _state = MutableStateFlow(MainState())
    val state = _state.asStateFlow()

    private val subscriptionMap = mapOf(
        0 to "monthly",
        1 to "yearly"
    )

    private fun selectedId() = subscriptionMap[state.value.selectedButtonPos]

    init {
        PurchaseKit.oneTimePurchaseHelper.initBilling("android.test.purchased")
        viewModelScope.apply {
            launch {
                PurchaseKit.oneTimePurchaseHelper.productPriceFlow.collectLatest { model ->
                    _state.update {
                        it.copy(
                            oneTimePrice = model.price
                        )
                    }
                }
            }

            launch {
                PurchaseKit.subscriptionHelper.subscriptionProducts.collectLatest {
                    _state.update {
//                        val price = getBillingPrice(
//                            "weekly_without_free_trail",
//                            "paid-trail",
//                            "P1W"
//                        )

                        it.copy(
//                            weeklyPrice = "${price.price} ${price.offerPrice}",
                            monthlyPrice = getBillingPrice(subscriptionMap[0]?: "", "", "P1M").price,
                            yearlyPrice = getBillingPrice(subscriptionMap[1]?: "", "", "P1Y").price,
                        )
                    }
                }
            }

            launch {
                PurchaseKit.subscriptionHelper.isAppSubscribed.collectLatest { isSubscribed ->
                    Log.d("purchase_status", "isAppSubscribed: $isSubscribed")
                }
            }
            launch {
                PurchaseKit.oneTimePurchaseHelper.appPurchased.collectLatest { oneTimePurchased ->
                    _state.update {
                        it.copy(
                            lifetimePurchased = oneTimePurchased
                        )
                    }
                    Log.d("purchase_status", "oneTimePurchased: $oneTimePurchased")
                }
            }
            launch {
                PurchaseKit.subscriptionHelper.subscribedId.collectLatest { subscribedId ->
                    _state.update {
                        it.copy(
                            subscribedId = subscribedId
                        )
                    }
                }
            }

            launch {
                PurchaseKit.subscriptionHelper.historyFetched.collectLatest {

                    val buttonText = when {
                        state.value.subscribedId.isEmpty() -> "subscribe"
                        state.value.subscribedId == selectedId() -> "cancel subscription"
                        PurchaseKit.subscriptionHelper.isSubscriptionUpdateSupported() -> "update subscription"
                        else -> state.value.buttonText // fallback to existing text
                    }

                    _state.update {
                        it.copy(buttonText = buttonText)
                    }
                }
            }
        }
    }

    fun loadProducts(activity: Activity, list: List<String>) {
        PurchaseKit.subscriptionHelper.initBilling(activity, list)
    }


    private fun getBillingPrice(
        productId: String,
        offerId: String,
        billingPeriod: String
    ): PriceModel {
        return PurchaseKit.subscriptionHelper.getBillingPrice(productId, offerId, billingPeriod)


    }

    fun updateSelectedButtonPos(activity: Activity, selectedButtonPos: Int) {
        _state.update {
            it.copy(
                selectedButtonPos = selectedButtonPos
            )
        }
        PurchaseKit.subscriptionHelper.querySubscriptionProducts(activity)
    }

    fun purchase(activity: Activity) {
        PurchaseKit.subscriptionHelper.purchase(activity, selectedId())
    }

    fun purchaseProduct(activity: Activity) {
        PurchaseKit.oneTimePurchaseHelper.purchaseProduct(activity)
    }
}