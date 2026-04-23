# In App Purchase Kit Documentation

A comprehensive Kotlin library for Android (Jetpack Compose + XML), designed to streamline monetization with support for in-app purchases.

---

## Installation

### Add Dependency

To integrate the Monetization Kit into your project, include the following in your app's `build.gradle`:

```kotlin
dependencies {
    implementation("com.github.Usman228811:app-purchase-kit:1.0.9")
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

## One-Time Purchase

Initialize billing in your splash screen:

```kotlin
 PurchaseKit.oneTimePurchaseHelper.initBilling(
            removeAdsIds = listOf("android.test.purchased"),
            featureIds = listOf()
        )
```

Handle purchase state in your ViewModel:

```kotlin
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
}

// Trigger purchase
PurchaseKit.oneTimePurchaseHelper.purchaseProduct(activity, "android.test.purchased", onUserDismissedPaywall = {
            Log.d("purchase_status", "one-time-purchase: paywall cancelled")
        })

// You can check if any purchased is done using PurchaseKit.
val isPurchased = PurchaseKit.preference.isAppPurchased


// You can check if the oneTime/LifeTime purhchased is done using PurchaseKit.
val isLifeTimePurchased = PurchaseKit.preference.isLifeTimePurchased
```

---

## Subscriptions

```kotlin
// You can check if the app is subscribed using PurchaseKit.
val isAppSubscribed = PurchaseKit.preference.isAppSubscribed


// You can check if any purchased is done using PurchaseKit.
val isPurchased = PurchaseKit.preference.isAppPurchased


```

### ViewModel for Subscriptions

```kotlin
data class MainState(
    val monthlyPrice: String = "",
    val yearlyPrice: String = "",
    val selectedButtonPos: Int = 0,
    val buttonText: String = "subscribe",
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

        // importatnt parameter "isForUpdatePlan"
		// Pass true → update existing subscription
		// Pass false → start a new subscription

        PurchaseKit.subscriptionHelper.purchase(
            activity,
            selectedId(),
            isForUpdatePlan = false,
            onUserDismissedPaywall = {
                Log.d(TAG, "subscription: paywall cancelled")
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

This documentation provides a clean, organized, and visually appealing guide to using the SDK in your Android app. Each section is clearly separated, with consistent formatting and detailed explanations for seamless integration. Kotlin code blocks are now explicitly marked with triple backticks and the `kotlin` language identifier.
