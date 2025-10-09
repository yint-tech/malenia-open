package cn.iinti.malenia2.service.base.metric.mql.func;

import cn.iinti.malenia2.service.base.metric.MetricVo;
import cn.iinti.malenia2.service.base.metric.mql.Context;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.TreeMap;

public abstract class XofVarFunc extends MQLFunction {
    private final String varName;

    public XofVarFunc(List<String> params) {
        super(params);
        if (params.isEmpty()) {
            throw new IllegalStateException("must has one param");
        }
        varName = params.get(0);
    }

    @Override
    public Context.MQLVar call(Context context) {
        Context.MQLVar var = context.getVariables().get(varName);
        if (var == null) {
            throw new IllegalStateException("no var : " + varName);
        }

        TreeMap<String, List<MetricVo>> ret = Maps.newTreeMap();
        var.data.forEach((s, metricVos) -> ret.put(s, apply(metricVos)));
        return Context.MQLVar.newVar(ret);
    }

    protected abstract List<MetricVo> apply(List<MetricVo> metricVos);
}
