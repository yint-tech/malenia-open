/**
 * 本模块处理注入目标作用域匹配模块，
 * 首先本模块的设计理念和nginx不相同，在mitm工作流程中，mitm决定是否需要开始工作之前能拿到的数据只有host字段，-> 包名目标网站的域名（也可能是纯ip）：端口
 * 一旦mitm开始工作，我们就会对ssl流量进行解密，此时就开始对流量有了侵入。
 *
 * 而ng来说，他的工作场景主要是考虑不同url的path的路由规则，ng也不需要考虑流量解密问题。所以我们可以看到ng大量关于uri pattern的功能支持。
 * 相对来说，我们的框架不会扩展uri pattern，因为我们在groovy脚本中可以直接写代码来判定任何url的规则。
 *
 * 但是对于host:port 的匹配，我们这里则是深度支持（虽然他的规则很简单），并且匹配的控制只能由框架来执行而不能给用户扩展。这是因为malenia系统面临的并发是单节点5k的设计，
 * 一旦让用户自己来决定每个链接是否需要进入mitm，则可能由于用户自己的不可靠代码影响所有链接。
 *
 * 我们提供一套表达式来描述注入目标匹配规则，他的格式如下：
 *
 * wildcard domain:portExp | repeatExp
 * 请注意portExp可以省略，省略portExp代表嗅探所有端口
 *
 * demo1: 嗅探百度https:         www.baidu.com:443
 * demo2: 嗅探百度https和http:   www.baidu.com:443,80
 * demo3: 嗅探百度的所有网站:     *.baidu.com
 * demo4: 嗅探新浪的微博和门户:   *.weibo.com|*.sina.com
 */
package cn.iinti.malenia2.api.mitm.interceptor;


class ExpSegment {
    /**
     * 空集合代表所有
     */
    Set<Integer> ports
    String domain
    boolean wildcard

    ExpSegment(Set<Integer> ports, String domain, boolean wildcard) {
        this.ports = ports
        this.domain = domain
        this.wildcard = wildcard
    }

    boolean match(String host, int port) {
        if (!ports.isEmpty() && !ports.contains(port)) {
            return false
        }

        if (host == domain) {
            return true
        }

        if (wildcard && host.endsWith(domain)) {
            return true
        }
        false
    }

    static ExpSegment parseSegment(String segmentStr) {
        boolean wildcard = false
        Set<Integer> ports = new HashSet<>()

        if (segmentStr.startsWith("*")) {
            wildcard = true
            segmentStr = segmentStr.substring(1)
        }
        int portIndex = segmentStr.indexOf(":")
        if (portIndex > 0) {
            segmentStr.substring(portIndex + 1).split(",").split {
                ports.add(Integer.parseInt(
                        it.trim()
                ))
            }
            segmentStr = segmentStr.substring(0, portIndex).trim()
        }
        new ExpSegment(ports, segmentStr, wildcard)
    }

    static List<ExpSegment> parseExp(String exp) {
        List<ExpSegment> ret = new LinkedList<>()
        exp.split("\\|").split {
            ret.add(parseSegment(it.trim()))
        }
        ret
    }
}
