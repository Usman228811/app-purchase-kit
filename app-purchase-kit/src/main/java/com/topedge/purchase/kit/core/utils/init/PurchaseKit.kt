package com.topedge.purchase.kit.core.utils.init

import android.app.Application
import com.topedge.purchase.kit.core.utils.InternetHelper
import com.topedge.purchase.kit.core.utils.PurchasePref
import com.topedge.purchase.kit.core.utils.purchase.AdKitPurchaseHelper
import com.topedge.purchase.kit.core.utils.purchase.AdKitSubscriptionHelper

object PurchaseKit {

    lateinit var preference: PurchasePref
        private set


    lateinit var internetHelper: InternetHelper
        private set

    lateinit var oneTimePurchaseHelper: AdKitPurchaseHelper
        private set
    lateinit var subscriptionHelper: AdKitSubscriptionHelper
        private set


    fun init(
        context: Application,
    ) {

        preference = PurchasePref.getInstance(context)
        internetHelper = InternetHelper.getInstance(context)
        oneTimePurchaseHelper = AdKitPurchaseHelper.getInstance(context)
        subscriptionHelper = AdKitSubscriptionHelper.getInstance(context)

    }
}
