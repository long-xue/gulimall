package com.atguigu.gulimall.product.web;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.gulimall.product.service.SkuInfoService;
import com.atguigu.gulimall.product.vo.SkuItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.concurrent.ExecutionException;


@Controller
public class ItemController {
	
	@Autowired
	SkuInfoService skuInfoService;
	
	@RequestMapping("/{skuId}.html")
	public String skuItem(@PathVariable("skuId") Long skuId, Model model, HttpServletRequest request, HttpServletResponse response) throws InterruptedException, ExecutionException {
		HttpSession session = request.getSession();
		Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
		session.setAttribute(AuthServerConstant.LOGIN_USER,session.getAttribute(AuthServerConstant.LOGIN_USER));
		//获取商品详细信息
		SkuItemVo vo = skuInfoService.item(skuId);
		model.addAttribute("item", vo);
		return "item";
	}
	
}
