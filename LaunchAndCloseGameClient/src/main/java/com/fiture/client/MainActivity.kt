package com.fiture.client

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fiture.commonsocket.TcpCallBack
import com.koushikdutta.async.AsyncNetworkSocket
import com.koushikdutta.async.AsyncSocket
import com.koushikdutta.async.Util
import com.koushikdutta.async.callback.CompletedCallback
import com.koushikdutta.async.callback.DataCallback
import com.koushikdutta.async.http.AsyncHttpClient
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.ref.WeakReference

@Suppress("NAME_SHADOWING", "DEPRECATION")
/**
 * 测试界面
 * 说明：socket的建立流程以目前手机端和Virgo端的`androidasync`的使用流程一样，
 * 主要模拟如何通过手机端向Virgo端发送指令执行第三方应用的启动和关闭
 */
class MainActivity : AppCompatActivity() {

    private var mAsyncSocket: AsyncSocket? = null

    private val TAG = "MainActivity"

    /**
     * 连接的服务端地址（Virgo端）
     */
    private val mHost = "10.1.3.176"
    private val mPort = 9000

    private var mTcpCallBack: TcpCallBack? = null

    private val mHandler = MyHandler(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initListener()
    }

    private fun initListener() {
        // 连接状态监听
        setTcpCallBack(object : TcpCallBack {
            override fun onConnectError(errorMsg: String?) {
                mHandler.sendEmptyMessage(MSG_CONNECT_ERROR)
            }

            override fun onConnectSucc(socket: AsyncSocket?) {
                mHandler.sendEmptyMessage(MSG_CONNECT_SUCCESS)
            }

            override fun onRevData(socket: AsyncSocket?, content: String?) {
                runOnUiThread {
                    content?.let {
                        tvState.text = it
                    }
                }
            }

            override fun onClosed(socket: AsyncSocket?) {
                runOnUiThread {
                    tvState.text = "已关闭连接"
                }
            }

            override fun onEnd(socket: AsyncSocket?) {
                runOnUiThread {
                    tvState.text = "连接结束"
                }
            }

            override fun onServerStop() {
                runOnUiThread {
                    tvState.text = "服务器停止"
                }
            }

            override fun onServerStart() {
                runOnUiThread {
                    tvState.text = "服务器启动"
                }
            }
        })
        // 连接服务器
        btnConnect.setOnClickListener {

            AsyncHttpClient.getDefaultInstance().server.connectSocket(
                mHost,
                mPort
            ) { ex, socket -> handleConnect(ex, socket) }
        }

        // 发送开启指令
        btnLaunch.setOnClickListener {
            sendData(true)
        }

        // 发送关闭指令
        btnClose.setOnClickListener {
            sendData(false)
        }
    }

    @Synchronized
    private fun handleConnect(ex: Exception?, socket: AsyncSocket?) {
        if (ex != null || socket == null) {
            mTcpCallBack?.onConnectError(if (ex != null) ex.message else "连接失败,请重试.")
            Log.d(TAG, "[TcpClient] connect fail")
            return
        }
        setSocket(socket)
        val networkSocket = socket as AsyncNetworkSocket?
        Log.d(
            TAG,
            "[TcpClient] connect success server:" + networkSocket?.remoteAddress?.hostName + ":" + networkSocket?.remoteAddress?.port
        )
        mTcpCallBack?.onConnectSucc(socket)
        socket.dataCallback = DataCallback { emitter, bb ->
            val message = String(bb.allByteArray)
            Log.d(TAG, "[TcpClient] Received Message $message")
        }
        socket.closedCallback = CompletedCallback { ex ->
            ex?.printStackTrace()
            // 连接断掉，获取当前正在请求的连接请求并坐回掉处理
            mTcpCallBack?.onClosed(socket)
            Log.d(TAG, "[TcpClient] Successfully closed connection")
        }
        socket.endCallback = CompletedCallback { ex ->
            ex?.printStackTrace()
            mTcpCallBack?.onEnd(socket)
            Log.d(TAG, "[TcpClient] Successfully end connection")
        }
    }

    private fun sendData(isOpen: Boolean) {
        if (mAsyncSocket == null) {
            toast("socket 异常")
            return
        }
        val sendDataContent = if (isOpen) {
            "1"
        } else {
            "2"
        }
        Util.writeAll(
            mAsyncSocket, sendDataContent.toByteArray()
        ) { ex ->
            if (ex != null) {
                toast("发送异常")
            } else {
                toast("发送成功")
                Log.d(
                    TAG, "[TcpClient] sendData Successfully wrote message is $sendDataContent"
                )
            }
        }
    }

    private fun toast(content: String) {
        Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
    }

    private fun setSocket(socket: AsyncSocket) {
        mAsyncSocket = socket
    }

    private fun getAsyncSocket(): AsyncSocket? {
        return mAsyncSocket
    }

    private fun setTcpCallBack(callBack: TcpCallBack) {
        mTcpCallBack = callBack
    }

    /**
     * 声明一个静态的Handler内部类，并持有外部类的弱引用
     */
    private class MyHandler(mActivity: MainActivity) : Handler() {
        private val mActivity: WeakReference<MainActivity> = WeakReference<MainActivity>(mActivity)
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val activity: MainActivity = mActivity.get() ?: return
            when (msg.what) {
                MSG_CONNECT_SUCCESS -> {
                    activity.tvState.text = "连接成功"
                }
                MSG_CONNECT_ERROR -> {
                    activity.tvState.text = "连接失败"
                }
            }
        }
    }

    companion object {
        const val MSG_CONNECT_SUCCESS = 1
        const val MSG_CONNECT_ERROR = 2
    }
}
