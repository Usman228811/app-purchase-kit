package com.example.inapp

import android.app.Application
import com.topedge.purchase.kit.core.utils.init.PurchaseKit

class AppClass : Application(){

    override fun onCreate() {
        super.onCreate()

        PurchaseKit.init(this)
    }
}
