package cc.core.net

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Handler
import android.text.TextUtils
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit


/**
 * 网络监听观察者
 * 使用Subject来实现观察者，回调网络状态数据。
 */
@SuppressLint("StaticFieldLeak")
object RxNetworkObserver {

    var subject = PublishSubject.create<Int>()
    private var receiver: NetWorkReceiver? = null
    private var context: Context? = null
    private var handler = Handler()
    private var type = NET_STATE__MOBILE

    @Deprecated("deprecated", ReplaceWith("globalReginster(context:Context)"))
    private fun init(context: Context) {
        subject = PublishSubject.create()
        receiver =
            NetWorkReceiver(context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
        this.context = context
        context.registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        type = getNetWorkType(context)
    }

    fun globalReginster(context: Context) {
        init(context)
    }

    /**
     * 解注册
     */
    fun globalUnregister() {
        subject.onComplete()
        context?.unregisterReceiver(receiver)
    }

    /**
     * 订阅(过滤1秒内网络切换过程中状态的变化)
     */
    fun subscribe(onNext: (Int) -> Unit): Disposable? {
        val d = subject.debounce(1, TimeUnit.SECONDS).subscribe {
            handler.post { onNext(it) }
        }
        subject.onNext(type)
        context = null
        return d
    }

    /**
     * 广播接收者
     */
    class NetWorkReceiver(private var conn: ConnectivityManager) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val networkInfo = conn.activeNetworkInfo
            if (networkInfo == null) {
                type = NET_STATE_DISCONNECT
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                type = NET_STATE_WIFI
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                type = NET_STATE__MOBILE
            }
            subject.onNext(type)
        }
    }

    fun getNetWorkType(context: Context): Int {
        var netWorkType = -1
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = manager.activeNetworkInfo
        if (networkInfo != null && networkInfo!!.isConnected) {
            val typeName = networkInfo!!.typeName
            if (typeName.equals("WIFI", ignoreCase = true)) {
                netWorkType = NET_STATE_WIFI
            } else if (typeName.equals("MOBILE", ignoreCase = true)) {
                netWorkType = NET_STATE__MOBILE
            }
        } else {
            netWorkType = NET_STATE_DISCONNECT
        }
        return netWorkType
    }
}


/**
 * 网络断开连接
 */
const val NET_STATE_DISCONNECT = -1

/**
 * WIFI网络
 */
const val NET_STATE_WIFI = 0

/**
 * 移动网络
 */
const val NET_STATE__MOBILE = 1

