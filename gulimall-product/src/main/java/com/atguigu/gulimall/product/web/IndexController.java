package com.atguigu.gulimall.product.web;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;


@Controller
public class IndexController {

	@Autowired
	private CategoryService categoryService;
	
	@GetMapping({"/", "index", "/index.html"})
	public String indexPage(Model model) {
		//获得所有分类的数据
		List<CategoryEntity> categorys = categoryService.getLevel1Categorys();
		model.addAttribute("categorys", categorys);
		return "index";
	}
	
	@ResponseBody
	@RequestMapping("index/catalog.json")
	public Map<String, List<Catelog2Vo>> getCatlogJson() {
		Map<String, List<Catelog2Vo>> map = categoryService.getCatelogJson();
		return map;
	}
}
