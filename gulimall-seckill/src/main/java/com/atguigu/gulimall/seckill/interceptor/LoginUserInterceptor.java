package com.atguigu.gulimall.seckill.interceptor;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.vo.MemberRsepVo;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * <p>Title: LoginUserInterceptor</p>
 * Description：
 * date：2020/7/9 15:58
 */

public class LoginUserInterceptor implements HandlerInterceptor {

	public static ThreadLocal<MemberRsepVo> threadLocal = new ThreadLocal<>();

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

		String uri = request.getRequestURI();
		// 这个请求直接放行
		boolean match = new AntPathMatcher().match("/kill", uri);
		if (match) {
			HttpSession session = request.getSession();
			Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
			MemberRsepVo memberRsepVo = (MemberRsepVo) session.getAttribute(AuthServerConstant.LOGIN_USER);

			if (memberRsepVo != null) {
				threadLocal.set(memberRsepVo);
				return true;
			} else {
				// 没登陆就去登录
				session.setAttribute("msg", AuthServerConstant.NOT_LOGIN);
				response.sendRedirect("http://auth.gulimall.com/login.html");
				return false;
			}
		}
		return true;
	}
}
