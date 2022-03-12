package com.atguigu.gulimall.order.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.atguigu.gulimall.order.vo.MemberAddressVo;

/**
 * <p>Title: MemberFeignService</p>
 * Description：
 * date：2020/6/30 16:54
 */
@FeignClient("gulimall-member")
public interface MemberFeignService {

	@GetMapping("/member/memberreceiveaddress/{memberId}/addresses")
	List<MemberAddressVo> getAddress(@PathVariable("memberId") Long memberId);

}
