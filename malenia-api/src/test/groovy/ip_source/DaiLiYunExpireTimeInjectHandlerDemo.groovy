package ip_source

import cn.iinti.malenia2.api.resource.GroovyIpResourceHandler
import com.google.common.base.Splitter
import groovy.transform.BaseScript

/**
 * 此行代码可以让编译器：idea 提供代码提示，语法检查等功能。
 * 如果您在idea中编辑脚本，建议保留此行
 */
@BaseScript GroovyIpResourceHandler _base


/**
 * 正常情况malenia不需要直到代理ip的过期时间，但是如果某些特殊的场景需要设定ip过期时间，
 * 则参考本事例，设置过期时间:expireTime
 */

parse {
    // content格式
    // "183.165.128.141:57114,183.165.128.141,中国-安徽-淮南--电信,1650093710,1650094010\n" +
    // "183.165.128.141:57114,183.165.128.141,中国-安徽-淮南--电信,1650093710,1650094010"
    //
    content.toString().split("\n").split {
        addProxyIp {
            def strings = Splitter.on(",").splitToList(it.toString())
            def hostPort = strings.get(0).split(":");
            proxyHost = hostPort[0]
            proxyPort = Integer.parseInt(hostPort[1])
            // 请注意，如果你在代理资源整体配置过账户密码，那么这里可以不用为单个代理资源陪配置密码
            setUserName null
            setPassword null
            expireTime = Long.parseLong(strings.get(3))
        }
    }
}