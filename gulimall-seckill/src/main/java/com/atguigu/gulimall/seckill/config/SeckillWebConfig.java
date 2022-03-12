package com.atguigu.gulimall.seckill.config;

import com.atguigu.gulimall.seckill.interceptor.LoginUserInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * <p>Title: SeckillWebConfig</p>
 * Description：
 * date：2020/7/9 16:02
 */
@Configuration
public class SeckillWebConfig implements WebMvcConfigurer {

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new LoginUserInterceptor()).addPathPatterns("/**");
	}
}
