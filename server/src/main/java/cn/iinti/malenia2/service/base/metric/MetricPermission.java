//package cn.iinti.malenia2.service.base.metric;
//
//import cn.iinti.malenia2.entity.metric.Metric;
//import cn.iinti.malenia2.entity.metric.MetricDay;
//import cn.iinti.malenia2.service.base.perm.Permission;
//import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
//import com.google.common.base.Function;
//import com.google.common.collect.HashMultimap;
//import com.google.common.collect.Multimap;
//import org.springframework.stereotype.Component;
//
//import java.util.Collection;
//import java.util.HashSet;
//import java.util.Set;
//import java.util.function.Consumer;
//
///**
// * 控制用户查看指标的权限,
// * 只是一个demo，实际上不应该让普通用户有查看指标的权限
// */
//@Component
//public class MetricPermission extends Permission<Metric> {
//    private final Set<String> categoryRegistry = new HashSet<>();
//    private final Multimap<String, String> metricMapRegistry = HashMultimap.create();
//
//    public MetricPermission() {
//        super(Metric.class);
//        addDefault("订单模块", "malenia.order.create");
//        addDefault("订单模块", "malenia.order.cancel");
//
//        addDefault("用户模块", "malenia.user.login");
//        addDefault("用户模块", "malenia.user.register");
//    }
//
//    private void addDefault(String category, String metricName) {
//        categoryRegistry.add(category);
//        metricMapRegistry.put(category, metricName);
//    }
//
//    @Override
//    public String scope() {
//        return "metric";
//    }
//
//    @Override
//    public Collection<String> perms() {
//        return categoryRegistry;
//    }
//
//    @Override
//    public void filter(Collection<String> perms, QueryWrapper<Metric> sql) {
//        if (perms.isEmpty()) {
//            sql.eq(MetricDay.ID, -1);
//            return;
//        }
//        Set<String> hasPermsMetrics = new HashSet<>();
//        for (String perm : perms) {
//            Collection<String> metricNames = metricMapRegistry.get(perm);
//            hasPermsMetrics.addAll(metricNames);
//        }
//        if (hasPermsMetrics.isEmpty()) {
//            sql.eq(MetricDay.ID, -1);
//            return;
//        }
//
//        sql.and(metricQueryWrapper -> metricQueryWrapper.in(Metric.NAME, hasPermsMetrics));
//    }
//
//
//    @Override
//    public boolean hasPermission(Collection<String> perms, Metric metric) {
//        return perms.stream().anyMatch(s -> metric.getName().startsWith(s));
//    }
//}
