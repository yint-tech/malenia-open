package cn.iinti.malenia2.api.mitm.interceptor

import cn.iinti.malenia2.api.mitm.http.HttpRequest
import cn.iinti.malenia2.api.mitm.http.HttpResponse


class Interceptor {
    private Closure<?> requestClosure
    private Closure<?> responseClosure
    private Config config = new Config()

    void request(HttpRequest request) {
        if (requestClosure == null) {
            // 没有设置拦截器
            return
        }
        // 这里我们扩充delegate，可以添加一些变量到对应作用域下面，这样可以方便我们快速访问资源
        // 我们直接把request解构到当前作用域，
        requestClosure
                .rehydrate(request, requestClosure.owner, requestClosure.thisObject)
                .call()
    }

    void response(HttpResponse response) {
        if (responseClosure == null) {
            return
        }
        responseClosure
                .rehydrate(response, responseClosure.owner, responseClosure.thisObject)
                .call()
    }

    Config getConfig() {
        config
    }


    def request(@DelegatesTo(HttpRequest) Closure closure) {
        this.requestClosure = closure
    }

    def response(@DelegatesTo(HttpResponse) Closure closure) {
        this.responseClosure = closure
    }

    def config(@DelegatesTo(Config) Closure closure) {
        closure.delegate = config
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call()
    }
}