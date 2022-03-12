package com.atguigu.gulimall.cart.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.cart.feign.ProductFeignService;
import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.Cart;
import com.atguigu.gulimall.cart.vo.CartItem;
import com.atguigu.gulimall.cart.vo.SkuInfoVo;
import com.atguigu.gulimall.cart.vo.UserInfoTo;

import lombok.extern.slf4j.Slf4j;


/**
 * @longxue
 *
 * 
 */
@Slf4j
@Service
public class CartServiceImpl implements CartService {

	@Autowired
	private StringRedisTemplate stringRedisTemplate;
	
	@Autowired
	ProductFeignService productFeignService;
	
	@Autowired
	private ThreadPoolExecutor executor;
	
	private final String CART_PREFIX = "FIRE:cart:";
	
	/**
	 * 获取购物车
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	@Override
	public Cart getCart() throws InterruptedException, ExecutionException {
		//获取当前用户信息
		UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
		Cart cart = new Cart();
		// 临时购物车的在redis中的key
		String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();
		//有以下几种情况
		//1.用户登录了 临时购物车没有数据 
		//2.用户登录了 临时购物车有数据 需要合并数据
		//3.用户没登录 获取临时购物车数据进行展示
		if (userInfoTo.getUserId() != null) {
			//用户登录了
			//用户的key
			String cartKey = CART_PREFIX + userInfoTo.getUserId();
			//获取临时用户购物车
			List<CartItem> tempItem = getCartItems(tempCartKey);
			//如果临时购物车有数据
			if (tempItem != null) {
				//进行合并
				log.info("\n[" + userInfoTo.getUsername() + "] 的购物车已合并");
				for (CartItem cartItem : tempItem) {
					//调用addtoCart方法合并购物车
						addToCart(cartItem.getSkuId(), cartItem.getCount());
				}
				//合并之后需要清空临时购物车
				clearCart(tempCartKey);
			}
			//如果临时购物车有数据，则已添加到用户购物车中，如果临时购物车无数据，则直接返回用户购物车数据
			List<CartItem> cartItems = getCartItems(cartKey);
			cart.setItems(cartItems);
		}
		//如果用户没有登录
		else {
			//返回临时购物车数据
			cart.setItems(getCartItems(tempCartKey));
		}
		return cart;
	}

	
	public void clearCart(String tempCartKey) {

		stringRedisTemplate.delete(tempCartKey);
	}
	
	@Override
	public CartItem addToCart(Long skuId, Integer num) throws InterruptedException, ExecutionException {
		BoundHashOperations<String, Object, Object> cartOps = getCartOps();
		String res = (String) cartOps.get(skuId.toString());
		if(StringUtils.isEmpty(res)){
			CartItem cartItem = new CartItem();
			// 异步编排
			CompletableFuture<Void> getSkuInfo = CompletableFuture.runAsync(() -> {
				// 1. 远程查询当前要添加的商品的信息
				R skuInfo = productFeignService.SkuInfo(skuId);
				SkuInfoVo sku = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {});
				// 2. 添加新商品到购物车
				cartItem.setCount(num);
				cartItem.setCheck(true);
				cartItem.setImage(sku.getSkuDefaultImg());
				cartItem.setPrice(sku.getPrice());
				cartItem.setTitle(sku.getSkuTitle());
				cartItem.setSkuId(skuId);
			}, executor);

			// 3. 远程查询sku组合信息
			CompletableFuture<Void> getSkuSaleAttrValues = CompletableFuture.runAsync(() -> {
				List<String> values = productFeignService.getSkuSaleAttrValues(skuId);
				cartItem.setSkuAttr(values);
			}, executor);
			CompletableFuture.allOf(getSkuInfo, getSkuSaleAttrValues).get();
			cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
			return cartItem;
		}else{
			//如果redis中的数据已经有购物车要添加的商品信息，则只增加相应商品的数量num
			CartItem cartItem = JSON.parseObject(res, CartItem.class);
			cartItem.setCount(cartItem.getCount() + num);
			cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
			return cartItem;
		}
		
	}

	
	/**
	 * @param cartKey
	 * @return 某个用户购物车的所有购物项信息
	 */
	private List<CartItem> getCartItems(String cartKey) {
		BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(cartKey);
		List<Object> values = hashOps.values();
		if(values != null && values.size() > 0){
			return values.stream().map(obj -> JSON.parseObject((String) obj, CartItem.class)).collect(Collectors.toList());
		}
		return null;
	}

	@Override
	public List<CartItem> getUserCartItems() {
		UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
		if(userInfoTo.getUserId() == null){
			return null;
		}else{
			String cartKey = CART_PREFIX + userInfoTo.getUserId();
			List<CartItem> cartItems = getCartItems(cartKey);
			// 获取所有被选中的购物项
			List<CartItem> collect = cartItems.stream().filter(item -> item.getCheck()).map(item -> {
				try {
					//TODO 为什么要远程查询price
					R r = productFeignService.getPrice(item.getSkuId());
					String price = (String) r.get("data");
					item.setPrice(new BigDecimal(price));
				} catch (Exception e) {
					log.warn("远程查询商品价格出错 [商品服务未启动]");
				}
				return item;
			}).collect(Collectors.toList());
			return collect;
		}
	}
	
	 /* 获取到我们要操作的购物车 [已经包含用户前缀 只需要带上用户id 或者临时id 就能对购物车进行操作]
			 */
	private BoundHashOperations<String, Object, Object> getCartOps() {
		UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
		// 1. 这里我们需要知道操作的是离线购物车还是在线购物车
		String cartKey = CART_PREFIX;
		if(userInfoTo.getUserId() != null){
			log.debug("\n用户 [" + userInfoTo.getUsername() + "] 正在操作购物车");
			// 已登录的用户购物车的标识
			cartKey += userInfoTo.getUserId();
		}else{
			log.debug("\n临时用户 [" + userInfoTo.getUserKey() + "] 正在操作购物车");
			// 未登录的用户购物车的标识
			cartKey += userInfoTo.getUserKey();
		}
		// 绑定这个 key 以后所有对redis 的操作都是针对这个key
		return stringRedisTemplate.boundHashOps(cartKey);
	}

	
	/**
	 * 根据skuid获取单个购物项对象
	 */
	@Override
	public CartItem getCartItem(Long skuId) {
		BoundHashOperations<String, Object, Object> cartOps = getCartOps();
		String o = (String) cartOps.get(skuId.toString());
		return JSON.parseObject(o, CartItem.class);
	}

	/**
	 * 勾选购物项
	 */
	@Override
	
	public void checkItem(Long skuId, Integer check) {
		// 获取要选中的购物项
		CartItem cartItem = getCartItem(skuId);
		cartItem.setCheck(check==1?true:false);
		BoundHashOperations<String, Object, Object> cartOps = getCartOps();
		cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
	}

	/**
	 * 改变购物车商品数量
	 */
	@Override
	public void changeItemCount(Long skuId, Integer num) {
		CartItem cartItem = getCartItem(skuId);
		cartItem.setCount(num);
		BoundHashOperations<String, Object, Object> cartOps = getCartOps();
		cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
	}

	@Override
	public void deleteItem(Long skuId) {
		BoundHashOperations<String, Object, Object> cartOps = getCartOps();
		cartOps.delete(skuId.toString());
	}

	/**
	 * 返回总价格
	 */
	@Override
	public BigDecimal toTrade() throws ExecutionException, InterruptedException {
		BigDecimal amount = getCart().getTotalAmount();
		UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
		stringRedisTemplate.delete(CART_PREFIX + (userInfoTo.getUserId() != null ? userInfoTo.getUserId().toString() : userInfoTo.getUserKey()));
		return amount;
	}
		
}
