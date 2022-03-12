package com.atguigu.gulimall.ware.service;

import java.util.List;
import java.util.Map;

import com.atguigu.common.to.es.SkuHasStockVo;
import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.to.mq.StockLockedTo;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.vo.WareSkuLockVo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 商品库存
 *
 * @author leifengyang
 * @email leifengyang@gmail.com
 * @date 2019-10-08 09:59:40
 */
public interface WareSkuService extends IService<WareSkuEntity> {

	void unlockStock(StockLockedTo to);

	PageUtils queryPage(Map<String, Object> params);

	/**
	 * 保存库存的时候顺便查到商品价格
	 */
	double addStock(Long skuId, Long wareId, Integer skuNum);

	/**
	 * 查询是否有库存
	 */
	List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds);

	/**
	 * 为某个订单锁定库存
	 */
	Boolean orderLockStock(WareSkuLockVo vo);

	/**
	 * 由于订单超时而自动释放订单之后来解锁库存
	 */
	void unlockStock(OrderTo to);


}

