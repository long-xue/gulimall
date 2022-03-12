package com.atguigu.gulimall.search.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.search.service.ProductSaveService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/search/save")
@Slf4j
public class ElasticSaveController {
	
	@Autowired
	ProductSaveService productSaveService;
	
	@PostMapping("/product")
	public R productStatusUp(@RequestBody List<SkuEsModel> skuEsModels) {
		//上架商品
		boolean status;
		try {
			status = productSaveService.productStatusUp(skuEsModels);
		} catch (IOException e) {
			log.error("Elastic连接错误: {}", e);
			return R.error(BizCodeEnume.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnume.PRODUCT_UP_EXCEPTION.getMsg());
		}
		if (!status) {
			return	R.ok();
		}
		//这个是bulk操作异常
		return R.error(BizCodeEnume.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnume.PRODUCT_UP_EXCEPTION.getMsg());
		
	}

}
