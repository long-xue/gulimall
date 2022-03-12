package com.atguigu.gulimall.product.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(value = "gulimall-seckill")
public interface SeckillFeignService {

	@RequestMapping("/sku/seckill/{skuId}")
	R getSkuSeckillInfo(@PathVariable("skuId") Long skuId);
}
