package com.atguigu.gulimall.product.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.atguigu.common.utils.R;

@FeignClient("gulimall-ware")
public interface WareFeignService {
	
	
	@RequestMapping("/ware/waresku/hasStock")
	 public R getSkusHasStock(@RequestBody List<Long> skuIds);
	
}
