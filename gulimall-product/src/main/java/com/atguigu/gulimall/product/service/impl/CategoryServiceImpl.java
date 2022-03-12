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
        //1、查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        //2、组装成父子的树形结构

        //2.1）、找到所有的一级分类
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
        //TODO  1、检查当前删除的菜单，是否被别的地方引用

        //逻辑删除
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
     * 级联更新所有关联的数据
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
        //1、收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if(byId.getParentCid()!=0){
            findParentPath(byId.getParentCid(),paths);
        }
        return paths;

    }


    //递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root,List<CategoryEntity> all){

        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            //1、找到子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity,all));
            return categoryEntity;
        }).sorted((menu1,menu2)->{
            //2、菜单的排序
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());

        return children;
    }

    /* 
	此方法用来根据parent_cid找到相对应的某级分类的Entitys
	
	 */
	private List<CategoryEntity> getCategoryEntities(List<CategoryEntity> entityList, Long parent_cid) {

		return entityList.stream().filter(item -> item.getParentCid() == parent_cid).collect(Collectors.toList());
	}
    
	@Override
	public List<CategoryEntity> getLevel1Categorys() {
		//得到一級分类的数据
		List<CategoryEntity> selectList = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("cat_level", 1));
		return selectList;
	}
	
	/**
	 * redis没有数据 查询DB [本地锁解决方案]
	 * 本地锁在分布式情况下无法锁住所有机器，在数据不是强一致性的情况下可以用本地锁
	 */
	public Map<String, List<Catelog2Vo>> getCatelogJsonFromDBWithLocalLock() {

		synchronized (this) {
			// 双重检查 是否有缓存
			return getDataFromDB();
		}
	}
	
	/**
	 * redis无缓存 查询数据库
		没加锁的情况下
	 */
	private Map<String, List<Catelog2Vo>> getDataFromDB() {
		String catelogJSON = stringRedisTemplate.opsForValue().get("catelogJSON");
		if (!StringUtils.isEmpty(catelogJSON)) {
			return JSON.parseObject(catelogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
			});
		}
		// 优化：将查询变为一次
		List<CategoryEntity> entityList = baseMapper.selectList(null);

		// 查询所有一级分类
		List<CategoryEntity> level1 = getCategoryEntities(entityList, 0L);
		Map<String, List<Catelog2Vo>> parent_cid = level1.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
			// 拿到每一个一级分类 然后查询他们的二级分类
			List<CategoryEntity> entities = getCategoryEntities(entityList, v.getCatId());
			List<Catelog2Vo> catelog2Vos = null;
			if (entities != null) {
				catelog2Vos = entities.stream().map(l2 -> {
					Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), l2.getName(), l2.getCatId().toString(), null);
					// 找当前二级分类的三级分类
					List<CategoryEntity> level3 = getCategoryEntities(entityList, l2.getCatId());
					// 三级分类有数据的情况下
					if (level3 != null) {
						List<Catalog3Vo> catalog3Vos = level3.stream().map(l3 -> new Catalog3Vo(l3.getCatId().toString(), l3.getName(), l2.getCatId().toString())).collect(Collectors.toList());
						catelog2Vo.setCatalog3List(catalog3Vos);
					}
					return catelog2Vo;
				}).collect(Collectors.toList());
			}
			return catelog2Vos;
		}));
		// 优化：查询到数据库就再锁还没结束之前放入缓存
		stringRedisTemplate.opsForValue().set("catelogJSON", JSON.toJSONString(parent_cid), 1, TimeUnit.DAYS);
		return parent_cid;
	}
	
	/**
	 * 分布式锁
	 *
	 * @return
	 */
	public Map<String, List<Catelog2Vo>> getCatelogJsonFromDBWithRedisLock() {
		// 1.占分布式锁  设置这个锁10秒自动删除 [原子操作]
		// 1.1 占坑操作和设置时间必须是原子操作，否则可能会由于特殊情况导致占坑成功但没设置时间的结果
		//uuid是保证删锁删的是本线程的锁，如果不加uuid，可能会导致最后删锁操作删除的是其他线程加的锁
		String uuid = UUID.randomUUID().toString();
		Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid, 30, TimeUnit.SECONDS);

		if (lock) {
			// 2.设置过期时间加锁成功 获取数据释放锁 [分布式下必须是Lua脚本删锁,不然会因为业务处理时间、网络延迟等等引起数据还没返回锁过期或者返回的过程中过期 然后把别人的锁删了]
			Map<String, List<Catelog2Vo>> data;
			try {
				data = getDataFromDB();
			} finally {
//			stringRedisTemplate.delete("lock");
				String lockValue = stringRedisTemplate.opsForValue().get("lock");
				//删除也必须保证获取值和删除操作是原子性的，
				// 删除也必须是原子操作 Lua脚本操作 删除成功返回1 否则返回0
				String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
				// 原子删锁
				stringRedisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList("lock"), uuid);
			}
			return data;
		} else {
			// 重试加锁
			ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
			Map<String, List<Catelog2Vo>> catelogMapJson = null;
			// 缓存中没有数据
			String catelogJSON = operations.get("catelogJSON");
			//等待采用自旋锁
			while (StringUtils.isEmpty(catelogJSON)) {
				getCatelogJsonFromDBWithRedisLock();
			}
			catelogMapJson = JSON.parseObject(catelogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
				});
			return catelogMapJson;
		}
	}
	
	/**
	 * redisson 微服务集群锁
	 * 缓存中的数据如何与数据库保持一致
	 */
	public Map<String, List<Catelog2Vo>> getCatelogJsonFromDBWithRedissonLock() {
		// 这里只要锁的名字一样那锁就是一样的
		// 关于锁的粒度 具体缓存的是某个数据 例如: 11-号商品 product-11-lock
		RLock lock = redissonClient.getLock("CatelogJson-lock");
		//阻塞式等待
		//锁的自动续期，如果业务时间过长，运行期间自动给锁续期，不用担心业务执行时间过长，锁自动删除。看门狗机制
		//加锁的业务只要运行完成，就不会给当前锁续期，即使不手动解锁，时间到了也会自动解锁，不必担心死锁的问题
		//1.如果传递了锁的超时时间，就发送给redis执行脚本，进行占锁，默认超时就是我们设置的时间
		//2.如果没有传递锁的超时时间，就会使用redisson设置的时间，默认是30*1000【LockWatchdogTimeout看门狗的默认时间】
		//	只要占锁成功，就会执行一个定时任务【重新给锁设置过期时间，新的过期时间默认是看门狗的时间】，每隔10S【1/3的看门狗时间】都会自动的执行这个任务
//		lock.lock(10, TimeUnit.SECONDS);//这个方法不会自动续期，所以设置的超时时间必须大于业务执行时间，不然会导致解锁失败
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
	 * 缓存中存的所有字符串都是JSON
	 * TODO 可能会产生堆外内存溢出 OutOfDirectMemoryError
	 * springBoot2.0以后默认使用lettuce作为操作redis的客户端 lettuce使用netty进行网络通信
	 * lettuce的bug导致netty堆外内存溢出OutOfDirectMemoryError netty如果没有指定堆外内存 默认使用-Xmx指定的内存
	 * lettuce+netty并没有进行回收堆外内存，所以无论设置多大的堆外内存，都会报异常，区别只是时间长短；
	 *  可以通过-Dio.netty.maxDirectMemory设置netty的堆外内存
	 *  
	 *  解决方案：
	 *  1.升级lettuce客户端 lettuce没有及时释放连接以及连接涉及的资源，导致堆外异常，并发量上来后，多个连接由于没有得到及时释放，会导致堆外异常
	 * 
	 *  2.切换操作redis的客户端  jedis
	 */
	public Map<String, List<Catelog2Vo>> getCatelogJson2() {
		/**
		 * 1.空结果缓存 解决缓存穿透
		 * 2.设置过期时间 解决缓存雪崩
		 * 3.加锁 解决缓存击穿
		 */
		ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
		Map<String, List<Catelog2Vo>> catelogJson;
		// 缓存中没有数据
		String catelogJSON = operations.get("catelogJSON");
		if (StringUtils.isEmpty(catelogJSON)) {
			//采用分布式锁
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
		
		//查找到数据库中所有的分类记录
		List<CategoryEntity> entityList = baseMapper.selectList(null);
		// 查询所有一级分类
		List<CategoryEntity> level1 = getCategoryEntities(entityList, 0L);
		//构造返回数据 key是一级分类id value是二级分类数组，其中二级分类又包含三级分类
		Map<String, List<Catelog2Vo>> parent_cid = level1.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
			// 拿到每一个一级分类 然后查询他们的二级分类
			List<CategoryEntity> entities = getCategoryEntities(entityList, v.getCatId());
			//构造返回的耳机分类数据
			List<Catelog2Vo> catelog2Vos = null;
			if (entities != null) {
				catelog2Vos = entities.stream().map(l2 -> {
					Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), l2.getName(), l2.getCatId().toString(), null);
					// 找当前二级分类的三级分类
					List<CategoryEntity> level3 = getCategoryEntities(entityList, l2.getCatId());
					// 三级分类有数据的情况下
					if (level3 != null) {
						List<Catalog3Vo> catalog3Vos = level3.stream().map(l3 -> new Catalog3Vo(l3.getCatId().toString(), l3.getName(), l2.getCatId().toString())).collect(Collectors.toList());
						catelog2Vo.setCatalog3List(catalog3Vos);
					}
					return catelog2Vo;
				}).collect(Collectors.toList());
			}
			//这个就是返回的map里的value
			return catelog2Vos;
		}));
		return parent_cid;
		
		
	}



}