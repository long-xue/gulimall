package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catalog3Vo;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

//    @Autowired
//    CategoryDao categoryDao;

	@Resource
	private RedissonClient redissonClient;
	
    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;
    
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1?????????????????????
        List<CategoryEntity> entities = baseMapper.selectList(null);

        //2?????????????????????????????????

        //2.1?????????????????????????????????
        List<CategoryEntity> level1Menus = entities.stream().filter(categoryEntity ->
             categoryEntity.getParentCid() == 0
        ).map((menu)->{
            menu.setChildren(getChildrens(menu,entities));
            return menu;
        }).sorted((menu1,menu2)->{
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());

        return level1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO  1????????????????????????????????????????????????????????????

        //????????????
        baseMapper.deleteBatchIds(asList);
    }

    //[2,25,225]
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);

        Collections.reverse(parentPath);


        return parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * ?????????????????????????????????
     * @param category
     */
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());
    }

    //225,25,2
    private List<Long> findParentPath(Long catelogId,List<Long> paths){
        //1?????????????????????id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if(byId.getParentCid()!=0){
            findParentPath(byId.getParentCid(),paths);
        }
        return paths;

    }


    //????????????????????????????????????
    private List<CategoryEntity> getChildrens(CategoryEntity root,List<CategoryEntity> all){

        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            //1??????????????????
            categoryEntity.setChildren(getChildrens(categoryEntity,all));
            return categoryEntity;
        }).sorted((menu1,menu2)->{
            //2??????????????????
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());

        return children;
    }

    /* 
	?????????????????????parent_cid?????????????????????????????????Entitys
	
	 */
	private List<CategoryEntity> getCategoryEntities(List<CategoryEntity> entityList, Long parent_cid) {

		return entityList.stream().filter(item -> item.getParentCid() == parent_cid).collect(Collectors.toList());
	}
    
	@Override
	public List<CategoryEntity> getLevel1Categorys() {
		//???????????????????????????
		List<CategoryEntity> selectList = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("cat_level", 1));
		return selectList;
	}
	
	/**
	 * redis???????????? ??????DB [?????????????????????]
	 * ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????
	 */
	public Map<String, List<Catelog2Vo>> getCatelogJsonFromDBWithLocalLock() {

		synchronized (this) {
			// ???????????? ???????????????
			return getDataFromDB();
		}
	}
	
	/**
	 * redis????????? ???????????????
		?????????????????????
	 */
	private Map<String, List<Catelog2Vo>> getDataFromDB() {
		String catelogJSON = stringRedisTemplate.opsForValue().get("catelogJSON");
		if (!StringUtils.isEmpty(catelogJSON)) {
			return JSON.parseObject(catelogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
			});
		}
		// ??????????????????????????????
		List<CategoryEntity> entityList = baseMapper.selectList(null);

		// ????????????????????????
		List<CategoryEntity> level1 = getCategoryEntities(entityList, 0L);
		Map<String, List<Catelog2Vo>> parent_cid = level1.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
			// ??????????????????????????? ?????????????????????????????????
			List<CategoryEntity> entities = getCategoryEntities(entityList, v.getCatId());
			List<Catelog2Vo> catelog2Vos = null;
			if (entities != null) {
				catelog2Vos = entities.stream().map(l2 -> {
					Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), l2.getName(), l2.getCatId().toString(), null);
					// ????????????????????????????????????
					List<CategoryEntity> level3 = getCategoryEntities(entityList, l2.getCatId());
					// ?????????????????????????????????
					if (level3 != null) {
						List<Catalog3Vo> catalog3Vos = level3.stream().map(l3 -> new Catalog3Vo(l3.getCatId().toString(), l3.getName(), l2.getCatId().toString())).collect(Collectors.toList());
						catelog2Vo.setCatalog3List(catalog3Vos);
					}
					return catelog2Vo;
				}).collect(Collectors.toList());
			}
			return catelog2Vos;
		}));
		// ??????????????????????????????????????????????????????????????????
		stringRedisTemplate.opsForValue().set("catelogJSON", JSON.toJSONString(parent_cid), 1, TimeUnit.DAYS);
		return parent_cid;
	}
	
	/**
	 * ????????????
	 *
	 * @return
	 */
	public Map<String, List<Catelog2Vo>> getCatelogJsonFromDBWithRedisLock() {
		// 1.???????????????  ???????????????10??????????????? [????????????]
		// 1.1 ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
		//uuid??????????????????????????????????????????????????????uuid?????????????????????????????????????????????????????????????????????
		String uuid = UUID.randomUUID().toString();
		Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid, 30, TimeUnit.SECONDS);

		if (lock) {
			// 2.?????????????????????????????? ????????????????????? [?????????????????????Lua????????????,????????????????????????????????????????????????????????????????????????????????????????????????????????????????????? ???????????????????????????]
			Map<String, List<Catelog2Vo>> data;
			try {
				data = getDataFromDB();
			} finally {
//			stringRedisTemplate.delete("lock");
				String lockValue = stringRedisTemplate.opsForValue().get("lock");
				//???????????????????????????????????????????????????????????????
				// ?????????????????????????????? Lua???????????? ??????????????????1 ????????????0
				String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
				// ????????????
				stringRedisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList("lock"), uuid);
			}
			return data;
		} else {
			// ????????????
			ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
			Map<String, List<Catelog2Vo>> catelogMapJson = null;
			// ?????????????????????
			String catelogJSON = operations.get("catelogJSON");
			//?????????????????????
			while (StringUtils.isEmpty(catelogJSON)) {
				getCatelogJsonFromDBWithRedisLock();
			}
			catelogMapJson = JSON.parseObject(catelogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
				});
			return catelogMapJson;
		}
	}
	
	/**
	 * redisson ??????????????????
	 * ????????????????????????????????????????????????
	 */
	public Map<String, List<Catelog2Vo>> getCatelogJsonFromDBWithRedissonLock() {
		// ???????????????????????????????????????????????????
		// ?????????????????? ?????????????????????????????? ??????: 11-????????? product-11-lock
		RLock lock = redissonClient.getLock("CatelogJson-lock");
		//???????????????
		//?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
		//??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
		//1.????????????????????????????????????????????????redis?????????????????????????????????????????????????????????????????????
		//2.???????????????????????????????????????????????????redisson???????????????????????????30*1000???LockWatchdogTimeout???????????????????????????
		//	????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????10S???1/3??????????????????????????????????????????????????????
//		lock.lock(10, TimeUnit.SECONDS);//????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
		lock.lock();
		Map<String, List<Catelog2Vo>> data;
		try {
			data = getDataFromDB();
		} finally {
			lock.unlock();
		}
		return data;
	}
	
	
	/**
	 * ????????????????????????????????????JSON
	 * TODO ????????????????????????????????? OutOfDirectMemoryError
	 * springBoot2.0??????????????????lettuce????????????redis???????????? lettuce??????netty??????????????????
	 * lettuce???bug??????netty??????????????????OutOfDirectMemoryError netty?????????????????????????????? ????????????-Xmx???????????????
	 * lettuce+netty???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
	 *  ????????????-Dio.netty.maxDirectMemory??????netty???????????????
	 *  
	 *  ???????????????
	 *  1.??????lettuce????????? lettuce??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
	 * 
	 *  2.????????????redis????????????  jedis
	 */
	public Map<String, List<Catelog2Vo>> getCatelogJson2() {
		/**
		 * 1.??????????????? ??????????????????
		 * 2.?????????????????? ??????????????????
		 * 3.?????? ??????????????????
		 */
		ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
		Map<String, List<Catelog2Vo>> catelogJson;
		// ?????????????????????
		String catelogJSON = operations.get("catelogJSON");
		if (StringUtils.isEmpty(catelogJSON)) {
			//??????????????????
			catelogJson = getCatelogJsonFromDBWithRedisLock();
		} else {
			catelogJson = JSON.parseObject(catelogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
			});
		}
		return catelogJson;
	}
	
	@Cacheable(value = "category", key = "#root.methodName")
	@Override
	public Map<String, List<Catelog2Vo>> getCatelogJson() {
		
		//??????????????????????????????????????????
		List<CategoryEntity> entityList = baseMapper.selectList(null);
		// ????????????????????????
		List<CategoryEntity> level1 = getCategoryEntities(entityList, 0L);
		//?????????????????? key???????????????id value???????????????????????????????????????????????????????????????
		Map<String, List<Catelog2Vo>> parent_cid = level1.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
			// ??????????????????????????? ?????????????????????????????????
			List<CategoryEntity> entities = getCategoryEntities(entityList, v.getCatId());
			//?????????????????????????????????
			List<Catelog2Vo> catelog2Vos = null;
			if (entities != null) {
				catelog2Vos = entities.stream().map(l2 -> {
					Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), l2.getName(), l2.getCatId().toString(), null);
					// ????????????????????????????????????
					List<CategoryEntity> level3 = getCategoryEntities(entityList, l2.getCatId());
					// ?????????????????????????????????
					if (level3 != null) {
						List<Catalog3Vo> catalog3Vos = level3.stream().map(l3 -> new Catalog3Vo(l3.getCatId().toString(), l3.getName(), l2.getCatId().toString())).collect(Collectors.toList());
						catelog2Vo.setCatalog3List(catalog3Vos);
					}
					return catelog2Vo;
				}).collect(Collectors.toList());
			}
			//?????????????????????map??????value
			return catelog2Vos;
		}));
		return parent_cid;
		
		
	}



}