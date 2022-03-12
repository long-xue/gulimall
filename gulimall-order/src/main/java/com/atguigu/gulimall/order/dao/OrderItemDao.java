package com.atguigu.gulimall.order.dao;

import org.apache.ibatis.annotations.Mapper;

import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 订单项信息
 * 
 * @author firenay
 * @email 1046762075@qq.com
 * @date 2020-05-30 00:54:56
 */
@Mapper
public interface OrderItemDao extends BaseMapper<OrderItemEntity> {
	
}
