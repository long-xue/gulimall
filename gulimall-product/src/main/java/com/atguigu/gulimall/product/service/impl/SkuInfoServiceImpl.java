package com.atguigu.gulimall.product.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.dao.SkuInfoDao;
import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import com.atguigu.gulimall.product.feign.SeckillFeignService;
import com.atguigu.gulimall.product.service.AttrGroupService;
import com.atguigu.gulimall.product.service.SkuImagesService;
import com.atguigu.gulimall.product.service.SkuInfoService;
import com.atguigu.gulimall.product.service.SkuSaleAttrValueService;
import com.atguigu.gulimall.product.service.SpuInfoDescService;
import com.atguigu.gulimall.product.vo.ItemSaleAttrVo;
import com.atguigu.gulimall.product.vo.SeckillInfoVo;
import com.atguigu.gulimall.product.vo.SkuItemVo;
import com.atguigu.gulimall.product.vo.SpuItemAttrGroup;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {


	@Autowired
	private SkuImagesService imagesService;

	@Autowired
	private SpuInfoDescService spuInfoDescService;

	@Autowired
	private AttrGroupService attrGroupService;

	@Autowired
	private SkuSaleAttrValueService skuSaleAttrValueService;

	@Autowired
	private SeckillFeignService seckillFeignService;

	/**
	 * ?????????????????????
	 */
	@Autowired
	private ThreadPoolExecutor executor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

	@Override
	public void saveSkuInfo(SkuInfoEntity skuInfoEntity) {
		this.baseMapper.insert(skuInfoEntity);
	}

	/**
	 * SKU ??????????????????
	 * key: ??????
	 * catelogId: 225
	 * brandId: 2
	 * min: 2
	 * max: 2
	 */
	@Override
	public PageUtils queryPageByCondition(Map<String, Object> params) {

		QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();
		String key = (String) params.get("key");
		if(!StringUtils.isEmpty(key)){
			wrapper.and(w -> w.eq("sku_id", key).or().like("sku_name", key));
		}
		// ??????id?????????????????????????????????  ????????????????????????
		String catelogId = (String) params.get("catelogId");
		if(!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)){
			wrapper.eq("catalog_id", catelogId);
		}
		String brandId = (String) params.get("brandId");
		if(!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)){
			wrapper.eq("brand_id", brandId);
		}
		String min = (String) params.get("min");
		if(!StringUtils.isEmpty(min)){
			// gt : ??????;  ge: ????????????
			wrapper.ge("price", min);
		}
		String max = (String) params.get("max");
		if(!StringUtils.isEmpty(max)){
			try {
				BigDecimal bigDecimal = new BigDecimal(max);
				if(bigDecimal.compareTo(new BigDecimal("0")) == 1){
					// le: ????????????
					wrapper.le("price", max);
				}
			} catch (Exception e) {
				System.out.println("com.firenay.mall.product.service.impl.SkuInfoServiceImpl??????????????????????????????");
			}
		}
		IPage<SkuInfoEntity> page = this.page(
				new Query<SkuInfoEntity>().getPage(params),
				wrapper
		);

		return new PageUtils(page);
	}

	@Override
	public List<SkuInfoEntity> getSkuBySpuid(Long spuId) {
		return this.list(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId));
	}
	


	/**
	 * ????????????????????????
	 */
	@Override
	public SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException {

		SkuItemVo skuItemVo = new SkuItemVo();

		CompletableFuture<SkuInfoEntity> infoFutrue = CompletableFuture.supplyAsync(() -> {
			//1 sku????????????
			SkuInfoEntity info = getById(skuId);
			skuItemVo.setInfo(info);
			return info;
		}, executor);

		CompletableFuture<Void> ImgageFuture = CompletableFuture.runAsync(() -> {
			//2 sku????????????
			List<SkuImagesEntity> images = imagesService.getImagesBySkuId(skuId);
			skuItemVo.setImages(images);
		}, executor);

		CompletableFuture<Void> saleAttrFuture =infoFutrue.thenAcceptAsync(res -> {
			//3 ??????spu??????????????????
			List<ItemSaleAttrVo> saleAttrVos = skuSaleAttrValueService.getSaleAttrsBuSpuId(res.getSpuId());
			skuItemVo.setSaleAttr(saleAttrVos);
		},executor);

		CompletableFuture<Void> descFuture = infoFutrue.thenAcceptAsync(res -> {
			//4 ??????spu??????
			SpuInfoDescEntity spuInfo = spuInfoDescService.getById(res.getSpuId());
			skuItemVo.setDesc(spuInfo);
		},executor);

		CompletableFuture<Void> baseAttrFuture = infoFutrue.thenAcceptAsync(res -> {
			//5 ??????spu??????????????????
			List<SpuItemAttrGroup> attrGroups = attrGroupService.getAttrGroupWithAttrsBySpuId(res.getSpuId(), res.getCatalogId());
			skuItemVo.setGroupAttrs(attrGroups);
		}, executor);

		// 6.????????????sku????????????????????????
		CompletableFuture<Void> secKillFuture = CompletableFuture.runAsync(() -> {
			R skuSeckillInfo = seckillFeignService.getSkuSeckillInfo(skuId);
			if (skuSeckillInfo.getCode() == 0) {
				SeckillInfoVo seckillInfoVo = skuSeckillInfo.getData(new TypeReference<SeckillInfoVo>() {});
				skuItemVo.setSeckillInfoVo(seckillInfoVo);
			}
		}, executor);

		// ????????????????????????????????????
		CompletableFuture.allOf(ImgageFuture,saleAttrFuture,descFuture,baseAttrFuture,secKillFuture).get();
		return skuItemVo;
	}



}