package com.tally.core.kernel.config;

import com.tally.core.kernel.web.PortScopeFilter;
import org.apache.catalina.connector.Connector;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Adds the operator connector (ADR-022, docs/02 §8) next to the tenant connector. Both connectors
 * share one servlet context, so {@link PortScopeFilter} is what actually separates the two APIs.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OpsServerProperties.class)
public class OpsConnectorConfig {

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> opsConnector(OpsServerProperties properties) {
        return factory -> {
            Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            connector.setPort(properties.port());
            factory.addAdditionalConnectors(connector);
        };
    }

    @Bean
    FilterRegistrationBean<PortScopeFilter> portScopeFilter(OpsServerProperties properties) {
        FilterRegistrationBean<PortScopeFilter> registration =
                new FilterRegistrationBean<>(new PortScopeFilter(properties.port()));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
