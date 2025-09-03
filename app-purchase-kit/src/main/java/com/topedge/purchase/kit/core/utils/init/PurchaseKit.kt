package com.topedge.purchase.kit.core.utils.init

import android.content.Context
import com.topedge.purchase.kit.core.utils.InternetHelper
import com.topedge.purchase.kit.core.utils.PurchasePref
import com.topedge.purchase.kit.core.utils.purchase.OneTimePurchaseHelper
import com.topedge.purchase.kit.core.utils.purchase.SubscriptionHelper

object PurchaseKit {

    lateinit var preference: PurchasePref
        private set


    lateinit var internetHelper: InternetHelper
        private set

    lateinit var oneTimePurchaseHelper: OneTimePurchaseHelper
        private set
    lateinit var subscriptionHelper: SubscriptionHelper
        private set



    fun init(
        context: Context,
    ) {

        preference = PurchasePref.getInstance(context)
        internetHelper = InternetHelper.getInstance(context)
        oneTimePurchaseHelper = OneTimePurchaseHelper.getInstance(context)
        subscriptionHelper = SubscriptionHelper.getInstance(context)

    }
}
