package cc.core.net

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Handler
import androidx.lifecycle.*
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.concurrent.TimeUnit


/**
 * 网络监听观察者
 * 使用Subject来实现观察者，回调网络状态数据。
 */
@SuppressLint("StaticFieldLeak")
class RxNetworkObserver {

    var subject = PublishSubject.create<Int>()
    private var receiver: NetWorkReceiver? = null
    private var context: Context? = null
    private var handler = Handler()
    private var type = NET_STATE__MOBILE
    private var isPaused = false

    @Deprecated("deprecated", ReplaceWith("reginster(context:Context)"))
    private fun init(context: Context) {
        subject = PublishSubject.create()
        receiver =
            NetWorkReceiver(
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager,
                this
            )
        this.context = context
        context.registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        type = getNetWorkType(context)
    }

    /**
     * 全局注册
     */
    fun reginster(context: Context, lifecycleOwner: LifecycleOwner?): RxNetworkObserver {
        init(context)
        lifecycleOwner?.lifecycle?.addObserver(obs)
        return this
    }

    private val obs = object : LifecycleObserver {

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            unregister()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun onResume() {
            isPaused = false
            subject.onNext(type)
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun onPause() {
            isPaused = true
        }

    }

    /**
     * 解注册
     */
    fun unregister() {
        subject.onComplete()
        context?.unregisterReceiver(receiver)
    }

    /**
     * 订阅(过滤1秒内网络切换过程中状态的变化)
     */
    fun subscribe(onNext: (Int) -> Unit): Disposable? {
        val d = subject.debounce(1, TimeUnit.SECONDS).subscribe {
            if (!isPaused) {
                handler.post { onNext(it) }
            }
        }
        subject.onNext(type)
        context = null
        return d
    }

    /**
     * 广播接收者
     */
    class NetWorkReceiver(private var conn: ConnectivityManager, var observer: RxNetworkObserver) :
        BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val networkInfo = conn.activeNetworkInfo
            if (networkInfo == null) {
                observer.type = NET_STATE_DISCONNECT
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                observer.type = NET_STATE_WIFI
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                observer.type = NET_STATE__MOBILE
            }
            if (!observer.isPaused) {
                observer.subject.onNext(observer.type)
            }
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

