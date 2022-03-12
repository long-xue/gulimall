package com.atguigu.gulimall.product.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class MyRedissonConfig {

	
//	@Bean(destroyMethod="shutdown")
//	public RedissonClient redisson() {
//		Config config = new Config();
//		config.useSingleServer().setAddress("redis://127.0.0.1:6379");
//		config.useSingleServer().setConnectionMinimumIdleSize(10);
//		RedissonClient redissonClient = Redisson.create(config);
//		return redissonClient;
//	}
	
}
