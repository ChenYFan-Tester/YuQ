package com.icecreamqaq.yuq.controller

import com.IceCreamQAQ.Yu.controller.DefaultControllerLoader
import com.IceCreamQAQ.Yu.controller.DefaultControllerLoaderImpl
import com.IceCreamQAQ.Yu.controller.router.DefaultActionInvoker
import com.IceCreamQAQ.Yu.controller.router.MethodInvoker
import com.icecreamqaq.yuq.annotation.NextContext
import com.icecreamqaq.yuq.annotation.QMsg
import java.lang.reflect.Method
import javax.inject.Inject

open class BotControllerLoader : DefaultControllerLoaderImpl() {

    override fun createMethodInvoker_(obj: Any, method: Method): MethodInvoker {
        return BotReflectMethodInvoker(method, obj)
    }

    override fun createActionInvoker_(level: Int, method: Method): DefaultActionInvoker {
        val ai = BotActionInvoker(level)
        ai.nextContext = {
            val nc = method.getAnnotation(NextContext::class.java)
            if (nc == null) null
            else NextActionContext(nc.value, nc.status)
        }()
        val qq = method.getAnnotation(QMsg::class.java) ?: return ai
        ai.reply = qq.reply
        ai.at = qq.at
        return ai
    }

}

