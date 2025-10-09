package ip_source

import cn.iinti.malenia2.api.resource.GroovyIpResourceHandler
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import groovy.transform.BaseScript

@BaseScript GroovyIpResourceHandler _base


/**
 * 绝大部分IP下载连接是GET 的URL，
 * 如果遇到需要是POST的，则参考本demo
 * 如：{link https://goipstar.com/docs/#api-Basic-GetNewIpList}
 */

TreeMap<Long, JSONArray> cache = new TreeMap<>();

parse {
    api.http.addHeader("X-TOKEN", "xxxxx-请修改这里")

    def response = api.http.post("https://www.goipstar.com/api/getNewIpList", [
            'num'     : '50',
            'username': 'malenia_user_001',
            'password': 'malenia_pass_001'
    ])
    if (response != null) {
        cache.put(System.currentTimeMillis(), JSONObject.parseObject(response).getJSONArray("data"));
    }

    def firstEntry = cache.firstEntry()
    if (System.currentTimeMillis() - firstEntry.key > 120_000) {
        cache.remove(firstEntry.key)

        firstEntry.value.each {
            def item = it as JSONObject
            addProxyIp {
                proxyHost = item.getString("host")
                proxyPort = item.getInteger("port")
                userName = "malenia_user_001"
                password = "malenia_pass_001"
            }
        }
    }

}