package com.example.tsetnetwork

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import cc.core.net.RxNetworkObserver

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        RxNetworkObserver.init(this)
        RxNetworkObserver.subscribe {
            println("网络类型改变${it}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        RxNetworkObserver.unregister()
    }
}
