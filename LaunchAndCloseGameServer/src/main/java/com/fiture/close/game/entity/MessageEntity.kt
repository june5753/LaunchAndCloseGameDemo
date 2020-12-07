package com.fiture.close.game.entity

/**
 *<pre>
 *  author : juneYang
 *  time   : 2020/12/04 6:58 PM
 *  desc   : 操作事件实体类
 *  将收到的消息json格式的转换成转成javabean
 *  version: 1.0
 *</pre>
 */
class MessageEntity {
    /**
     * 想要操作的包名
     */
    val gamePackageName = ""

    /**
     * 定义打开或关闭事件
     * 1 - 打开，2 -关闭
     */
    val event = 1
}
