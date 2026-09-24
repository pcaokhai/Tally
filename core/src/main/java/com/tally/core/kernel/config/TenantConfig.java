package com.tally.core.kernel.config;

import com.tally.core.kernel.tenant.TenantAwareDataSource;
import com.tally.core.kernel.tenant.TenantFilter;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Wires tenant context propagation (TLY-102): {@link TenantFilter} runs right after request
 * logging/tracing so downstream handlers and repositories see the tenant already bound, and every
 * {@link DataSource} bean is wrapped in {@link TenantAwareDataSource} so RLS is enforced regardless
 * of which module obtained the connection.
 */
@Configuration(proxyBeanMethods = false)
public class TenantConfig {

    @Bean
    FilterRegistrationBean<TenantFilter> tenantFilter() {
        FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>(new TenantFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }

    // static: a BeanPostProcessor bean must not trigger other beans to initialize early (Spring docs).
    @Bean
    static BeanPostProcessor tenantAwareDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource dataSource && !(bean instanceof TenantAwareDataSource)) {
                    return new TenantAwareDataSource(dataSource);
                }
                return bean;
            }
        };
    }
}
