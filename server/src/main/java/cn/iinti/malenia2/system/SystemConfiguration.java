package cn.iinti.malenia2.system;

import cn.iinti.malenia2.BuildConfig;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SystemConfiguration {
    @Bean
    public PaginationInnerInterceptor paginationInterceptor() {
        return new PaginationInnerInterceptor();
    }

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Malenia")
                        .description("malenia系统")
                        .version("v" + BuildConfig.versionName)
                        .license(new License().name("Apache 2.0")
                                .url("http://malenia.iinti.cn/malenia-doc")))
                .externalDocs(new ExternalDocumentation()
                        .description("iinti")
                        .url("https://iinti.cn"));
    }

    @Bean
    public OperationCustomizer apiCustomizer() {
        return (operation, handlerMethod) -> {
            LoginRequired loginRequired = handlerMethod.getMethod().getAnnotation(LoginRequired.class);
            if (loginRequired == null) {
                return operation;
            }
            List<String> newSummary = new ArrayList<>();
            if (loginRequired.forAdmin()) {
                newSummary.add("AdminOnly");
            }
            if (loginRequired.apiToken()) {
                newSummary.add("SupportApiToken");
            }
            String summary = operation.getSummary();
            if (StringUtils.isNotBlank(summary)) {
                newSummary.add(summary);
            }
            operation.setSummary(StringUtils.join(newSummary, " "));
            Parameter parameter = new QueryParameter()
                    .name(BuildConfig.userLoginTokenKey)
                    .description("接口Token")
                    .required(true)
                    .allowEmptyValue(false);
            operation.addParametersItem(parameter);

            return operation;
        };
    }
}