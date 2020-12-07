package com.fiture.close.game.server

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fiture.close.game.server.utils.ApkUtils
import com.fiture.close.game.server.utils.RootCmd
import com.koushikdutta.async.AsyncNetworkSocket
import com.koushikdutta.async.AsyncServer
import com.koushikdutta.async.AsyncServerSocket
import com.koushikdutta.async.AsyncSocket
import com.koushikdutta.async.callback.DataCallback
import com.koushikdutta.async.callback.ListenCallback
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private val mPort = 9000
    private var mServer: AsyncServerSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStartServer.setOnClickListener {
            startServer()
            toast("启动服务")
        }
    }

    private fun toast(content: String) {
        Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
    }

    @Synchronized
    fun startServer() {
        setup()
    }

    private fun setup() {
        mServer = AsyncServer.getDefault().listen(
            null, mPort,
            object : ListenCallback {
                override fun onAccepted(socket: AsyncSocket) {
                    handleAccept(socket)
                }

                override fun onListening(socket: AsyncServerSocket) {
                    Log.d(TAG, "TcpServer started listening for connections")
                    Log.i(TAG, "------------startServer--------onListening--------------------")
                    tvState.text = "监听中……"
                }

                override fun onCompleted(ex: Exception) {
                    Log.i(TAG, "------------startServer--------onCompleted--------------------")
                    mServer = null
                }
            }
        )
    }

    // 得到当前连接的客户端信息
    private fun getSocketName(socket: AsyncSocket): String? {
        val networkSocket = socket as AsyncNetworkSocket?
        return if (socket == null || networkSocket!!.remoteAddress == null) {
            null
        } else networkSocket.remoteAddress.hostName + ":" + networkSocket.remoteAddress.port
    }

    private fun handleAccept(socket: AsyncSocket) {
        Log.d(TAG, "New Connection " + getSocketName(socket))

        // 收到消息
        socket.dataCallback = DataCallback { emitter, bb ->
            val message = String(bb!!.allByteArray)
            val socketName: String? = getSocketName(socket)
            Log.d(TAG, "Received Message from:$socketName message: $message")
            tvReceiveMessage.text = "收到消息:$message"
            // 向手机端发送当前安装的包名，或者手机直接发送后，收到消息后再判断是否有此包名
            val pkgName = "com.microsoft.fluentuidemo"
            if (!ApkUtils.isInstalled(this, pkgName)) {
                runOnUiThread {
                    toast("当前应用未安装")
                }
                return@DataCallback
            }

            // TODO:打开消息事件和包名封装成javaBean
            if (message == "1") {
                if (ApkUtils.isTopActivity(this, pkgName)) {
                    runOnUiThread {
                        toast("程序已启动")
                    }
                    return@DataCallback
                }
                // 启动程序
                startUpApplication(this, pkgName)
            } else {
                // 关闭程序 root权限
                val commend = "am force-stop $pkgName"
                RootCmd.execRootCmd(commend)
            }
        }
    }

    /**
     *  启动应用程序
     */
    private fun startUpApplication(mContext: Context, pkg: String) {
        val packageManager = mContext.packageManager
        var packageInfo: PackageInfo? = null
        packageInfo = try {
            // 获取指定包名的应用程序的PackageInfo实例
            packageManager.getPackageInfo(pkg, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            // 未找到指定包名的应用程序
            e.printStackTrace()
            toast("未找到程序")
            return
        }
        if (packageInfo != null) {
            // 已安装应用
            val resolveIntent = Intent(Intent.ACTION_MAIN, null)
            resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            resolveIntent.setPackage(packageInfo.packageName)
            val apps = packageManager.queryIntentActivities(
                resolveIntent, 0
            )
            var ri: ResolveInfo? = null
            ri = try {
                apps.iterator().next()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                return
            }
            if (ri != null) {
                // 获取应用程序对应的启动Activity类名
                val className = ri.activityInfo.name
                // 启动应用程序对应的Activity
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                val componentName = ComponentName(pkg, className)
                intent.component = componentName
                mContext.startActivity(intent)
            }
        }
    }

    // TODO: 未执行成功
    private fun forceStopApp(mContext: Context, packageName: String) {
        @SuppressLint("ServiceCast") val mActivityManager: ActivityManager = getSystemService(
            ACTIVITY_SERVICE
        ) as ActivityManager
        try {
            val method = Class.forName("android.app.ActivityManager").getMethod(
                "forceStopPackage",
                String::class.java
            )
            method.invoke(mActivityManager, packageName)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}
