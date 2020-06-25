package com.icecreamqaq.yuq.controller

import com.IceCreamQAQ.Yu.annotation.Action
import com.IceCreamQAQ.Yu.annotation.Before
import com.IceCreamQAQ.Yu.controller.NewActionContext
import com.IceCreamQAQ.Yu.controller.router.NewMethodInvoker
import com.IceCreamQAQ.Yu.controller.router.NewRouter
import com.IceCreamQAQ.Yu.di.YuContext
import com.IceCreamQAQ.Yu.loader.LoadItem
import com.icecreamqaq.yuq.annotation.ContextTip
import com.icecreamqaq.yuq.annotation.ContextTips
import java.lang.RuntimeException
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class ContextRouter {

    val routers: MutableMap<String, ContextAction> = ConcurrentHashMap()

    fun invoke(path: String, context: NewActionContext) = this.routers[path]?.invoker?.invoke("", context) ?: false
}

data class ContextAction(val invoker: NewRouter, val tips: Map<Int, String>)

class BotContextControllerLoader : BotControllerLoader() {

    @Inject
    private lateinit var context: YuContext

    override fun load(items: Map<String, LoadItem>) {
        val router = ContextRouter()
        for (item in items.values) {
            val instance = context[item.type] ?: continue
            controllerToRouter(instance, router)
        }
        context.putBean(ContextRouter::class.java, "", router)
    }

    private fun controllerToRouter(instance: Any, rootRouter: ContextRouter) {
        val controllerClass = instance::class.java

        val methods = controllerClass.methods
        val befores = ArrayList<NewMethodInvoker>()
        for (method in methods) {
            val before = method.getAnnotation(Before::class.java)
            if (before != null) {
                val beforeInvoker = createMethodInvoker(instance, method)
                befores.add(beforeInvoker)
            }
        }
        val before = befores.toTypedArray()
        for (method in methods) {
            val action = method.getAnnotation(Action::class.java)
            if (action != null) {
                val path: String = action.value

                val methodInvoker = createMethodInvoker(instance, method)
                val actionInvoker = createActionInvoker(1, method)

                actionInvoker.invoker = methodInvoker
                actionInvoker.befores = before

                val tip = HashMap<Int, String>()
                val tips = method.getAnnotationsByType(ContextTip::class.java)
                        ?: method.getAnnotation(ContextTips::class.java)?.value
                if (tips != null) {
                    for (contextTip in tips) {
                        tip[contextTip.status] = contextTip.value
                    }
                }

                rootRouter.routers[path] = ContextAction(actionInvoker, tip)
            }
        }
    }

}

data class ContextSession(val id: String, private val saves: MutableMap<String, Any> = ConcurrentHashMap()) {

    var context: String? = null

    operator fun get(name: String) = saves[name]
    operator fun set(name: String, value: Any) {
        saves[name] = value
    }

}

data class NextActionContext(val router: String, val status: Int = 0) : RuntimeException()