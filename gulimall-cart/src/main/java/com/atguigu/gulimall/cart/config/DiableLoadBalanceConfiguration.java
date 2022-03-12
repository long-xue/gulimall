package com.atguigu.gulimall.cart.config;

import feign.Client;
import org.springframework.context.annotation.Bean;

/**
 * @ClassName DiableLoadBalanceConfiguration
 * @Description TODO
 * @Author lonng-xue
 * @Date 2022/3/4/004 12:14
 **/
public class DiableLoadBalanceConfiguration {
    @Bean
    public Client feignClient() {
        return new Client.Default(null, null);
    }
}
