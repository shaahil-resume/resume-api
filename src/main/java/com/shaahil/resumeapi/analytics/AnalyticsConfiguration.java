package com.shaahil.resumeapi.analytics;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableConfigurationProperties(AnalyticsPricingProperties.class)
public class AnalyticsConfiguration {
}
