package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRsepVo;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.vo.SocialUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/oauth2.0")
public class Oauth2Controller {

	@Autowired
	MemberFeignService memberFeignService;
	
	
	/**
	 * 登录成功回调
	 * {
	 *     "access_token": "2.00b5w4HGbwxc6B0e3d62c666DlN1DD",
	 *     "remind_in": "157679999",
	 *     "expires_in": 157679999,
	 *     "uid": "5605937365",
	 *     "isRealName": "true"
	 * }
	 * @throws Exception 
	 */
	@GetMapping("/weibo/success")
	public String weibo(@RequestParam("code") String code, HttpSession session) throws Exception {
		
		//根据微博传回来的code获取令牌
		Map<String,String> map = new HashMap<String, String>();
		map.put("client_id", "1294828100");
		map.put("client_secret", "a8e8900e15fba6077591cdfa3105af44");
		map.put("grant_type", "authorization_code");
		map.put("redirect_uri", "http://auth.gulimall.com/oauth2.0/weibo/success");
		map.put("code", code);
		//传入空请求头
		Map<String, String> headers = new HashMap<>();
		HttpResponse response = HttpUtils.doPost("https://api.weibo.com", "/oauth2/access_token", "post", headers, null, map);
		if (response.getStatusLine().getStatusCode() == 200) {
			//获取令牌Access Token成功
			String json = EntityUtils.toString(response.getEntity());
			SocialUser socialUser = JSON.parseObject(json, SocialUser.class);
			//我们获取到了当前登录用户在微博的信息
			//1.如果用户是第一次进来，自动注册(为当前社交用户生成一个会员信息 以后这个账户就会关联这个账号)
			R login = memberFeignService.login(socialUser);
			if (login.getCode() == 0) {
				MemberRsepVo rsepVo = login.getData("data", new TypeReference<MemberRsepVo>() {});
				log.info("\n欢迎 [" + rsepVo.getUsername() + "] 使用社交账号登录");
				session.setAttribute(AuthServerConstant.LOGIN_USER, rsepVo);
				// 登录成功 跳回首页
				return "redirect:http://gulimall.com";
			}else{
				return "redirect:http://auth.gulimall.com/login.html";
			}
		}else{
			return "redirect:http://auth.gulimall.com/login.html";
		}
	}
}
