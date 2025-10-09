package cn.iinti.malenia2.service.proxy.dbconfigs;

import cn.iinti.malenia2.api.mitm.interceptor.GroovyMitmScript;
import cn.iinti.malenia2.entity.MitmScript;
import cn.iinti.malenia2.service.proxy.core.mitm.api.ApiImpl;
import cn.iinti.malenia2.utils.Md5Utils;
import lombok.experimental.Delegate;

public class WrapperMitmScript {
    private String contentMd5;

    @Delegate
    private GroovyMitmScript groovyMitmScript;

    @Delegate
    private MitmScript mitmScript;

    public WrapperMitmScript reload(MitmScript mitmScript) {
        String md5 = Md5Utils.md5Hex(mitmScript.getContent());
        if (md5.equals(contentMd5)) {
            return this;
        }
        this.mitmScript = mitmScript;
        groovyMitmScript = GroovyMitmScript.compileScript(mitmScript.getContent(), new ApiImpl(mitmScript.getUser()));
        contentMd5 = md5;
        return this;
    }
}
