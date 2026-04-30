# In App Purchase Kit Documentation

A comprehensive Kotlin library for Android (Jetpack Compose + XML), designed to streamline monetization with support for in-app purchases.

---

## Installation

### Add Dependency

To integrate the Monetization Kit into your project, include the following in your app's `build.gradle`:

```kotlin
dependencies {
    implementation("com.github.Usman228811:app-purchase-kit:1.1.1")
}
```

### Configure JitPack Repository

In your `settings.gradle`, add the JitPack repository:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://www.jitpack.io") }
    }
}
```

## SDK Initialization

Initialize the SDK in your `Application` class's `onCreate` method:

```kotlin
PurchaseKit.init(this)
```

---

## Premium Billing

### ViewModel for Billing

```kotlin
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
                        "isMonthlyPurchased: ${premiumState.allPurchases.contains("monthly")} "
                    )
                    Log.d(
                        TAG,
                        "isYearlyPurchased: ${premiumState.allPurchases.contains("yearly")} "
                    )


                    val monthly = PurchaseKit.premiumHelper.getBillingPrice("monthly")
                    val yearly = PurchaseKit.premiumHelper.getBillingPrice("yearly")


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
                            lifetimePurchased = premiumState.allPurchases.contains("android.test.purchased"),
                            oneTimePrice = PurchaseKit.premiumHelper.getBillingPrice("android.test.purchased").mainOfferText?: "",
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
                "Update Subscription"

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
                BillingItem.Lifetime(productId = "android.test.purchased", type = BillingItem.Type.REMOVE_ADS),
                BillingItem.Subscription(productId = "monthly", type = BillingItem.Type.REMOVE_ADS),
                BillingItem.Subscription(productId = "yearly", type = BillingItem.Type.REMOVE_ADS),
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
            productId = "android.test.purchased",
            onUserDismissedPaywall = {
                Log.d(TAG, "one-time-purchase: paywall cancelled")
            })
    }
}
```

### Implementing Subscriptions

```kotlin
LaunchedEffect(Unit) {
    subscriptionViewModel.loadProducts(
        activity,
    )
}

// Example: Weekly subscription button
Button(
    modifier = Modifier.fillMaxWidth(),
    onClick = { subscriptionViewModel.updateSelectedButtonPos(activity,0) }
) {
    Text(text = "Weekly ${state.weeklyPrice}")
}

/* 

What happens when you click the Subscribe button:

The code checks your current subscription status and proceeds accordingly:

✅ Already subscribed → Goes to the Cancel Subscription screen
❌ Not subscribed → Proceeds to Subscribe
🔄 Already subscribed but a different plan is selected → Goes to Update Subscription screen

*/
Button(
    modifier = Modifier.fillMaxWidth(),
    onClick = { subscriptionViewModel.purchase(activity) }
) {
    Text(text = state.buttonText)
}
```

---

This documentation provides a clean, organized, and visually appealing guide to using the SDK in your Android app. Each section is clearly separated, with consistent formatting and detailed explanations for seamless integration.
