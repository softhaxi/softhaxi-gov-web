package com.softhaxi.marves.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/** 
 * Main point of application
 * 
 * @author Raja Sihombing
 * @since 1
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.softhaxi.marves.core")
public class MarvesCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(MarvesCoreApplication.class, args);
	}

}
