package com.atguigu.gulimall.product.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;

@FeignClient("gulimall-search")
public interface SerchFeignService {

	@PostMapping("/search/save/product")
	R productStatusUp(@RequestBody List<SkuEsModel> skuEsModels);
}
