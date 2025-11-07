package com.payhint.api.infrastructure.shared.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.payhint.api.domain", includeFilters = @ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ANNOTATION, classes = com.payhint.api.domain.shared.annotation.DomainComponent.class))
public class DomainConfiguration {

}
