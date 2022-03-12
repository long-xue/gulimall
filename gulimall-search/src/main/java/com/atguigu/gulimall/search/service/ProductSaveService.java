package com.atguigu.gulimall.search.service;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.atguigu.common.to.es.SkuEsModel;

@Service
public interface ProductSaveService {

	boolean productStatusUp(List<SkuEsModel> skuEsModel) throws IOException;

	
}
