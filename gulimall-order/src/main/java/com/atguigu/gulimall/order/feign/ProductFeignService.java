package com.atguigu.gulimall.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.atguigu.common.utils.R;

/**
 * <p>Title: ProductFeignService</p>
 * Description：
 * date：2020/7/2 0:43
 */
@FeignClient("gulimall-product")
public interface ProductFeignService {

	@GetMapping("/product/spuinfo/skuId/{id}")
	R getSkuInfoBySkuId(@PathVariable("id") Long skuId);
}
