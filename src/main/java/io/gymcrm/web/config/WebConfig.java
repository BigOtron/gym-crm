package io.gymcrm.web.config;

import io.gymcrm.web.auth.BasicAuthArgumentResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private BasicAuthArgumentResolver basicAuthArgumentResolver;

    @Autowired
    public void setBasicAuthArgumentResolver(BasicAuthArgumentResolver basicAuthArgumentResolver) {
        this.basicAuthArgumentResolver = basicAuthArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(basicAuthArgumentResolver);
    }
}
