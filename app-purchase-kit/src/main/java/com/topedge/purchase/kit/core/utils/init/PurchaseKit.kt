package com.topedge.purchase.kit.core.utils.init

import android.app.Application
import com.topedge.purchase.kit.core.utils.InternetHelper
import com.topedge.purchase.kit.core.utils.PurchasePref
import com.topedge.purchase.kit.core.utils.purchase.AdKitPurchaseHelper
import com.topedge.purchase.kit.core.utils.purchase.AdKitSubscriptionHelper

object PurchaseKit {

    private lateinit var mContext: Application

    val preference: PurchasePref by lazy {
        PurchasePref.getInstance(mContext)
    }


    val  internetHelper: InternetHelper by lazy {
        InternetHelper.getInstance(mContext)
    }


    val oneTimePurchaseHelper: AdKitPurchaseHelper by lazy {
        AdKitPurchaseHelper.getInstance(mContext)
    }

    val subscriptionHelper: AdKitSubscriptionHelper by lazy {
        AdKitSubscriptionHelper.getInstance(mContext)
    }



    fun init(
        context: Application,
    ) {
        mContext = context

//        preference = PurchasePref.getInstance(context)
//        internetHelper = InternetHelper.getInstance(context)
//        oneTimePurchaseHelper = AdKitPurchaseHelper.getInstance(context)
//        subscriptionHelper = AdKitSubscriptionHelper.getInstance(context)

    }
}
