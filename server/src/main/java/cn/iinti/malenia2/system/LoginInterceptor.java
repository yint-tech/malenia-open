package cn.iinti.malenia2.system;


import cn.iinti.malenia2.BuildConfig;
import cn.iinti.malenia2.entity.CommonRes;
import cn.iinti.malenia2.entity.UserInfo;
import cn.iinti.malenia2.service.base.UserInfoService;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;


@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Resource
    private UserInfoService userInfoService;

    private static final byte[] needLoginResponse = JSONObject.toJSONString(CommonRes.failed("请登录后访问")).getBytes(Charsets.UTF_8);
    private static final byte[] loginExpire = JSONObject.toJSONString(CommonRes.failed("请重新登录")).getBytes(Charsets.UTF_8);
    private static final byte[] onlyForAdminResponse = JSONObject.toJSONString(CommonRes.failed("非管理员")).getBytes(Charsets.UTF_8);

    private List<String> collectTokenList(HttpServletRequest request) {
        List<String> tokenList = Lists.newArrayList();

        // header 不区分大小写
        String operatorToken = request.getHeader(BuildConfig.userLoginTokenKey);
        if (StringUtils.isNotBlank(operatorToken)) {
            tokenList.add(operatorToken);
        }
        operatorToken = request.getParameter(BuildConfig.userLoginTokenKey);
        if (StringUtils.isNotBlank(operatorToken)) {
            tokenList.add(operatorToken);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(BuildConfig.userLoginTokenKey)) {
                    tokenList.add(cookie.getValue());
                }
            }
        }
        return tokenList.stream()
                .filter(s -> StringUtils.isNotBlank(s) &&
                        // undefined,null as blank from frontend javascript
                        !StringUtils.containsAny(s, "undefined", "null"))
                .toList();
    }

    private boolean handleNoToken(LoginRequired loginRequired, HttpServletResponse response) throws IOException {
        // 不存在鉴权token
        if (loginRequired == null) {
            //不需要登陆
            return true;
        }
        // 需要登录，但是没有token
        response.addHeader("content-type", "application/json; charset=utf-8");
        response.getOutputStream().write(needLoginResponse);
        return false;
    }

    @Override
    public boolean preHandle(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        if (request == null || response == null) {
            return true;
        }

        Method method = ((HandlerMethod) handler).getMethod();
        LoginRequired loginRequired = method.getAnnotation(LoginRequired.class);
        List<String> tokenList = collectTokenList(request);
        if (tokenList.isEmpty()) {
            return handleNoToken(loginRequired, response);
        }

        CommonRes<UserInfo> result = userInfoService.checkLogin(tokenList);
        if (loginRequired == null) {
            // no need login
            if (result.isOk()) {
                // but user send userToken
                AppContext.setUser(result.getData());
            }
            return true;
        }

        if (!result.isOk()) {
            //如果这个接口允许api token访问，那么直接允许，并且使用对应的token所在账户身份
            if (loginRequired.apiToken()) {
                result = userInfoService.checkAPIToken(tokenList);
            }
            if (!result.isOk()) {
                response.addHeader("content-type", "application/json; charset=utf-8");
                response.getOutputStream().write(loginExpire);
                return false;
            }
            AppContext.markApiUser();
        }
        if (loginRequired.forAdmin() && BooleanUtils.isNotTrue(result.getData().getIsAdmin())) {
            response.addHeader("content-type", "application/json; charset=utf-8");
            response.getOutputStream().write(onlyForAdminResponse);
            return false;
        }
        AppContext.setUser(result.getData());
        AppContext.setLoginAnnotation(loginRequired);
        return true;
    }

    @Override
    public void postHandle(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Object handler, ModelAndView modelAndView) {
        AppContext.removeUser();
    }
}