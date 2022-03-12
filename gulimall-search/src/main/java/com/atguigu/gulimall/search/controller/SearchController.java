package com.atguigu.gulimall.search.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.atguigu.gulimall.search.service.MallSearchSevice;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;



@Controller
public class SearchController {

	@Autowired
	MallSearchSevice mallSearchSevice;
	
	@GetMapping("/list.html")
	//无论任何服务请求搜索服务，都要将搜索条件封装成SearchParm类，之后调用MallSearchSevice的search方法返回结果
	//springMvc自动将页面传过来的查询条件封装成封装好的对象
	public String listPage(SearchParam parm, Model model, HttpServletRequest request){
		// 获取路径原生的查询属性
		parm.set_queryString(request.getQueryString());
		// ES中检索到的结果 传递给页面
		SearchResult result = mallSearchSevice.search(parm);
		model.addAttribute("result", result);
		return "list";
	}
}
