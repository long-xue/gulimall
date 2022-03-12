package com.atguigu.gulimall.search.service.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.gulimall.search.config.GulimallElasticsearchConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.service.ProductSaveService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProductSaveServiceImpl implements ProductSaveService{

	@Resource
	RestHighLevelClient restHighLevelClient;
	
	@Override
	public boolean productStatusUp(List<SkuEsModel> skuEsModel) throws IOException {
		//上架商品
		BulkRequest bulkRequest = new BulkRequest();
		for (SkuEsModel sku : skuEsModel) {
			
			//执行批量index操作
			IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
			indexRequest.id(sku.getSkuId().toString());
			String jsonString = JSON.toJSONString(sku);
			indexRequest.source(jsonString,XContentType.JSON);
			bulkRequest.add(indexRequest);
		}
		//bulkRequest, options
		BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, GulimallElasticsearchConfig.COMMON_OPTIONS);
		boolean hasFailures = bulkResponse.hasFailures();
		//如果保存异常，打印异常商品id
		if (hasFailures) {
			List<String> collect = Arrays.stream(bulkResponse.getItems()).map(item -> {
				return item.getId();
			}).collect(Collectors.toList());
			log.error("商品上架错误：{}"+collect);
		}
		return hasFailures;
		
	}
	
	

}
