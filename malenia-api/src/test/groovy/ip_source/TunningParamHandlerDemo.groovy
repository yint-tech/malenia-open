package ip_source

import cn.iinti.malenia2.api.resource.GroovyIpResourceHandler
import groovy.transform.BaseScript
import org.apache.commons.lang3.StringUtils

/**
 * 此行代码可以让编译器：idea 提供代码提示，语法检查等功能。
 * 如果您在idea中编辑脚本，建议保留此行
 */
@BaseScript GroovyIpResourceHandler _base

buildUpStreamUser {
    // 获取用户zone参数，并使用zone1兜底
    def zone = StringUtils.defaultString(getSessionParam("zone"), "zone1")
    // 获取用户country参数，并使用us兜底
    def country = StringUtils.defaultString(getSessionParam("country"), "us")
    // malenia为所有用户创建了sessionId
    def session = sessionId

    // 下面两行代码等价
    //  userName getInboundUser() + "-zone-" + zone + "-country-" + country + "-session-" + session
    userName "${inboundUser}-zone-${zone}-country-${country}-session-${session}"
}