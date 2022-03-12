package com.atguigu.gulimall.member.feign;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.atguigu.common.utils.R;

/**
 * <p>Title: OrderFeignService</p>
 * Description：
 * date：2020/7/4 23:43
 */
@FeignClient("gulimall-order")
public interface OrderFeignService {

	@PostMapping("/order/order/listWithItem")
	R listWithItem(@RequestBody Map<String, Object> params);
}
