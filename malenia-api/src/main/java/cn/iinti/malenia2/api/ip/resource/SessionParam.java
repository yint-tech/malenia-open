package cn.iinti.malenia2.api.ip.resource;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum SessionParam {
    INBOUND_USER("inbound_user", "入栈用户"),
    OUTBOUND_USER("outbound_user", "出栈用户"),
    OUTBOUND_PREFER_IP_SOURCE("outbound_prefer_ip_source", "偏好ip源"),
    USER_TRACE_ID("trace_id", "traceId,给框架调试bug使用"),


    /////////////////////// 相关标准指令 ///////////////////////
    SESSION_ID("session_id", "会话id，相同会话可以代表相对稳定出口关系"),
    COUNTRY("country", "国家和地区,如中国:cn"),
    CITY("city", "城市,请注意当指定城市时，必须确保传递了国家字段"),
    LNG_LAT("lng_lat", "经纬度，格式:(经度_纬度)");


    @Getter
    private final String key;
    @Getter
    private final String desc;

    SessionParam(String key, String desc) {
        this.key = key;
        this.desc = desc;
    }

    public static final Splitter slashSplitter = Splitter.on('_').omitEmptyStrings().trimResults();

    public static ConcurrentHashMap<String, String> parseUserParam(String input) {
        ConcurrentHashMap<String, String> sessionParam = new ConcurrentHashMap<>();
        if (StringUtils.isBlank(input)) {
            return sessionParam;
        }

        List<String> segment = Lists.newArrayList();
        StringBuilder sb = new StringBuilder();
        char[] chars = input.trim().toCharArray();
        for (int i = 1; i < chars.length; i++) {
            char pre = chars[i - 1];
            char ch = chars[i];
            if (pre == '\\') {
                // 转义处理
                if (ch == '-' || ch == '\\') {
                    continue;
                }
            }
            sb.append(pre);

            if (ch == '-') {
                segment.add(sb.toString());
                sb = new StringBuilder();
                i++;
            } else if (i == chars.length - 1) {
                sb.append(ch);
                segment.add(sb.toString());
            }
        }

        String userName = segment.get(0);
        for (int i = 1; i < segment.size(); i += 2) {
            sessionParam.put(segment.get(i), segment.get(i + 1));
        }
        INBOUND_USER.set(sessionParam, userName);
        return sessionParam;
    }

    public String get(Map<String, String> params) {
        return params.get(key);
    }

    public void set(Map<String, String> param, String value) {
        if (value == null) {
            return;
        }
        param.put(key, value);
    }

    public static void main(String[] args) {
        System.out.println(parseUserParam("iinti-session_id-123456-country-cn"));
        System.out.println(parseUserParam("iinti-session_id-123456-lng_lat-116.457_95.444"));
        System.out.println(parseUserParam("iinti-session_id-123456-lng_lat-116.457_\\-95.444"));
    }
}
