package com.fiture.commonsocket

import com.koushikdutta.async.AsyncSocket

/**
 *socket的连接状态接口
 */
interface TcpCallBack {
    fun onConnectError(errorMsg: String?)
    fun onConnectSucc(socket: AsyncSocket?)
    fun onRevData(socket: AsyncSocket?, content: String?)
    fun onClosed(socket: AsyncSocket?)
    fun onEnd(socket: AsyncSocket?)
    fun onServerStop()
    fun onServerStart()
}
