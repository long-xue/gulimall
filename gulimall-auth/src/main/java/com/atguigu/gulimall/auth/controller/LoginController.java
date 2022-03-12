package com.atguigu.gulimall.auth.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRsepVo;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.feign.ThirdPartFeignService;
import com.atguigu.gulimall.auth.vo.UserLoginVo;
import com.atguigu.gulimall.auth.vo.UserRegisterVo;

import lombok.extern.slf4j.Slf4j;

/**
 * @longxue
 *	登录注册模块
 * 
 */
/**
 * @longxue
 *
 * 
 */
@Controller
@Slf4j
public class LoginController {

	@Autowired
	ThirdPartFeignService thirdPartFeignService;
	
	@Autowired
	MemberFeignService memberFeignService;
	
	@Autowired
	StringRedisTemplate stringRedisTemplate;
	
	@GetMapping({"/login.html","/","/index","/index.html"})
	public String loginPage(HttpSession session) {
		Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
		if(attribute == null){
			return "login";
		}
		return "redirect:http://gulimall.com";
	}
	
	@PostMapping("/login")
	public String login(UserLoginVo userLoginVo, RedirectAttributes redirectAttributes, HttpSession session) {
		
		// 调用会员服务远程登录
		R r = memberFeignService.login(userLoginVo);
		if (r.getCode() == 0) {
			//登录成功
			MemberRsepVo data = r.getData("data", new TypeReference<MemberRsepVo>() {});
			session.setAttribute(AuthServerConstant.LOGIN_USER, data);
			log.info("\n欢迎 [" + data.getUsername() + "] 登录");
			return "redirect:http://gulimall.com";
		}
		else {
			HashMap<String, String> error = new HashMap<String, String>();
			String data = r.getData("msg", new TypeReference<String>(){});
			error.put("msg", data);
			redirectAttributes.addFlashAttribute("msg", error);
			return "redirect:http://auth.gulimall.com/login.html";
			
		}
	}
	
	
	@ResponseBody
	@GetMapping("/sms/snedcode")
	public R senCode(@RequestParam("phone") String phone) {
	
	// TODO 接口防刷
	String redisCode = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
	if (null != redisCode && redisCode.length() > 0) {
		long CuuTime = Long.parseLong(redisCode.split("_")[1]);
		if(System.currentTimeMillis() - CuuTime < 60 * 1000){
			return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(), BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
		}
	}
	//正常情况
	String code = UUID.randomUUID().toString().substring(0, 6);	
	String redis_code = code + "_" + System.currentTimeMillis();
	// 缓存验证码
	stringRedisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, redis_code, 10, TimeUnit.MINUTES);

	try {
		return thirdPartFeignService.sendCode(phone, code);
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		log.error("远程调用发送验证码错误");
	}	
	return R.ok();
	}		
	
	
	/**
	 * TODO 重定向携带数据原理,利用session原理 将数据放在sessoin中 取一次之后删掉
	 *	webMvc配置的视图解析器只支持get方式的请求
	 * 
	 * TODO 1. 分布式下的session问题 session不同步问题
	 * 校验
	 * 
	 * RedirectAttributes redirectAttributes ： 模拟重定向带上数据
	 */
	@PostMapping("/register")
	public String register(@Valid UserRegisterVo vo, BindingResult result, RedirectAttributes redirectAttributes) {
		//注册成功，回到登录页
				if(result.hasErrors()){

					// 将错误属性与错误信息一一封装
					Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, fieldError -> fieldError.getDefaultMessage()));
					// addFlashAttribute 这个数据只取一次
					redirectAttributes.addFlashAttribute("errors", errors);
					return "redirect:http://auth.gulimall.com/reg.html";
				}
				// 开始注册 调用远程服务
				// 1.校验验证码
				String code = vo.getCode();

				String redis_code = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
				if(!StringUtils.isEmpty(redis_code)){
					// 验证码通过
					if(code.equals(redis_code.split("_")[0])){
						// 删除验证码
						stringRedisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
						// 调用远程服务进行注册
						R r = memberFeignService.register(vo);
						if(r.getCode() == 0){
							// 成功
							return "redirect:http://auth.gulimall.com/login.html";
						}else{
							Map<String, String> errors = new HashMap<>();
							errors.put("msg",r.getData("msg",new TypeReference<String>(){}));
							redirectAttributes.addFlashAttribute("errors",errors);
							return "redirect:http://auth.gulimall.com/reg.html";
						}
					}else{
						Map<String, String> errors = new HashMap<>();
						errors.put("code", "验证码错误");
						// addFlashAttribute 这个数据只取一次
						redirectAttributes.addFlashAttribute("errors", errors);
						return "redirect:http://auth.gulimall.com/reg.html";
					}
				}else{
					Map<String, String> errors = new HashMap<>();
					errors.put("code", "验证码错误");
					// addFlashAttribute 这个数据只取一次
					redirectAttributes.addFlashAttribute("errors", errors);
					return "redirect:http://auth.gulimall.com/reg.html";
				}
	}
	
}
