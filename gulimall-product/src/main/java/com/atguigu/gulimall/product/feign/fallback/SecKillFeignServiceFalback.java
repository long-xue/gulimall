package com.atguigu.gulimall.product.feign.fallback;

import org.springframework.stereotype.Component;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.feign.SeckillFeignService;
import com.atguigu.common.exception.BizCodeEnume;

/**
 * <p>Title: SecKillFeignServiceFalback</p>
 * Description：
 * date：2020/7/10 16:03
 */
@Component
public class SecKillFeignServiceFalback implements SeckillFeignService {

	@Override
	public R getSkuSeckillInfo(Long skuId) {
		System.out.println("触发熔断");
		return R.error(BizCodeEnume.TO_MANY_REQUEST.getCode(), BizCodeEnume.TO_MANY_REQUEST.getMsg());
	}
}
