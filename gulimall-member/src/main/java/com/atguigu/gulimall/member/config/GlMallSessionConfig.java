package com.atguigu.gulimall.member.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * <p>Title: GlMallSessionConfig</p>
 * Description：设置Session作用域、自定义cookie序列化机制
 * date：2020/7/4 22:57
 */
@Configuration
public class GlMallSessionConfig {

	
	/**
	 * @return
	 * 解决session分布式下的不同域不不共享问题，更改cookieSerializer的设置方式
	 */
	@Bean
	public CookieSerializer cookieSerializer(){
		DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
		// 明确的指定Cookie的作用域
		cookieSerializer.setDomainName("gulimall.com");
		cookieSerializer.setCookieName("FIRESESSION");
		return cookieSerializer;
	}

	/**
	 * 自定义序列化机制
	 * 这里方法名必须是：springSessionDefaultRedisSerializer
	 */
	@Bean
	public RedisSerializer<Object> springSessionDefaultRedisSerializer(){
		return new GenericJackson2JsonRedisSerializer();
	}
}
