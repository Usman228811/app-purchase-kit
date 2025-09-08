package com.example.inapp

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.topedge.purchase.kit.core.utils.init.PurchaseKit
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
    val selectedButtonPos: Int = 0,
    val isPurchased: Boolean = false,
    val price: String = "",
    val monthlyPrice: String = "",
    val yearlyPrice: String = "",
    val subscribedId: String = "",
    val buttonText: String = "",

    )

class MainViewModel() : ViewModel() {

    private val _mainState = MutableStateFlow(MainState())
    val mainState = _mainState.asStateFlow()

    private val subscriptionMap = mapOf(
        0 to "monthly",
        1 to "yearly",
    )

    private fun selectedId() = subscriptionMap[mainState.value.selectedButtonPos]

    init {
        PurchaseKit.oneTimePurchaseHelper.initBilling("one_time_purchase_id")
        collections()
    }

    fun collections() {
        viewModelScope.apply {

            launch {
                PurchaseKit.oneTimePurchaseHelper.appPurchased.collectLatest { isPurchased ->
                    _mainState.update {
                        it.copy(
                            isPurchased = isPurchased
                        )
                    }
                }
            }

            launch {
                PurchaseKit.oneTimePurchaseHelper.productPriceFlow.collectLatest { model ->
                    _mainState.update {
                        it.copy(
                            price = model.price
                        )
                    }
                }
            }

            launch {
                PurchaseKit.subscriptionHelper.subscriptionProducts.collectLatest {
                    _mainState.update {
                        it.copy(
                            monthlyPrice = getBillingPrice("monthly", "P1M"),
                            yearlyPrice = getBillingPrice("yearly", "P1Y")
                        )
                    }
                }
            }
            launch {
                PurchaseKit.subscriptionHelper.subscribedId.collectLatest { subscribedId ->
                    _mainState.update {
                        it.copy(
                            subscribedId = subscribedId
                        )
                    }
                }
            }

            launch {
                PurchaseKit.subscriptionHelper.historyFetched.collectLatest {

                    val buttonText = when {
                        mainState.value.subscribedId.isEmpty() -> "subscribe"
                        mainState.value.subscribedId == selectedId() -> "cancel subscription"
                        PurchaseKit.subscriptionHelper.isSubscriptionUpdateSupported() -> "update subscription"
                        else -> mainState.value.buttonText // fallback to existing text
                    }
                    _mainState.update {
                        it.copy(
                            buttonText = buttonText
                        )
                    }
                }
            }
        }
    }

    private fun getBillingPrice(productId: String, billingPeriod: String): String {
        return PurchaseKit.subscriptionHelper.getBillingPrice(productId, billingPeriod)
            .ifEmpty { "..." }


    }

    fun loadSubscriptionProducts(activity: Activity, list: List<String>) {
        PurchaseKit.subscriptionHelper.loadProducts(activity, list)
    }

    fun purchase(activity: Activity) {
        PurchaseKit.subscriptionHelper.purchase(activity, selectedId())
    }
}