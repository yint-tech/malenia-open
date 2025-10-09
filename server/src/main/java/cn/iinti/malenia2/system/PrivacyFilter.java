package cn.iinti.malenia2.system;

import cn.iinti.malenia2.BuildConfig;
import cn.iinti.malenia2.service.base.config.Settings;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class PrivacyFilter implements Filter {
    private static boolean isSwaggerAccess(String uri) {
        return uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-ui/");
    }

    private static boolean isActuator(String uri) {
        return uri.startsWith(BuildConfig.restfulApiPrefix + "/actuator");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String requestURI = httpServletRequest.getRequestURI();
        boolean block = Settings.blockSwagger.value && isSwaggerAccess(requestURI);
        if (!block && Settings.blockActuator.value && isActuator(requestURI)) {
            block = true;
        }
        if (!block) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        httpServletResponse.setStatus(404);
    }

    @Bean
    public FilterRegistrationBean<PrivacyFilter> privacyFilterRegistrationBean() {
        FilterRegistrationBean<PrivacyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(this);
        registration.setName("privacyFilter");
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }

}
