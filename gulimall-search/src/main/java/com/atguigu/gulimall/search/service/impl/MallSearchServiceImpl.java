package com.atguigu.gulimall.search.service.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.search.config.GulimallElasticsearchConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.feign.ProductFeignService;
import com.atguigu.gulimall.search.service.MallSearchSevice;
import com.atguigu.gulimall.search.vo.AttrResponseVo;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import com.atguigu.gulimall.search.vo.SearchResult.AttrVo;
import com.atguigu.gulimall.search.vo.SearchResult.BrandVo;
import com.atguigu.gulimall.search.vo.SearchResult.CatalogVo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MallSearchServiceImpl implements MallSearchSevice {

	@Autowired
	RestHighLevelClient restHighLevelClient;
	
	@Resource
	private ProductFeignService productFeignService;
	/**
	 *根据封装好的查询条件动态构建queryDsl
	 *
	 */
	@Override
	public SearchResult search(SearchParam param) {
		//动态构建查询dsl
		//1.准备检索请求
		SearchRequest searchRequest = buildSearchRequest(param);
		
		SearchResponse searchResponse = null;
		//2.通过RestHighLevelClient执行检索请求
		try {
			searchResponse = restHighLevelClient.search(searchRequest, GulimallElasticsearchConfig.COMMON_OPTIONS);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//3.分析响应的数据封装成我们需要的格式
		SearchResult searchResult = buildSearchResult(searchResponse, param);
		return searchResult;
	}

	/**
	 * @param searchresponse
	 * @return 将es搜索得到的数据封装成SearchResult
	 * 
	 */
	private SearchResult buildSearchResult(SearchResponse searchresponse, SearchParam param) {
	SearchResult result = new SearchResult();
	// 1.封装返回所有查询到的商品
	SearchHits hits = searchresponse.getHits();
	
	List<SkuEsModel> esModels = new ArrayList<SkuEsModel>();
	if (hits.getHits() != null &&  hits.getHits().length > 0) {
		for (SearchHit hit : hits.getHits()) {
			String sourceAsString = hit.getSourceAsString();
			SkuEsModel esModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
			if (!StringUtils.isEmpty(param.getKeyword())) {
				// 1.1 获取标题高亮属性
				HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");;
				String highlightFields = skuTitle.getFragments()[0].string();
				// 1.2 设置文本高亮
				esModel.setSkuTitle(highlightFields);
			}
			esModels.add(esModel);
		}
	}
		result.setProducts(esModels);
		// 2. 当前所有商品涉及到的所有属性信息
		ArrayList<AttrVo> attrVos = new ArrayList<SearchResult.AttrVo>();
		ParsedNested attr_agg = searchresponse.getAggregations().get("attr_agg");
		ParsedLongTerms attr_id = attr_agg.getAggregations().get("attr_id_agg");
		
		for (Bucket bucket : attr_id.getBuckets()) {
			AttrVo attrVo = new AttrVo();
			// 2.1 得到属性的id
			attrVo.setAttrId(bucket.getKeyAsNumber().longValue());
			// 2.2 得到属性的名字
			String attr_name = ((ParsedStringTerms)bucket.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();
			attrVo.setAttrName(attr_name);
			// 2.3 得到属性的所有值
			List<String> attr_value =((ParsedStringTerms) bucket.getAggregations().get("attr_value_agg")).getBuckets().stream().map(item -> item.getKeyAsString()).collect(Collectors.toList());
			attrVo.setAttrValue(attr_value);
			attrVos.add(attrVo);
		}
		result.setAttrs(attrVos);
		
		//3. 当前商品涉及到的所有品牌信息
		ArrayList<BrandVo> brandVos = new ArrayList<BrandVo>();
		ParsedLongTerms brand_agg = searchresponse.getAggregations().get("brand_agg");
		for (Bucket bucket : brand_agg.getBuckets()) {
			BrandVo brandVo = new BrandVo();
			//3.1 得到品牌的id
			long brandId = bucket.getKeyAsNumber().longValue();
			brandVo.setBrandId(brandId);
			
			//3.2 得到品牌的名字
			String brand_name = ((ParsedStringTerms)bucket.getAggregations().get("brand_name_agg")).getBuckets().get(0).getKeyAsString();
			brandVo.setBrandName(brand_name);
			// 3.3 得到品牌的图片
			String brand_img = ((ParsedStringTerms)bucket.getAggregations().get("brand_img_agg")).getBuckets().get(0).getKeyAsString();
			brandVo.setBrandImg(brand_img);
			brandVos.add(brandVo);
		}
		result.setBrands(brandVos);
		
		//4. 当前商品涉及到的所有分类信息
		ParsedLongTerms catalog_agg = searchresponse.getAggregations().get("catalog_agg");
		List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
		for (Terms.Bucket bucket : catalog_agg.getBuckets()) {
			CatalogVo catalogVo = new SearchResult.CatalogVo();
			// 设置分类id
			catalogVo.setCatalogId(Long.parseLong(bucket.getKeyAsString()));
			//设置分类名字
			ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
			String catalog_name = catalog_name_agg.getBuckets().get(0).getKeyAsString();
			catalogVo.setCatalogName(catalog_name);
			catalogVos.add(catalogVo);
		}
		result.setCatalogs(catalogVos);
		// ================以上信息从聚合信息中获取
		
		// 5.分页信息-页码
		result.setPageNum(param.getPageNum());
		// 总记录数
		long total = hits.getTotalHits().value;
		result.setTotal(total);
		// 总页码：计算得到
		int page = (int) (total % EsConstant.PRODUCT_PASIZE);
		int totalPages = page==0?page:page+1;
		result.setTotalPages(totalPages);
		// 设置导航页
		ArrayList<Integer> pageNavs = new ArrayList<>();
		for (int i = 1;i <= totalPages; i++){
			pageNavs.add(i);
		}
		result.setPageNavs(pageNavs);
		
		
		// 6.构建面包屑导航功能
		if(param.getAttrs() != null){
			List<SearchResult.NavVo> navVos = param.getAttrs().stream().map(attr -> {
				SearchResult.NavVo navVo = new SearchResult.NavVo();
				String[] s = attr.split("_");
				navVo.setNavValue(s[1]);
				R r = productFeignService.getAttrsInfo(Long.parseLong(s[0]));
				// 将已选择的请求参数添加进去 前端页面进行排除，面包屑上有属性就不该显示属性
				result.getAttrIds().add(Long.parseLong(s[0]));
				if(r.getCode() == 0){
					AttrResponseVo data = r.getData(new TypeReference<AttrResponseVo>(){});
					navVo.setName(data.getAttrName());
				}else{
					// 失败了就拿id作为名字
					navVo.setName(s[0]);
				}
				// 拿到所有查询条件 替换查询条件
				//当取消面包屑的一个属性时，将url中对应的属性缓成空字符串
				String replace = replaceQueryString(param, attr, "attrs");
				navVo.setLink("http://search.gulmall.com/list.html?" + replace);
				return navVo;
			}).collect(Collectors.toList());
			result.setNavs(navVos);
		}
		
		
		// 面包屑中品牌、分类
		if(param.getBrandId() != null && param.getBrandId().size() > 0){
			List<SearchResult.NavVo> navs = result.getNavs();
			SearchResult.NavVo navVo = new SearchResult.NavVo();
			navVo.setName("品牌");
			// TODO 远程查询所有品牌
			R r = productFeignService.brandInfo(param.getBrandId());
			if(r.getCode() == 0){
				List<BrandVo> brand = r.getData("data", new TypeReference<List<BrandVo>>() {});
				StringBuffer buffer = new StringBuffer();
				// 替换所有品牌ID
				String replace = "";
				for (BrandVo brandVo : brand) {
					buffer.append(brandVo.getBrandName() + ";");
					replace = replaceQueryString(param, brandVo.getBrandId() + "", "brandId");
				}
				navVo.setNavValue(buffer.toString());
				navVo.setLink("http://search.gulimall.com/list.html?" + replace);
			}
			navs.add(navVo);
		}
		
		return result;
	}
	
	/**
	 * 替换字符
	 * key ：需要替换的key
	 */
	private String replaceQueryString(SearchParam Param, String value, String key) {
		String encode = null;
		try {
			encode = URLEncoder.encode(value,"UTF-8");
			// 浏览器对空格的编码和java的不一样
			encode = encode.replace("+","%20");
			encode = encode.replace("%28", "(").replace("%29",")");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return Param.get_queryString().replace("&" + key + "=" + encode, "");
	}
	

	/**
	 * @return
	 * 构建SearchRequest请求	
	 * 准备检索请求  [构建查询语句]
	 *
	 */
	private SearchRequest buildSearchRequest(SearchParam param) {
		
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		// 1. 模糊匹配 过滤(按照属性、分类、品牌、价格区间、库存) 先构建一个布尔Query
		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
		// 1.1 must
		if (!StringUtils.isEmpty(param.getKeyword())) {
			boolQuery.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
		}
		// 1.2 bool - filter Catalog3Id
		if (param.getCatelog3Id()!=null) {
			boolQuery.filter(QueryBuilders.termQuery("catalogId", param.getCatelog3Id()));
		}
		// 1.2 bool - brandId [集合]
		
		if (param.getBrandId()!=null && param.getBrandId().size()>0) {
			boolQuery.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
		}
		// 属性查询
		//attrs=1_白色:黑色&attrs=.
		if (param.getAttrs() != null && param.getAttrs().size() > 0) {
			for (String attrStr : param.getAttrs()) {
					BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
					String[] s = attrStr.split("_");
					// 检索的id  属性检索用的值
					String attrId = s[0];
					String[] attrValue = s[1].split(":");
					boolQueryBuilder.must(QueryBuilders.termQuery("attrs.attrId", attrId));
					boolQueryBuilder.must(QueryBuilders.termsQuery("attrs.attrValue", attrValue));
					// 构建一个嵌入式Query 每一个必须都得生成嵌入的 nested 查询
					NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", boolQueryBuilder, ScoreMode.None);
					boolQuery.filter(nestedQuery);
			}
		}
		// 1.2 bool - filter [库存]
		if (param.getHasStock()!=null) {
			boolQuery.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
		}
		
		// 1.2 bool - filter [价格区间]
		if (!StringUtils.isEmpty(param.getSkuPrice())) {
			RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
			String[] s = param.getSkuPrice().split("_");
			if(s.length == 2){
				// 有三个值 就是区间
				rangeQuery.gte(s[0]).lte(s[1]);
			}else if(s.length == 1){
				// 单值情况
				if(param.getSkuPrice().startsWith("_")){
					rangeQuery.lte(s[0]);
				}
				if(param.getSkuPrice().endsWith("_")){
					rangeQuery.gte(s[0]);
				}
			}
			boolQuery.filter(rangeQuery);
		}
		//把以前所有条件都拿来进行封装
		searchSourceBuilder.query(boolQuery);
		// 1.排序
		if(!StringUtils.isEmpty(param.getSort())){
			String sort = param.getSort();
			String[] s1 = sort.split("_");
			SortOrder order = s1[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
			searchSourceBuilder.sort(s1[0],order);
		}
		// 2.分页 pageSize ： 5
		searchSourceBuilder.from((param.getPageNum()-1)*EsConstant.PRODUCT_PASIZE);
		searchSourceBuilder.size(EsConstant.PRODUCT_PASIZE);
		
		// 3.高亮
		HighlightBuilder builder = new HighlightBuilder();
		builder.field("skuTitle");
		builder.preTags("<b style='color:red'>");
		builder.postTags("</b>");
		searchSourceBuilder.highlighter(builder);
		
		// 聚合分析
		// TODO 1.品牌聚合
		TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
		brand_agg.field("brandId").size(50);
		
		// 品牌聚合的子聚合
		TermsAggregationBuilder brand_name = AggregationBuilders.terms("brand_name_agg").field("brandName").size(1); 
		TermsAggregationBuilder brand_img = AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1); 
		brand_agg.subAggregation(brand_name);
		brand_agg.subAggregation(brand_img);
		// 将品牌聚合加入 sourceBuilder
		searchSourceBuilder.aggregation(brand_agg);
		
		// TODO 2.分类聚合
		TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
		//分类聚合的子聚合
		TermsAggregationBuilder catalog_name_agg = AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1);
		catalog_agg.subAggregation(catalog_name_agg);
		// 将分类聚合加入 sourceBuilder
		searchSourceBuilder.aggregation(catalog_agg);
		
		
		// TODO 3.属性聚合 attr_agg 构建嵌入式聚合
		NestedAggregationBuilder nested = AggregationBuilders.nested("attr_agg", "attrs");
		// 3.1 聚合出当前所有的attrId
		
		TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId").size(10);
		// 3.1.1 聚合分析出当前attrId对应的attrName
		TermsAggregationBuilder attr_name_agg = AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1);
		// 3.1.2 聚合分析出当前attrId对应的所有可能的属性值attrValue	这里的属性值可能会有很多 所以写50
		TermsAggregationBuilder attr_value_agg = AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50);
		attr_id_agg.subAggregation(attr_name_agg);
		attr_id_agg.subAggregation(attr_value_agg);
		// 3.2 将这个子聚合加入嵌入式聚合
		nested.subAggregation(attr_id_agg);
		searchSourceBuilder.aggregation(nested);
		//至此，dsl语句构建完成
		//打印出构建好的dsl语句
		log.info("\n构建语句：->\n" + searchSourceBuilder.toString());
		//构建SearchRequest对象
		SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, searchSourceBuilder);
		return searchRequest;
		}
	
	
	
	
}