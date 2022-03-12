package com.atguigu.gulimall.search.config;

import org.apache.http.HttpHost;
import org.aspectj.apache.bcel.generic.ReturnaddressType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * 1. 导入配置
 * 2.编写配制类(给容器中注入一个RestHighLevelClient)
 * 
*/
@Configuration
public class GulimallElasticsearchConfig {
	
	@Value("${ipAddr}")
	private String ipAddr;
	
	public static final RequestOptions COMMON_OPTIONS;
	static {
	    RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
//	    builder.addHeader("Authorization", "Bearer " + TOKEN); 
//	    builder.setHttpAsyncResponseConsumerFactory(           
//	        new HttpAsyncResponseConsumerFactory
//	            .HeapBufferedResponseConsumerFactory(30 * 1024 * 1024 * 1024));
	    COMMON_OPTIONS = builder.build();
	}
	
	@Bean
	public RestHighLevelClient esRestClient() {
		
		RestClientBuilder builder = RestClient.builder(new HttpHost(ipAddr,9200,"http"));
		RestHighLevelClient client = new RestHighLevelClient(builder);
		return client;
	}
	
	
}
