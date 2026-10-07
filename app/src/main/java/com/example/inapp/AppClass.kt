package com.example.inapp

import android.app.Application
import com.topedge.purchase.kit.core.utils.init.PurchaseKit
import com.topedge.purchase.kit.data.impl.RevenueCatBuilder

class AppClass : Application() {

    override fun onCreate() {
        super.onCreate()

        PurchaseKit.init(
            this,
            revenueCatBuilder = RevenueCatBuilder(
                revenueCatKey = "goog_uGnCSFTTAMJNpLlYoGCCQMNsVNd",
                offeringKey = "default_offerings"
            )
        )
    }
}
