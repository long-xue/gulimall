package com.atguigu.gulimall.ware.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.atguigu.common.utils.R;

/**
 * <p>Title: MemberFeignService</p>
 * Description：
 * date：2020/7/1 12:56
 */
@FeignClient("gulimall-member")
public interface MemberFeignService {

	@RequestMapping("/member/memberreceiveaddress/info/{id}")
	R addrInfo(@PathVariable("id") Long id);
}
