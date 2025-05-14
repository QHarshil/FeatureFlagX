package com.featureflagx.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// This class is primarily for enabling JPA features like auditing if needed.
// Basic DataSource configuration is typically handled by Spring Boot auto-configuration
// based on the properties in application.yml (e.g., spring.datasource.url).
// If more specific bean definitions for DataSource are needed, they would go here.

@Configuration
@EnableJpaAuditing // If you plan to use @CreatedDate, @LastModifiedDate in your entities
public class DataSourceConfig {
    // Spring Boot will automatically configure a DataSource bean if the
    // spring-boot-starter-data-jpa dependency is present and database connection
    // properties (spring.datasource.url, username, password, driver-class-name)
    // are provided in application.yml or application.properties.

    // Example of a custom DataSource bean if needed:
    /*
    @Bean
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.driver-class-name}") String driverClassName) {
        DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName(driverClassName);
        dataSourceBuilder.url(url);
        dataSourceBuilder.username(username);
        dataSourceBuilder.password(password);
        return dataSourceBuilder.build();
    }
    */
}

