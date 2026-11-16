package io.gymcrm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
public class DatabaseSettingsCheck implements BeanFactoryPostProcessor, EnvironmentAware {

    static final String URL = "spring.datasource.url";
    static final String USERNAME = "spring.datasource.username";
    static final String PASSWORD = "spring.datasource.password";

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        String url = require(URL);
        String username = require(USERNAME);
        require(PASSWORD);
        log.info("Environment {}: database {} as user {}", activeProfiles(), url, username);
    }

    private String require(String key) {
        try {
            String value = environment.getProperty(key);
            if (value == null) {
                throw new IllegalStateException(key + " is not set for environment " + activeProfiles());
            }
            return value;
        } catch (IllegalArgumentException e) {
            // "Could not resolve placeholder 'DB_PASSWORD' in value ..."
            throw new IllegalStateException("Missing setting for environment " + activeProfiles()
                    + " (" + key + "): " + e.getMessage(), e);
        }
    }

    private String activeProfiles() {
        String[] active = environment.getActiveProfiles();
        return Arrays.toString(active.length > 0 ? active : environment.getDefaultProfiles());
    }
}
