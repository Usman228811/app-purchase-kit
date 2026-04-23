package com.example.inapp

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.topedge.purchase.kit.core.utils.init.PurchaseKit
import com.topedge.purchase.kit.domain.model.OfferType
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
    val monthlyPrice: String = "",
    val yearlyPrice: String = "",
    val selectedButtonPos: Int = 0,
    val buttonText: String = "subscribe",
    val oneTimePrice: String = "",
    val lifetimePurchased: Boolean = false,
    val subscriptionPurchasesList: List<String> = emptyList()

)

class MainViewModel : ViewModel() {

    private var _state = MutableStateFlow(MainState())
    val state = _state.asStateFlow()

    private val subscriptionMap = mapOf(
        0 to "monthly",
        1 to "yearly"
    )

    companion object {
        const val TAG = "MainViewModel"
    }

    private fun selectedId() = subscriptionMap[state.value.selectedButtonPos]

    init {
        PurchaseKit.oneTimePurchaseHelper.initBilling(
            removeAdsIds = listOf("android.test.purchased"),
            featureIds = listOf()
        )
        viewModelScope.apply {
            launch {
                PurchaseKit.oneTimePurchaseHelper.oneTimePurchaseState.collectLatest { oneTimePurchaseState ->
                    Log.d(TAG, "oneTimePurchasesList: ${oneTimePurchaseState.purchasesList} ")
                    Log.d(TAG, "oneTimeOfferList: ${oneTimePurchaseState.offers} ")
                    _state.update {
                        it.copy(
                            lifetimePurchased = oneTimePurchaseState.purchasesList.contains("android.test.purchased"),
                            oneTimePrice = PurchaseKit.oneTimePurchaseHelper.getBillingPrice("android.test.purchased")
                        )
                    }
                }
            }

            launch {
                PurchaseKit.subscriptionHelper.subscriptionState.collectLatest { subscriptionState ->
                    Log.d(TAG, "subscriptionPurchasesList: ${subscriptionState.purchasesList} ")
                    Log.d(TAG, "subscriptionOffersList: ${subscriptionState.offers} ")
                    Log.d(TAG, "isMonthlyPurchased: ${subscriptionState.purchasesList.contains("monthly")} ")
                    Log.d(TAG, "isYearlyPurchased: ${subscriptionState.purchasesList.contains("yearly")} ")


                    val monthly = PurchaseKit.subscriptionHelper.getBillingPrice("monthly")
                    val yearly = PurchaseKit.subscriptionHelper.getBillingPrice("yearly")


                    when (monthly.type) {
                        OfferType.FREE_TRIAL -> {
                            Log.d(TAG, ": FREE_TRIAL")
                        }

                        OfferType.PAID_TRIAL -> {
                            Log.d(TAG, ": PAID_TRIAL")
                        }

                        OfferType.STRAIGHT -> {
                            Log.d(TAG, ": STRAIGHT")
                        }
                    }

                    Log.d(
                        TAG,
                        "mainOfferText=${monthly.mainOfferText} - period=${monthly.period} - freeTrialText=${monthly.freeTrialText} - paidTrialText=${monthly.paidTrialText}"
                    )
                    Log.d(
                        TAG,
                        "mainOfferText=${yearly.mainOfferText} - period=${yearly.period}- freeTrialText=${yearly.freeTrialText} - paidTrialText=${yearly.paidTrialText}"
                    )
                    _state.update {

                        it.copy(
                            subscriptionPurchasesList = subscriptionState.purchasesList,
                            monthlyPrice = "${monthly.mainOfferText}",
                            yearlyPrice = "${yearly.mainOfferText}",
                        )
                    }


                    changeButtonText()
                }

            }
        }
    }


    fun changeButtonText() {

        val selectedId = subscriptionMap[state.value.selectedButtonPos]
        val purchases = state.value.subscriptionPurchasesList

        val buttonText = when {
            purchases.isEmpty() -> "Subscribe"

            selectedId != null && purchases.contains(selectedId) ->
                "Cancel Subscription"

            purchases.isNotEmpty() &&
                    PurchaseKit.subscriptionHelper.isSubscriptionUpdateSupported() ->
                "Update Subscription"

            else -> state.value.buttonText
        }

        _state.update {
            it.copy(buttonText = buttonText)
        }
    }

    fun loadProducts(activity: Activity,) {
        PurchaseKit.subscriptionHelper.initBilling(activity,
            removeAdsIds = listOf("monthly", "yearly"),
            featureIds = listOf())
    }


    fun updateSelectedButtonPos(selectedButtonPos: Int) {
        _state.update {
            it.copy(
                selectedButtonPos = selectedButtonPos
            )
        }

        changeButtonText()

    }

    fun purchase(activity: Activity) {
        PurchaseKit.subscriptionHelper.purchase(
            activity,
            selectedId(),
            isForUpdatePlan = false,
            onUserDismissedPaywall = {
                Log.d(TAG, "subscription: paywall cancelled")
            })
    }

    fun purchaseProduct(activity: Activity) {
        PurchaseKit.oneTimePurchaseHelper.purchaseProduct(
            activity,
            productId = "android.test.purchased",
            onUserDismissedPaywall = {
                Log.d(TAG, "one-time-purchase: paywall cancelled")
            })
    }
}