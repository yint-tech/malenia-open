package cn.iinti.malenia2.api.resource

import cn.iinti.malenia2.api.ScriptFactory
import cn.iinti.malenia2.api.ip.resource.*
import cn.iinti.malenia2.api.tools.Api

abstract class GroovyIpResourceHandler extends Script implements IpResourceHandler {
    public Closure parseCode
    public Closure dropEventCode
    public Closure buildUpStreamUserNameCode
    public Api api


    static IpResourceHandler compileScript(
            String groovyScriptSource, Api api
    ) {
        GroovyIpResourceHandler groovyParserScript = ScriptFactory.compileScript(groovyScriptSource, GroovyIpResourceHandler.class)
        groovyParserScript.api = api
        groovyParserScript.run()
        groovyParserScript
    }

    def whenDrop(@DelegatesTo(DropEvent) Closure closure) {
        this.dropEventCode = closure
    }

    def parse(@DelegatesTo(ParseContext) Closure closure) {
        this.parseCode = closure
    }

    /**
     * 使用脚本来构建上游IP资源
     */
    def buildUpStreamUser(@DelegatesTo(AuthUserContext) Closure closure) {
        this.buildUpStreamUserNameCode = closure
    }

    /**
     * 使用ipPortPlain解析器，当前已过期,调用此方法无任何效果
     */
    @Deprecated
    def useIpPortPlainText() {

    }

    /**
     * 使用PortSpace解析器，当前已过期,调用此方法无任何效果
     */
    @Deprecated
    def usePortSpace() {

    }


    @Override
    List<ProxyIp> parse(String responseText) {
        if (parseCode == null) {
            return SmartParser.instance.parse(responseText)
        }

        List<ProxyIp> proxyIps = new LinkedList<>()
        def context = new ParseContext(proxyIps, responseText)
        def newCode = parseCode.rehydrate(context, parseCode.getOwner(), parseCode.getThisObject())
        newCode.setResolveStrategy(Closure.DELEGATE_FIRST)
        newCode.call()
        proxyIps
    }

    @Override
    void onProxyIpDrop(ProxyIp proxyIp, CountStatus countStatus, DropReason dropReason) {
        if (dropEventCode == null) {
            return
        }
        def dropEvent = new DropEvent(proxyIp, countStatus, dropReason)
        dropEventCode.rehydrate(dropEvent, dropEventCode.getOwner(), dropEventCode.getThisObject())
                .call()
    }

    @Override
    void buildAuthUser(Map<String, String> sessionParam, AuthUser.AuthUserBuilder builder) {
        if (buildUpStreamUserNameCode == null) {
            return
        }
        AuthUserContext authUserContext = new AuthUserContext(sessionParam)
        buildUpStreamUserNameCode.rehydrate(authUserContext, buildUpStreamUserNameCode.getOwner(), buildUpStreamUserNameCode.getThisObject())
                .call()

        builder.userName(authUserContext._upstream_user_name)
                .password(authUserContext._upstream_password)
    }


    static class AuthUserContext {
        private Map<String, String> sessionParam

        AuthUserContext(Map<String, String> sessionParam) {
            this.sessionParam = new HashMap<>(sessionParam)
        }

        String getSessionId() {
            return SessionParam.SESSION_ID.get(sessionParam)
        }

        String getCountry() {
            return SessionParam.COUNTRY.get(sessionParam)
        }

        String getCity() {
            return SessionParam.CITY.get(sessionParam)
        }

        String getLngLat() {
            return SessionParam.LNG_LAT.get(sessionParam)
        }

        String getInboundUser() {
            return SessionParam.INBOUND_USER.get(sessionParam)
        }

        String getOutboundUser() {
            return SessionParam.OUTBOUND_USER.get(sessionParam)
        }

        String getSessionParam(String key) {
            return sessionParam.get(key)
        }

        public String _upstream_user_name

        public String _upstream_password

        def userName(String uName) {
            _upstream_user_name = uName
        }

        def password(String password) {
            _upstream_password = password
        }
    }

    static class DropEvent {
        public ProxyIp proxyIp
        public CountStatus countStatus
        public DropReason dropReason

        DropEvent(ProxyIp proxyIp, CountStatus countStatus, DropReason dropReason) {
            this.proxyIp = proxyIp
            this.countStatus = countStatus
            this.dropReason = dropReason
        }
    }


    static class ParseContext {
        public List<ProxyIp> proxyList = new LinkedList<>()
        public String content

        ParseContext(List<ProxyIp> proxyList, String content) {
            this.proxyList = proxyList
            this.content = content
        }

        def addProxyIp(@DelegatesTo(ProxyIp) Closure closure) {
            ProxyIp proxyIp = new ProxyIp()
            closure.delegate = proxyIp
            closure.setResolveStrategy(Closure.DELEGATE_FIRST)
            closure.call()

            proxyList.add(proxyIp)
        }
    }
}
