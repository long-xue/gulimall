package com.atguigu.gulimall.search.service;

import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;

/**
 * @longxue
 *
 * 
 */
public interface MallSearchSevice {
	

	/**
	 * 用来根据条件搜索结果
	 */
	SearchResult search(SearchParam param);
	
}
