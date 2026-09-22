package com.tally.core.kernel.config;

import com.tally.core.kernel.web.RequestContextMdcFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers {@link RequestContextMdcFilter} ahead of every other filter, so that even a request the
 * {@code PortScopeFilter} rejects is logged with a trace id (docs/02 §7.6).
 */
@Configuration(proxyBeanMethods = false)
public class RequestContextConfig {

    @Bean
    FilterRegistrationBean<RequestContextMdcFilter> requestContextMdcFilter() {
        FilterRegistrationBean<RequestContextMdcFilter> registration =
                new FilterRegistrationBean<>(new RequestContextMdcFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
