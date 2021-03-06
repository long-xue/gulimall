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
	 *??????????????????????????????????????????queryDsl
	 *
	 */
	@Override
	public SearchResult search(SearchParam param) {
		//??????????????????dsl
		//1.??????????????????
		SearchRequest searchRequest = buildSearchRequest(param);
		
		SearchResponse searchResponse = null;
		//2.??????RestHighLevelClient??????????????????
		try {
			searchResponse = restHighLevelClient.search(searchRequest, GulimallElasticsearchConfig.COMMON_OPTIONS);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//3.???????????????????????????????????????????????????
		SearchResult searchResult = buildSearchResult(searchResponse, param);
		return searchResult;
	}

	/**
	 * @param searchresponse
	 * @return ???es??????????????????????????????SearchResult
	 * 
	 */
	private SearchResult buildSearchResult(SearchResponse searchresponse, SearchParam param) {
	SearchResult result = new SearchResult();
	// 1.????????????????????????????????????
	SearchHits hits = searchresponse.getHits();
	
	List<SkuEsModel> esModels = new ArrayList<SkuEsModel>();
	if (hits.getHits() != null &&  hits.getHits().length > 0) {
		for (SearchHit hit : hits.getHits()) {
			String sourceAsString = hit.getSourceAsString();
			SkuEsModel esModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
			if (!StringUtils.isEmpty(param.getKeyword())) {
				// 1.1 ????????????????????????
				HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");;
				String highlightFields = skuTitle.getFragments()[0].string();
				// 1.2 ??????????????????
				esModel.setSkuTitle(highlightFields);
			}
			esModels.add(esModel);
		}
	}
		result.setProducts(esModels);
		// 2. ????????????????????????????????????????????????
		ArrayList<AttrVo> attrVos = new ArrayList<SearchResult.AttrVo>();
		ParsedNested attr_agg = searchresponse.getAggregations().get("attr_agg");
		ParsedLongTerms attr_id = attr_agg.getAggregations().get("attr_id_agg");
		
		for (Bucket bucket : attr_id.getBuckets()) {
			AttrVo attrVo = new AttrVo();
			// 2.1 ???????????????id
			attrVo.setAttrId(bucket.getKeyAsNumber().longValue());
			// 2.2 ?????????????????????
			String attr_name = ((ParsedStringTerms)bucket.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();
			attrVo.setAttrName(attr_name);
			// 2.3 ????????????????????????
			List<String> attr_value =((ParsedStringTerms) bucket.getAggregations().get("attr_value_agg")).getBuckets().stream().map(item -> item.getKeyAsString()).collect(Collectors.toList());
			attrVo.setAttrValue(attr_value);
			attrVos.add(attrVo);
		}
		result.setAttrs(attrVos);
		
		//3. ??????????????????????????????????????????
		ArrayList<BrandVo> brandVos = new ArrayList<BrandVo>();
		ParsedLongTerms brand_agg = searchresponse.getAggregations().get("brand_agg");
		for (Bucket bucket : brand_agg.getBuckets()) {
			BrandVo brandVo = new BrandVo();
			//3.1 ???????????????id
			long brandId = bucket.getKeyAsNumber().longValue();
			brandVo.setBrandId(brandId);
			
			//3.2 ?????????????????????
			String brand_name = ((ParsedStringTerms)bucket.getAggregations().get("brand_name_agg")).getBuckets().get(0).getKeyAsString();
			brandVo.setBrandName(brand_name);
			// 3.3 ?????????????????????
			String brand_img = ((ParsedStringTerms)bucket.getAggregations().get("brand_img_agg")).getBuckets().get(0).getKeyAsString();
			brandVo.setBrandImg(brand_img);
			brandVos.add(brandVo);
		}
		result.setBrands(brandVos);
		
		//4. ??????????????????????????????????????????
		ParsedLongTerms catalog_agg = searchresponse.getAggregations().get("catalog_agg");
		List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
		for (Terms.Bucket bucket : catalog_agg.getBuckets()) {
			CatalogVo catalogVo = new SearchResult.CatalogVo();
			// ????????????id
			catalogVo.setCatalogId(Long.parseLong(bucket.getKeyAsString()));
			//??????????????????
			ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
			String catalog_name = catalog_name_agg.getBuckets().get(0).getKeyAsString();
			catalogVo.setCatalogName(catalog_name);
			catalogVos.add(catalogVo);
		}
		result.setCatalogs(catalogVos);
		// ================????????????????????????????????????
		
		// 5.????????????-??????
		result.setPageNum(param.getPageNum());
		// ????????????
		long total = hits.getTotalHits().value;
		result.setTotal(total);
		// ????????????????????????
		int page = (int) (total % EsConstant.PRODUCT_PASIZE);
		int totalPages = page==0?page:page+1;
		result.setTotalPages(totalPages);
		// ???????????????
		ArrayList<Integer> pageNavs = new ArrayList<>();
		for (int i = 1;i <= totalPages; i++){
			pageNavs.add(i);
		}
		result.setPageNavs(pageNavs);
		
		
		// 6.???????????????????????????
		if(param.getAttrs() != null){
			List<SearchResult.NavVo> navVos = param.getAttrs().stream().map(attr -> {
				SearchResult.NavVo navVo = new SearchResult.NavVo();
				String[] s = attr.split("_");
				navVo.setNavValue(s[1]);
				R r = productFeignService.getAttrsInfo(Long.parseLong(s[0]));
				// ??????????????????????????????????????? ?????????????????????????????????????????????????????????????????????
				result.getAttrIds().add(Long.parseLong(s[0]));
				if(r.getCode() == 0){
					AttrResponseVo data = r.getData(new TypeReference<AttrResponseVo>(){});
					navVo.setName(data.getAttrName());
				}else{
					// ???????????????id????????????
					navVo.setName(s[0]);
				}
				// ???????????????????????? ??????????????????
				//??????????????????????????????????????????url????????????????????????????????????
				String replace = replaceQueryString(param, attr, "attrs");
				navVo.setLink("http://search.gulmall.com/list.html?" + replace);
				return navVo;
			}).collect(Collectors.toList());
			result.setNavs(navVos);
		}
		
		
		// ???????????????????????????
		if(param.getBrandId() != null && param.getBrandId().size() > 0){
			List<SearchResult.NavVo> navs = result.getNavs();
			SearchResult.NavVo navVo = new SearchResult.NavVo();
			navVo.setName("??????");
			// TODO ????????????????????????
			R r = productFeignService.brandInfo(param.getBrandId());
			if(r.getCode() == 0){
				List<BrandVo> brand = r.getData("data", new TypeReference<List<BrandVo>>() {});
				StringBuffer buffer = new StringBuffer();
				// ??????????????????ID
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
	 * ????????????
	 * key ??????????????????key
	 */
	private String replaceQueryString(SearchParam Param, String value, String key) {
		String encode = null;
		try {
			encode = URLEncoder.encode(value,"UTF-8");
			// ??????????????????????????????java????????????
			encode = encode.replace("+","%20");
			encode = encode.replace("%28", "(").replace("%29",")");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return Param.get_queryString().replace("&" + key + "=" + encode, "");
	}
	

	/**
	 * @return
	 * ??????SearchRequest??????	
	 * ??????????????????  [??????????????????]
	 *
	 */
	private SearchRequest buildSearchRequest(SearchParam param) {
		
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		// 1. ???????????? ??????(??????????????????????????????????????????????????????) ?????????????????????Query
		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
		// 1.1 must
		if (!StringUtils.isEmpty(param.getKeyword())) {
			boolQuery.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
		}
		// 1.2 bool - filter Catalog3Id
		if (param.getCatelog3Id()!=null) {
			boolQuery.filter(QueryBuilders.termQuery("catalogId", param.getCatelog3Id()));
		}
		// 1.2 bool - brandId [??????]
		
		if (param.getBrandId()!=null && param.getBrandId().size()>0) {
			boolQuery.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
		}
		// ????????????
		//attrs=1_??????:??????&attrs=.
		if (param.getAttrs() != null && param.getAttrs().size() > 0) {
			for (String attrStr : param.getAttrs()) {
					BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
					String[] s = attrStr.split("_");
					// ?????????id  ?????????????????????
					String attrId = s[0];
					String[] attrValue = s[1].split(":");
					boolQueryBuilder.must(QueryBuilders.termQuery("attrs.attrId", attrId));
					boolQueryBuilder.must(QueryBuilders.termsQuery("attrs.attrValue", attrValue));
					// ?????????????????????Query ???????????????????????????????????? nested ??????
					NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", boolQueryBuilder, ScoreMode.None);
					boolQuery.filter(nestedQuery);
			}
		}
		// 1.2 bool - filter [??????]
		if (param.getHasStock()!=null) {
			boolQuery.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
		}
		
		// 1.2 bool - filter [????????????]
		if (!StringUtils.isEmpty(param.getSkuPrice())) {
			RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
			String[] s = param.getSkuPrice().split("_");
			if(s.length == 2){
				// ???????????? ????????????
				rangeQuery.gte(s[0]).lte(s[1]);
			}else if(s.length == 1){
				// ????????????
				if(param.getSkuPrice().startsWith("_")){
					rangeQuery.lte(s[0]);
				}
				if(param.getSkuPrice().endsWith("_")){
					rangeQuery.gte(s[0]);
				}
			}
			boolQuery.filter(rangeQuery);
		}
		//??????????????????????????????????????????
		searchSourceBuilder.query(boolQuery);
		// 1.??????
		if(!StringUtils.isEmpty(param.getSort())){
			String sort = param.getSort();
			String[] s1 = sort.split("_");
			SortOrder order = s1[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
			searchSourceBuilder.sort(s1[0],order);
		}
		// 2.?????? pageSize ??? 5
		searchSourceBuilder.from((param.getPageNum()-1)*EsConstant.PRODUCT_PASIZE);
		searchSourceBuilder.size(EsConstant.PRODUCT_PASIZE);
		
		// 3.??????
		HighlightBuilder builder = new HighlightBuilder();
		builder.field("skuTitle");
		builder.preTags("<b style='color:red'>");
		builder.postTags("</b>");
		searchSourceBuilder.highlighter(builder);
		
		// ????????????
		// TODO 1.????????????
		TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
		brand_agg.field("brandId").size(50);
		
		// ????????????????????????
		TermsAggregationBuilder brand_name = AggregationBuilders.terms("brand_name_agg").field("brandName").size(1); 
		TermsAggregationBuilder brand_img = AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1); 
		brand_agg.subAggregation(brand_name);
		brand_agg.subAggregation(brand_img);
		// ????????????????????? sourceBuilder
		searchSourceBuilder.aggregation(brand_agg);
		
		// TODO 2.????????????
		TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
		//????????????????????????
		TermsAggregationBuilder catalog_name_agg = AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1);
		catalog_agg.subAggregation(catalog_name_agg);
		// ????????????????????? sourceBuilder
		searchSourceBuilder.aggregation(catalog_agg);
		
		
		// TODO 3.???????????? attr_agg ?????????????????????
		NestedAggregationBuilder nested = AggregationBuilders.nested("attr_agg", "attrs");
		// 3.1 ????????????????????????attrId
		
		TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId").size(10);
		// 3.1.1 ?????????????????????attrId?????????attrName
		TermsAggregationBuilder attr_name_agg = AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1);
		// 3.1.2 ?????????????????????attrId?????????????????????????????????attrValue	???????????????????????????????????? ?????????50
		TermsAggregationBuilder attr_value_agg = AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50);
		attr_id_agg.subAggregation(attr_name_agg);
		attr_id_agg.subAggregation(attr_value_agg);
		// 3.2 ???????????????????????????????????????
		nested.subAggregation(attr_id_agg);
		searchSourceBuilder.aggregation(nested);
		//?????????dsl??????????????????
		//?????????????????????dsl??????
		log.info("\n???????????????->\n" + searchSourceBuilder.toString());
		//??????SearchRequest??????
		SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, searchSourceBuilder);
		return searchRequest;
		}
	
	
	
	
}