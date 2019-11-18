package com.example.tsetnetwork

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import cc.core.net.NET_STATE_DISCONNECT
import cc.core.net.RxNetworkObserver
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        RxNetworkObserver().reginster(this, this)
            .subject.filter {
            it != NET_STATE_DISCONNECT
        }.debounce(1, TimeUnit.SECONDS).subscribe {
            runOnUiThread {
                tv.text = "网络类型变了，${it}"
            }
            println("网络类型变了，${it}")
        }
        tv.setOnClickListener {
            startActivity(Intent(this, Main2Activity::class.java))
        }
    }

}
