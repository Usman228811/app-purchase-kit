package com.example.inapp

import android.app.Activity
import android.util.Log
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

class MainViewModel() : ViewModel() {

    private var _selectedButtonPos = MutableStateFlow(0)
    val  selectedButtonPos = _selectedButtonPos.asStateFlow()
    private var _isPurchased = MutableStateFlow(false)
    val  isPurchased = _isPurchased.asStateFlow()
    private var _price = MutableStateFlow("")
    val  price = _price.asStateFlow()
    private var _monthly = MutableStateFlow("")
    val  monthly = _monthly.asStateFlow()
    private var _yearly = MutableStateFlow("")
    val  yearly = _yearly.asStateFlow()
    private var _subscribedId = MutableStateFlow("")
    val  subscribedId = _subscribedId.asStateFlow()
    private var _buttonText = MutableStateFlow("")
    val  buttonText = _buttonText.asStateFlow()

    private val subscriptionMap = mapOf(
        0 to "monthly",
        1 to "yearly",
    )

    private fun selectedId() = subscriptionMap[selectedButtonPos.value]

    init {
        PurchaseKit.oneTimePurchaseHelper.initBilling("one_time_purchase_id")
        collections()
    }

    fun collections() {
        viewModelScope.apply {

            launch {
                PurchaseKit.oneTimePurchaseHelper.appPurchased.collectLatest {
                    _isPurchased.value = it
                }
            }

            launch {
                PurchaseKit.oneTimePurchaseHelper.productPriceFlow.collectLatest { model ->
                    _price.value = model.price

                }
            }

            launch {
                PurchaseKit.subscriptionHelper.subscriptionProducts.collectLatest {

                    _monthly.value = getBillingPrice("monthly", "P1M")
                    _yearly.value = getBillingPrice("yearly", "P1Y")
                }
            }
            launch {
                PurchaseKit.subscriptionHelper.subscribedId.collectLatest { subscribedId ->
                    _subscribedId.value = subscribedId
                }
            }

            launch {
                PurchaseKit.subscriptionHelper.historyFetched.collectLatest {

                    val buttonText = when {
                        subscribedId.value.isEmpty() -> "subscribe"
                        subscribedId.value == selectedId() -> "cancel subscription"
                        PurchaseKit.subscriptionHelper.isSubscriptionUpdateSupported() -> "update subscription"
                        else -> buttonText.value // fallback to existing text
                    }
                    _buttonText.value = buttonText
                }
            }
        }
    }

    private fun getBillingPrice(productId: String, billingPeriod: String): String {
        return PurchaseKit.subscriptionHelper.getBillingPrice(productId, billingPeriod).ifEmpty { "..." }


    }

    fun loadSubscriptionProducts(activity: Activity, list: List<String>) {
        PurchaseKit.subscriptionHelper.loadProducts(activity, list)
    }

    fun purchase(activity: Activity) {
        PurchaseKit.subscriptionHelper.purchase(activity, selectedId())
    }
}