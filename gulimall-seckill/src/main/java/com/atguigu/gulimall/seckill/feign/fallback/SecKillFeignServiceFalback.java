package com.atguigu.gulimall.seckill.feign.fallback;

import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.seckill.feign.CouponFeignService;
import org.springframework.stereotype.Component;

/**
 * <p>Title: SecKillFeignServiceFalback</p>
 * Description：
 * date：2020/7/10 16:03
 */
@Component
public class SecKillFeignServiceFalback implements CouponFeignService {

	@Override
	public R getLate3DaySession() {
		System.out.println("触发熔断");
		return com.atguigu.common.utils.R.error(BizCodeEnume.TO_MANY_REQUEST.getCode(), BizCodeEnume.TO_MANY_REQUEST.getMsg());
	}
}
