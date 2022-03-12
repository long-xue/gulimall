package com.atguigu.gulimall.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;



/**
 * @longxue
 * springMvc配置类
 * 
 */
@Configuration
public class MyWebConfig implements WebMvcConfigurer{

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/reg.html").setViewName("reg");
		WebMvcConfigurer.super.addViewControllers(registry);
		
	}
	
	
	
}
