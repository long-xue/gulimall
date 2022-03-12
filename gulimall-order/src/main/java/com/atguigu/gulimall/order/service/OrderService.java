package com.atguigu.gulimall.order.service;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.ibatis.annotations.Param;

import com.atguigu.common.to.mq.SecKillOrderTo;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import com.atguigu.gulimall.order.vo.PayAsyncVo;
import com.atguigu.gulimall.order.vo.PayVo;
import com.atguigu.gulimall.order.vo.SubmitOrderResponseVo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 订单
 *
 * @author firenay
 * @email 1046762075@qq.com
 * @date 2020-05-30 00:54:56
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

	/**
	 * 给订单确认页返回需要的数据
	 */
	OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

	/**
	 * 下单操作
	 */
	SubmitOrderResponseVo submitOrder(OrderSubmitVo submitVo);

	OrderEntity getOrderByOrderSn(String orderSn);

	void closeOrder(OrderEntity entity);

	/**
	 * 获取当前订单的支付信息
	 */
	PayVo getOrderPay(String orderSn);

	PageUtils queryPageWithItem(@Param("params") Map<String, Object> params);

	/**
	 * 处理支付宝的返回数据
	 */
	String handlePayResult(PayAsyncVo vo);

	void createSecKillOrder(SecKillOrderTo secKillOrderTo);
}

