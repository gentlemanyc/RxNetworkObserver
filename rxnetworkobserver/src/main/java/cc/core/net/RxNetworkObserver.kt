package cc.core.net

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Handler
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit


/**
 * 网络监听观察者
 * 使用Subject来实现观察者，回调网络状态数据。
 */
object RxNetworkObserver {

    lateinit var subject: PublishSubject<Int>
    private var receiver: NetWorkReceiver? = null
    private lateinit var reference: WeakReference<Context>
    private var handler = Handler()
    fun init(context: Context) {
        subject = PublishSubject.create()
        receiver =
            NetWorkReceiver(context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
        reference = WeakReference(context)
        context.registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    /**
     * 解注册
     */
    fun unregister() {
        subject.onComplete()
        reference.get()?.unregisterReceiver(receiver)
    }

    /**
     * 订阅(过滤1秒内网络切换过程中状态的变化)
     */
    fun subscribe(onNext: (Int) -> Unit): Disposable? {
        return subject.debounce(1, TimeUnit.SECONDS).subscribe {
            handler.post { onNext(it) }
        }
    }

    /**
     * 广播接收者
     */
    class NetWorkReceiver(private var conn: ConnectivityManager) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val networkInfo = conn.activeNetworkInfo
            if (networkInfo == null) {
                RxNetworkObserver.subject.onNext(NET_STATE_DISCONNECT)
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                RxNetworkObserver.subject.onNext(NET_STATE_WIFI)
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                RxNetworkObserver.subject.onNext(NET_STATE__MOBILE)
            }
        }
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

