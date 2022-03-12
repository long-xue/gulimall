package com.atguigu.gulimall.cart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@ConfigurationProperties(prefix = "gulimall.thread")
@Component
@Data
public class ThreadPoolConfigProperties {

	private Integer coreSize;

	private Integer maxSize;

	private Integer keepAliveTime;
}
