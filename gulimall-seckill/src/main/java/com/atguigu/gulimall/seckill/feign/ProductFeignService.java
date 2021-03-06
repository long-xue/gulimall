package com.atguigu.gulimall.seckill.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.atguigu.common.utils.R;

/**
 * <p>Title: ProductFeignService</p>
 * Description：
 * date：2020/7/6 19:16
 */
@FeignClient(value = "gulimall-product")
public interface ProductFeignService {

	@RequestMapping("/product/skuinfo/info/{skuId}")
	R skuInfo(@PathVariable("skuId") Long skuId);
}
