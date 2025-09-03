# In App Purchase Kit Documentation

A comprehensive Kotlin library for Android (Jetpack Compose + XML), designed to streamline monetization with support for in-app purchases.

---

## Installation

### Add Dependency

To integrate the Monetization Kit into your project, include the following in your app's `build.gradle`:

```kotlin
dependencies {
    implementation("com.github.Usman228811:app-purchase-kit:v1.0.0")
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
PurchaseKit.oneTimePurchaseHelper.initBilling("one_time_purchase_id")
```

Handle purchase state in your ViewModel:

```kotlin
viewModelScope.apply {
    launch {
        PurchaseKit.oneTimePurchaseHelper.appPurchased.collectLatest { isPurchased ->
            Log.d("ioiioo", "isPurchased: $isPurchased")
        }
    }
    launch {
        PurchaseKit.oneTimePurchaseHelper.productPriceFlow.collectLatest {
            Log.d("ioiioo", "productPriceFlow: ${it.price.ifEmpty { "..." }}")
        }
    }
}

// Trigger purchase
PurchaseKit.oneTimePurchaseHelper.purchaseProduct(activity)

// You can check if the app is purchased using PurchaseKit.
val isPurchased = PurchaseKit.preference.isAppPurchased
```

---

## Subscriptions

```kotlin
// You can check if the app is subscribed using PurchaseKit.
val isPurchased = PurchaseKit.preference.isAppPurchased

```

### ViewModel for Subscriptions

```kotlin
data class SettingScreenState(
    val weeklyPrice: String = "",
    val monthlyPrice: String = "",
    val yearlyPrice: String = "",
    val subscribedId: String = "",
    val selectedButtonPos: Int = 0,
    val buttonText: String = "subscribe"
)

class SubscriptionViewModel : ViewModel() {
    private var _state = MutableStateFlow(SettingScreenState())
    val state = _state.asStateFlow()

    private val subscriptionMap = mapOf(
        0 to "weekly_subscription2",
        1 to "monthly1_subscription",
        2 to "yearly_subscription"
    )

    private fun selectedId() = subscriptionMap[state.value.selectedButtonPos]

    init {
        viewModelScope.apply {
            launch {
                PurchaseKit.subscriptionHelper.subscriptionProducts.collectLatest {
                    _state.update {
                        it.copy(
                            weeklyPrice = getBillingPrice("weekly_subscription2", "P1W"),
                            monthlyPrice = getBillingPrice("monthly1_subscription", "P1M"),
                            yearlyPrice = getBillingPrice("yearly_subscription", "P1Y")
                        )
                    }
                }
            }
            launch {
                PurchaseKit.subscriptionHelper.subscribedId.collectLatest { subscribedId ->
                    _state.update {
                        it.copy(subscribedId = subscribedId)
                    }
                }
            }
            launch {
                PurchaseKit.subscriptionHelper.historyFetched.collectLatest {
                    val buttonText = when {
                        state.value.subscribedId.isEmpty() -> "subscribe"
                        state.value.subscribedId == selectedId() -> "cancel subscription"
                        PurchaseKit.subscriptionHelper.isSubscriptionUpdateSupported() -> "update subscription"
                        else -> state.value.buttonText
                    }
                    _state.update {
                        it.copy(buttonText = buttonText)
                    }
                }
            }
        }
    }

    fun loadProducts(activity: Activity, list: List<String>) {
        PurchaseKit.subscriptionHelper.loadProducts(activity, list)
    }

    private fun getBillingPrice(productId: String, billingPeriod: String): String {
        return PurchaseKit.subscriptionHelper.getBillingPrice(productId, billingPeriod).ifEmpty { "..." }
    }

    fun updateSelectedButtonPos(selectedButtonPos: Int) {
        _state.update {
            it.copy(selectedButtonPos = selectedButtonPos)
        }
        PurchaseKit.subscriptionHelper.querySubscriptionProducts()
    }

    fun purchase(activity: Activity) {
        PurchaseKit.subscriptionHelper.purchase(activity, selectedId())
    }
}
```

### Implementing Subscriptions

```kotlin
LaunchedEffect(Unit) {
    subscriptionViewModel.loadProducts(
        activity,
        listOf("weekly_subscription2", "monthly1_subscription", "yearly_subscription")
    )
}

// Example: Weekly subscription button
Button(
    modifier = Modifier.fillMaxWidth(),
    onClick = { subscriptionViewModel.updateSelectedButtonPos(0) }
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
