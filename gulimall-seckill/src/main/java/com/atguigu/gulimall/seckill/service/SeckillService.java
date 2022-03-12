package com.atguigu.gulimall.seckill.service;

import java.util.List;

import com.atguigu.gulimall.seckill.to.SeckillSkuRedisTo;

public interface SeckillService {

	void uploadSeckillSkuLatest3Day();

	SeckillSkuRedisTo getSkuSeckillInfo(Long skuId);

	List<SeckillSkuRedisTo> getCurrentSeckillSkus();

	String kill(String killId, String key, Integer num);

}
