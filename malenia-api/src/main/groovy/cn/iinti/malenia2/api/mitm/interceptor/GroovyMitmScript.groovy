package cn.iinti.malenia2.api.mitm.interceptor


import cn.iinti.malenia2.api.tools.Api
import cn.iinti.malenia2.api.ScriptFactory
import com.google.common.collect.Lists
import com.google.common.collect.Maps

/**
 * 编译脚本代码，转化为拦截器对象
 */
abstract class GroovyMitmScript extends Script {
    // fast cache
    private final Map<String, Interceptor> cache = Maps.newConcurrentMap()
    private List<Interceptor> interceptors = Lists.newArrayList()
    public Api api

    static GroovyMitmScript compileScript(String groovyScriptSource, Api api) {
        GroovyMitmScript mitmScript = ScriptFactory.compileScript(groovyScriptSource, GroovyMitmScript.class)
        mitmScript.api = api
        mitmScript.run()
        mitmScript
    }

    def intercept(@DelegatesTo(Interceptor) Closure closure) {
        Interceptor interceptor = new Interceptor()
        closure.delegate = interceptor
        closure.setResolveStrategy(Closure.DELEGATE_FIRST)
        closure.call()
        if (interceptor.config.valid()) {
            interceptors.add(interceptor)
        } else {
            throw new IllegalStateException("no target sniffer defined!!")
        }
    }

    Interceptor findInterceptor(String host, int port, String product) {
        String key = host + ":" + port + "#" + product;
        //一个脚本能够命中的对象理论上应该不超过20个，所以这里可以直接缓存，并且不需要考虑缓存被撑爆
        Interceptor cacheHolder = cache.get(key);
        if (cacheHolder != null) {
            return cacheHolder;
        }
        for (Interceptor interceptor : interceptors) {
            if (interceptor.getConfig().match(host, port, product)) {
                cache.put(key, interceptor);
                return interceptor;
            }
        }
        null
    }
}






