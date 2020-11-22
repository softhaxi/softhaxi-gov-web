package com.softhaxi.marves.core.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebAssetConfiguration implements WebMvcConfigurer {

    @Value("${app.mobile.asset}")
    private String mobileAsset;

    @Value("${app.upload.path}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if(!uploadPath.endsWith("/"))
            uploadPath += "/";
        if(!mobileAsset.endsWith("/"))
            mobileAsset += "/";
        registry.addResourceHandler("/asset/**")
            .addResourceLocations(String.format("file:%s", uploadPath), 
                String.format("file:%s", mobileAsset))
            .setCachePeriod(3600)
            .resourceChain(true)
            .addResolver(new PathResourceResolver());
        // WebMvcConfigurer.super.addResourceHandlers(registry);
    }
}
