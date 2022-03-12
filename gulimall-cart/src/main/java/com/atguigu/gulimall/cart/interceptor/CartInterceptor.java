package com.atguigu.gulimall.cart.interceptor;

import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.constant.CartConstant;
import com.atguigu.common.vo.MemberRsepVo;
import com.atguigu.gulimall.cart.vo.UserInfoTo;



/**
 * @longxue
 *	配置一个拦截器
 *  无论是临时用户还是已登录用户，都需要通过这个拦截器
 *  拦截器处理之后返回一个封装好的vo对象传给Controller
 * 
 */
public class CartInterceptor implements HandlerInterceptor {

	public static ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<>();

	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		//封装好的用户对象，无论用户状态如何，都会分配一个user-key用来标识用户在redis，并且如果第一次进来网站，没有user-key的用户还会自动分配
		UserInfoTo userInfoTo = new UserInfoTo();
		HttpSession session = request.getSession();
		MemberRsepVo user = (MemberRsepVo) session.getAttribute(AuthServerConstant.LOGIN_USER);
		//判断用户登录状态
		if (user != null) {
			//已登录，分配userId
			userInfoTo.setUserId(user.getId());
			userInfoTo.setUsername(user.getUsername());
		}
		//如果cookie中带了user-key。就不用了为用户分配user-key
		Cookie[] cookies = request.getCookies();
		if (cookies != null & cookies.length>0) {
			for (Cookie cookie : cookies) {
				   	String name = cookie.getName();
				   	if (name.equals(CartConstant.TEMP_USER_COOKIE_NAME)) {
				   		userInfoTo.setUserKey(cookie.getValue());
						userInfoTo.setTempUser(true);
					}
			}
		}
		//如果没有user-key则分配一个user-key
		if (StringUtils.isEmpty(userInfoTo.getUserKey())){
			String uuid = UUID.randomUUID().toString().replace("-","");
			userInfoTo.setUserKey("FIRE-" + uuid);
		}
		
		//通过threadLocal共享这个线程的数据，让这个线程的其他组件能够拿到数据
		threadLocal.set(userInfoTo);
		return true;
	}
	/**
	 * 执行完毕之后分配临时用户让浏览器保存
	 */
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

		UserInfoTo userInfoTo = threadLocal.get();
		if(!userInfoTo.isTempUser()){
			Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
			// 设置这个cookie作用域 过期时间
			cookie.setDomain("gulimall.com");
			cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_TIME_OUT);
			response.addCookie(cookie);
		}
	}
}
	

