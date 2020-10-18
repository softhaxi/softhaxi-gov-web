package com.softhaxi.marves.core.support;

import com.softhaxi.marves.core.MarvesCoreApplication;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Servlet Initializer
 * 
 * @author Raja Sihombing
 * @since 1
 */
public class ServletInitializer extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(MarvesCoreApplication.class);
    }
}
