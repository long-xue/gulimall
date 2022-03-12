package com.atguigu.gulimall.seckill.service.impl;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.mq.SecKillOrderTo;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRsepVo;
import com.atguigu.gulimall.seckill.feign.CouponFeignService;
import com.atguigu.gulimall.seckill.feign.ProductFeignService;
import com.atguigu.gulimall.seckill.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.seckill.service.SeckillService;
import com.atguigu.gulimall.seckill.to.SeckillSkuRedisTo;
import com.atguigu.gulimall.seckill.vo.SeckillSessionsWithSkus;
import com.atguigu.gulimall.seckill.vo.SkuInfoVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {


	@Autowired
		private CouponFeignService couponFeignService;

		@Autowired
		private StringRedisTemplate stringRedisTemplate;

		@Autowired
		private ProductFeignService productFeignService;

		@Autowired
		private RedissonClient redissonClient;

		@Autowired
		private RabbitTemplate rabbitTemplate;

		private final String SESSION_CACHE_PREFIX = "seckill:sessions:";

		private final String SKUKILL_CACHE_PREFIX = "seckill:skus:";

		private final String SKUSTOCK_SEMAPHONE = "seckill:stock:"; // +商品随机码
		
		/**
		 *	上架最近三天的秒杀商品到redis中
		 */
		@Override
		public void uploadSeckillSkuLatest3Day() {
			//1. 远程coupon服务扫描最近三天秒杀的商品
			R r = couponFeignService.getLate3DaySession();
			if (r.getCode() == 0) {
				List<SeckillSessionsWithSkus> sessions = r.getData(new TypeReference<List<SeckillSessionsWithSkus>>() {});
				if(sessions!=null){
					// 2.缓存活动信息
					saveSessionInfo(sessions);
					// 3.缓存活动的关联的商品信息
					saveSessionSkuInfo(sessions);
				}

				else{
					log.info("最近三天没有商品");
					return;
				}
			}
		}

		/**
		 * @param sessions
		 * 缓存活动场次信息
		 * redis中的保存 SESSION_CACHE_PREFIX + startTime + "_" + endTime：场次id+skuid
		 */
		private void saveSessionSkuInfo(List<SeckillSessionsWithSkus> sessions) {
			if (sessions != null) {
				sessions.stream().forEach(session -> {
					long startTime = session.getStartTime().getTime();
					long endTime = session.getEndTime().getTime();
					String key = SESSION_CACHE_PREFIX + startTime + "_" + endTime;
					Boolean hasKey = stringRedisTemplate.hasKey(key);
					if (!hasKey) {
						//获取所有商品id
						List<String> collect = session.getRelationSkus().stream().map(
								item -> item.getPromotionSessionId()+ "-" + item.getSkuId()
								).collect(Collectors.toList());
						stringRedisTemplate.opsForList().leftPushAll(key, collect);
					}
				});
			}
		}

		/**
		 * @param sessions
		 * 缓存秒杀商品信息 
		 * redis中的保存
		 * 场次id + "-" + 商品的skuid, SeckillSkuRedisTo对象
		 */
		private void saveSessionInfo(List<SeckillSessionsWithSkus> sessions) {
			sessions.stream().forEach(session -> {
				BoundHashOperations<String, Object, Object> ops = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
				session.getRelationSkus().stream().forEach(seckillSkuVo -> {
					//1. 设置商品的随机码，防止恶意攻击
					String randomCode = UUID.randomUUID().toString().replace("-", "");
					if(!ops.hasKey(seckillSkuVo.getPromotionSessionId() + "-" + seckillSkuVo.getSkuId())){
						//2. 构造redis保存的数据 
						SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
						BeanUtils.copyProperties(seckillSkuVo, redisTo);
						// 3.远程商品服务查询sku的基本数据 设置sku的开始结束时间
						R info = productFeignService.skuInfo(seckillSkuVo.getSkuId());
						if(info.getCode() == 0){
							SkuInfoVo skuInfo = info.getData("skuInfo", new TypeReference<SkuInfoVo>() {});
							redisTo.setSkuInfoVo(skuInfo);
						}
						// 4.设置当前商品秒杀的开始结束时间
						redisTo.setStartTime(session.getStartTime().getTime());
						redisTo.setEndTime(session.getEndTime().getTime());
						// 5.设置随机码
						redisTo.setRandomCode(randomCode);
						ops.put(seckillSkuVo.getPromotionSessionId() + "-" + seckillSkuVo.getSkuId(), JSON.toJSONString(redisTo));
						// 如果当前这个场次的商品库存已经上架就不需要上架了
						// 5.使用库存作为分布式信号量  限流
						RSemaphore semaphore = redissonClient.getSemaphore(SKUSTOCK_SEMAPHONE + randomCode);
						semaphore.trySetPermits(seckillSkuVo.getSeckillCount().intValue());
					}
				});
			});
		}
		
		@Override
		@SentinelResource(value = "资源名")
		public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
			ArrayList<SeckillSkuRedisTo> redisTos = new ArrayList<>();

			// 1.确定当前时间属于那个秒杀场次
			long time = new Date().getTime();
			// 定义一段受保护的资源
			try (Entry entry = SphU.entry("seckillSkus")){
				Set<String> keys = stringRedisTemplate.keys(SESSION_CACHE_PREFIX + "*");
				for (String key : keys) {
					// seckill:sessions:1593993600000_1593995400000
					String replace = key.replace("seckill:sessions:", "");
					String[] split = replace.split("_");
					long start = Long.parseLong(split[0]);
					long end = Long.parseLong(split[1]);
					if(time >= start && time <= end){
						// 2.获取这个秒杀场次的所有商品信息
						List<String> range = stringRedisTemplate.opsForList().range(key, 0, 100);
						BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
						List<String> list = hashOps.multiGet(range);
						if(list != null){
							for(String item : list){
								SeckillSkuRedisTo redisTo = JSON.parseObject(item, SeckillSkuRedisTo.class);
								redisTos.add(redisTo);
							}
// 							List<SeckillSkuRedisTo> collect = list.stream().map(item -> {
// 								SeckillSkuRedisTo redisTo = JSON.parseObject(item, SeckillSkuRedisTo.class);
// //							redisTo.setRandomCode(null);
// 								redisTos.add(redisTo);
// 								return redisTo;
// 							}).collect(Collectors.toList());
						}
					}
				}

			}catch (BlockException e){
				log.warn("资源被限流：" + e.getMessage());
				return null;
			}
			return redisTos;
		}

		@Override
		public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {
			BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
			Set<String> keys = hashOps.keys();
			if(keys != null && keys.size() > 0){
				String regx = "\\d-" + skuId;
				for (String key : keys) {
					if(Pattern.matches(regx, key)){
						String json = hashOps.get(key);
						SeckillSkuRedisTo to = JSON.parseObject(json, SeckillSkuRedisTo.class);
						// 处理一下随机码
						long current = new Date().getTime();

						if(current <= to.getStartTime() || current >= to.getEndTime()){
							to.setRandomCode(null);
						}
						return to;
					}
				}
			}
			return null;
		}

		/**
		 *killId 场次id+skuid
		 *key	随机码
		 *num	秒杀数量
		 */
		@Override
		public String kill(String killId, String key, Integer num) {

			MemberRsepVo memberRsepVo = LoginUserInterceptor.threadLocal.get();

			// 1.获取当前秒杀商品的详细信息
			BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
			String json = hashOps.get(killId);
			if(StringUtils.isEmpty(json)){
				return null;
			}else{
				SeckillSkuRedisTo redisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);
				// 校验合法性
				long time = new Date().getTime();
				if(time >= redisTo.getStartTime() && time <= redisTo.getEndTime()){
					// 1.校验随机码跟商品id是否匹配
					String randomCode = redisTo.getRandomCode();
					String skuId = redisTo.getPromotionSessionId() + "-" + redisTo.getSkuId();
					
					if(randomCode.equals(key) && killId.equals(skuId)){
						// 2.说明数据合法
						BigDecimal limit = redisTo.getSeckillLimit();
						if(num <= limit.intValue()){
							// 3.验证这个人是否已经购买过了
							String redisKey = memberRsepVo.getId() + "-" + skuId;
							// 让数据自动过期
							long ttl = redisTo.getEndTime() - redisTo.getStartTime();
							//占坑操作
							Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl<0?0:ttl, TimeUnit.MILLISECONDS);
							if(aBoolean){
								// 占位成功 说明从来没买过
								RSemaphore semaphore = redissonClient.getSemaphore(SKUSTOCK_SEMAPHONE + randomCode);
								boolean acquire = semaphore.tryAcquire(num);
								if(acquire){
									// 秒杀成功
									// 快速下单 发送MQ
									String orderSn = IdWorker.getTimeId() + UUID.randomUUID().toString().replace("-","").substring(7,8);
									SecKillOrderTo orderTo = new SecKillOrderTo();
									orderTo.setOrderSn(orderSn);
									orderTo.setMemberId(memberRsepVo.getId());
									orderTo.setNum(num);
									orderTo.setSkuId(redisTo.getSkuId());
									orderTo.setSeckillPrice(redisTo.getSeckillPrice());
									orderTo.setPromotionSessionId(redisTo.getPromotionSessionId());
									rabbitTemplate.convertAndSend("order-event-exchange","order.seckill.order", orderTo);
									return orderSn;
								}
							}else {
								return null;
							}
						}
					}else{
						return null;
					}
				}else{
					return null;
				}
			}
			return null;
		}

		
	}


