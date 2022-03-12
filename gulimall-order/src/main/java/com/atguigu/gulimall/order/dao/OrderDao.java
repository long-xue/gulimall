package com.atguigu.gulimall.order.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 订单
 * 
 * @author firenay
 * @email 1046762075@qq.com
 * @date 2020-05-30 00:54:56
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {

	void updateOrderStatus(@Param("orderSn") String orderSn, @Param("code") Integer code);
}
