package com.atguigu.gulimall.order.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.atguigu.gulimall.order.vo.OrderItemVo;

/**
 * <p>Title: CartFeignService</p>
 * Description：
 * date：2020/6/30 18:08
 */
@FeignClient("gulimall-cart")
public interface CartFeignService {

	@GetMapping("/currentUserCartItems")
	List<OrderItemVo> getCurrentUserCartItems();

}
