package com.atguigu.gulimall.product.app;

import java.util.Arrays;
import java.util.Map;

import com.atguigu.gulimall.product.vo.SpuSaveVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atguigu.gulimall.product.entity.SpuInfoEntity;
import com.atguigu.gulimall.product.service.SpuInfoService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;



/**
 * spu信息
 *
 * @author leifengyang
 * @email leifengyang@gmail.com
 * @date 2019-10-01 22:50:32
 */
@RestController
@RequestMapping("product/spuinfo")
public class SpuInfoController {
    @Autowired
    private SpuInfoService spuInfoService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:spuinfo:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = spuInfoService.queryPageByCondition(params);

        return R.ok().put("page", page);
    }
    
    @GetMapping("/skuId/{id}")
    public R getSkuInfoBySkuId(@PathVariable("id") Long skuId){

    	SpuInfoEntity entity = spuInfoService.getSpuInfoBySkuId(skuId);
    	return R.ok().setData(entity);
	}
    
	/* 
	 * 上架商品，向ES中保存数据
	 * 有两种数据模型
	 * 空间复杂度大，时间复杂度小
	 * 第一种，方便检索，{
	 * sku信息
	 * spuid
	 * attrs{
	 * 		{sku所属的spu信息}
	 * }
	 * 
	 * 空间复杂度小，时间复杂度大
	 * 第二种，内存占用较小，
	 * sku索引
	 * {
	 * sku信息
	 * spuid
	 * }
	 * 
	 * attr索引
	 * {
	 * spuid：spu信息
	 * ....
	 * }
	 * 考虑到高并发情况下第二种会产生更多的请求数据，使用第一种
	 * 例：在界面中一万人同时搜小米，检索得到的商品有1000，其中400是含有spuid的信息，则会产生400*8*10000字节信息，约等于32G的信息
	 *  */
	@PostMapping("/{spuId}/up")
	public R up(@PathVariable("spuId") Long spuId){
		spuInfoService.up(spuId);
		return R.ok();
	}
    

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		SpuInfoEntity spuInfo = spuInfoService.getById(id);

        return R.ok().put("spuInfo", spuInfo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:spuinfo:save")
    public R save(@RequestBody SpuSaveVo vo){
		//spuInfoService.save(spuInfo);

        spuInfoService.saveSpuInfo(vo);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:spuinfo:update")
    public R update(@RequestBody SpuInfoEntity spuInfo){
		spuInfoService.updateById(spuInfo);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:spuinfo:delete")
    public R delete(@RequestBody Long[] ids){
		spuInfoService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
