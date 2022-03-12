package com.atguigu.gulimall.search.vo;

import java.util.List;

import lombok.Data;

/**
 * @longxue
 * 封装页面传过来的所有查询条件
 * 
 */
@Data
public class SearchParam {

	//全文检索关键字
	private String keyword;
	
	//三级分类id
	private Long catelog3Id;
	
	/**
	 * 排序
	 * sort=saleCount_asc/desc
	 * sort=skuPrice_asc/desc
	 * 按照综合排序
	 * sort=hostScore_asc/desc
	 */
	private String sort;
	
	//以下都是过滤条件
	
	// 0：无货 1：有货
	private Integer hasStock;

	/**
	 * 价格区间
	 * skuPrice=1_500/_500/500_
	 */
	private String skuPrice;

	/**
	 * 品牌id 可以多选
	 * 
	 * brandId=1&brandId=2...
	 */
	private List<Long> brandId;

	/**
	 * 按照属性进行筛选
	 * 属性可以多个，格式为属性id_值：值&属性...
	 * attrs=1_xxx:xxx&attrs=2_xxx:xxx
	 */
	private List<String> attrs;

	/**
	 * 页码
	 * 检索出的商品过多时，需要返回分页
	 */
	private Integer pageNum = 1;

	/**
	 * 原生所有查询属性
	 */
	private String _queryString;

	
}
