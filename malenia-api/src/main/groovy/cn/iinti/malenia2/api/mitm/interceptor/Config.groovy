package cn.iinti.malenia2.api.mitm.interceptor

import com.google.common.collect.Lists
import com.google.common.collect.Sets

class Config {
    // 当前请求直接经过malenia连接，不经过上级代理
    boolean directSend
    // 当你的mitm脚本处理耗时任务卡住线程导致线程资源不足的时候，有两种策略：
    // 1. 中断请求 2.取消mitm注入直接转发。默认策略为取消mitm直接转发
    boolean abortWhenSubmitFailed

    /**
     * 禁用http2.0
     */
    boolean disableH2
    /**
     * 监控目标
     */
    List<ExpSegment> segments = Lists.newLinkedList()

    /**
     * 监控产品，为空代表监控所有产品
     */
    Set<String> products = Sets.newHashSet()


    boolean match(String host, int port, String product) {
        if (products != null && !products.isEmpty() && !products.contains(product)) {
            // 当前监听器针对于特定产品生效
            return false
        }

        for (ExpSegment expSegment : segments) {
            if (expSegment.match(host, port)) {
                return true
            }
        }

        false
    }

    boolean valid() {
        !segments.isEmpty()
    }

    def sniff(String exp) {
        segments.addAll(ExpSegment.parseExp(exp))
    }

    def product(String product) {
        product.split(",").split {
            products.add(it.trim())
        }
    }


}