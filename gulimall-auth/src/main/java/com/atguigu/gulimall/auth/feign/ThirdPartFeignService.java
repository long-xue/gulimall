package com.atguigu.gulimall.auth.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.atguigu.common.utils.R;


@FeignClient(value = "gulimall-third-party")
public interface ThirdPartFeignService {

	@GetMapping("/sms/sendcode")
	R sendCode(@RequestParam("phone") String phone, @RequestParam("code") String code);
}
