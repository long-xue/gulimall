package com.atguigu.gulimall.search;


import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.search.bean.User;
import com.atguigu.gulimall.search.config.GulimallElasticsearchConfig;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Slf4j
@SpringBootTest
public class GulimallSearchApplicationTests {

	@Autowired
	private RestHighLevelClient client;
	private IndexRequest request;
	
	@Test
	void estest() {
		log.info(client.toString());
	}

	@Test
	public void searchData() throws IOException {
		// 1 创建制定检索请求
		SearchRequest searchRequest = new SearchRequest();
		// 2 指定索引
		searchRequest.indices("product");
		// 3 指定dsl
		
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		//指定匹配属性
		searchSourceBuilder.query(QueryBuilders.matchQuery("brandId", 2));
		//构造分布聚合
		TermsAggregationBuilder aggregationBuilder = AggregationBuilders.terms("skuPrice").field("skuPrice").size(10);
		searchSourceBuilder.aggregation(aggregationBuilder);
		//构造平均聚合
		AvgAggregationBuilder avgAggregationBuilder = AggregationBuilders.avg("saleCount").field("saleCount");
		searchSourceBuilder.aggregation(avgAggregationBuilder);
		//构造请求对象
		SearchRequest request = searchRequest.source(searchSourceBuilder);
		SearchResponse response = client.search(request, GulimallElasticsearchConfig.COMMON_OPTIONS);
		// 分析结果
		SearchHits hits = response.getHits();
		for (SearchHit hit : hits) {
			String sourceAsString = hit.getSourceAsString();
			Map<String, Object> sourceAsMap = hit.getSourceAsMap();
			log.info(sourceAsString);
			log.info("----------------------------------------------");
			Set<String> keySet = sourceAsMap.keySet();
			String string = keySet.toString();
			log.info(string);
			
		}
		//聚合分析数据
		Aggregations aggregations = response.getAggregations();
		Terms agg = aggregations.get("skuPrice");
		for (Terms.Bucket bucket : agg.getBuckets()) {
			System.out.println("属性名字: " + bucket.getKeyAsString() + "-->" + bucket.getDocCount() + "个");
		}

		Avg avg = aggregations.get("saleCount");
		System.out.println("平均薪资： " + avg.getValue());
		
		
		
	}
	
	
	
	@Test
	void indexData() throws IOException {
		request = new IndexRequest("users");
		// 设置索引id
		request.id("2");
		// 第1种方式
//		request.source("userName","firenay","age","20","gender","男");

		// 第2种方式
		User user = new User();
		user.setUserName("firenay");
		user.setAge("20");
		user.setGender("男");
		String jsonString = JSON.toJSONString(user);
		// 传入json时 指定类型
		request.source(jsonString, XContentType.JSON);

		
		// 执行保存操作
		IndexResponse response = client.index(request, GulimallElasticsearchConfig.COMMON_OPTIONS);

		System.out.println(response);
		System.out.println(response.status());
	}
	
}
