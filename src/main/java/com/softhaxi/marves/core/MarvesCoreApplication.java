package com.softhaxi.marves.core;

import com.softhaxi.marves.core.properties.FileStorageProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** 
 * Main point of application
 * 
 * @author Raja Sihombing
 * @since 1
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableJpaRepositories
@ComponentScan(basePackages = "com.softhaxi.marves.core")
@EnableConfigurationProperties({
	FileStorageProperties.class
})
public class MarvesCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(MarvesCoreApplication.class, args);
	}

}
