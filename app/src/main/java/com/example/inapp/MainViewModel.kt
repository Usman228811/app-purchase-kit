package com.example.inapp

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.topedge.purchase.kit.core.utils.init.PurchaseKit
import com.topedge.purchase.kit.core.utils.purchase.BillingItem
import com.topedge.purchase.kit.domain.model.OfferType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch



val LIFE_TIME_ID = "one_time_purchase_id"
var MONTHLY_ID = "unlockphotos"
var YEARLY_ID = "duplicate_scan"

//var MONTHLY_ID = "unlockphotos:unlockphotos"
//var YEARLY_ID = "duplicate_scan:duplicate-scan-unllock"



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
        0 to MONTHLY_ID,
        1 to YEARLY_ID
    )

    companion object {
        const val TAG = "MainViewModel"
    }

    private fun selectedId() = subscriptionMap[state.value.selectedButtonPos]

    init {
        viewModelScope.apply {
            launch {
                PurchaseKit.premiumHelper.premiumState.collectLatest { premiumState ->
                    Log.d(TAG, "AllPurchasesList: ${premiumState.allPurchases} ")
                    Log.d(TAG, "subscriptionPurchasesList: ${premiumState.subscriptionPurchases} ")
                    Log.d(TAG, "LifeTimePurchasesList: ${premiumState.oneTimePurchases} ")
                    Log.d(TAG, "LifeTimeOffersList: ${premiumState.oneTimeOffers} ")
                    Log.d(TAG, "subscriptionOffersList: ${premiumState.subscriptionOffers} ")
                    Log.d(
                        TAG,
                        "isMonthlyPurchased: ${premiumState.allPurchases.contains(MONTHLY_ID)} "
                    )
                    Log.d(
                        TAG,
                        "isYearlyPurchased: ${premiumState.allPurchases.contains(YEARLY_ID)} "
                    )


                    val monthly = PurchaseKit.premiumHelper.getBillingPrice(MONTHLY_ID)
                    val yearly = PurchaseKit.premiumHelper.getBillingPrice(YEARLY_ID)


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
                            lifetimePurchased = premiumState.allPurchases.contains(LIFE_TIME_ID),
                            oneTimePrice = PurchaseKit.premiumHelper.getBillingPrice(LIFE_TIME_ID).mainOfferText?: "",
                            subscriptionPurchasesList = premiumState.subscriptionPurchases,
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
                    PurchaseKit.premiumHelper.isSubscriptionUpdateSupported() ->
                "Subscribe"

            else -> state.value.buttonText
        }

        _state.update {
            it.copy(buttonText = buttonText)
        }
    }

    fun loadProducts(activity: Activity) {
        PurchaseKit.premiumHelper.initBilling(
            activity,
            items = listOf(
                BillingItem.Lifetime(productId = LIFE_TIME_ID, type = BillingItem.Type.REMOVE_ADS),
                BillingItem.Subscription(productId = MONTHLY_ID, type = BillingItem.Type.REMOVE_ADS),
                BillingItem.Subscription(productId = YEARLY_ID, type = BillingItem.Type.REMOVE_ADS),
            ),
        )
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
        PurchaseKit.premiumHelper.purchase(
            activity,
            selectedId(),
            isForUpdatePlan = false,
            onUserDismissedPaywall = {
                Log.d(TAG, "subscription: paywall cancelled")
            })
    }

    fun purchaseProduct(activity: Activity) {
        PurchaseKit.premiumHelper.purchase(
            activity,
            productId = LIFE_TIME_ID,
            onUserDismissedPaywall = {
                Log.d(TAG, "one-time-purchase: paywall cancelled")
            })
    }
}