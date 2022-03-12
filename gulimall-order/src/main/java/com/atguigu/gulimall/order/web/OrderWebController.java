package com.atguigu.gulimall.order.web;

import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.atguigu.common.exception.NotStockException;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import com.atguigu.gulimall.order.vo.SubmitOrderResponseVo;

/**
 * <p>Title: OrderController</p>
 * Description：订单
 * date：2020/6/29 22:35
 */
/**
 * @longxue
 *
 * 
 */
@Controller
public class OrderWebController {

	@Autowired
	private OrderService orderService;

	
	/**
	 * @param model
	 * @return 返回订单详情数据OrderConfirmVo
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@GetMapping("/toTrade")
	public String toTrade(Model model) throws ExecutionException, InterruptedException {
		OrderConfirmVo confirmVo = orderService.confirmOrder();

		model.addAttribute("orderConfirmData", confirmVo);
		return "confirm";
	}

	/**
	 * 下单功能
	 */
	@PostMapping("/submitOrder")
	public String submitOrder(OrderSubmitVo submitVo, Model model, RedirectAttributes redirectAttributes){

		try {
			SubmitOrderResponseVo responseVo = orderService.submitOrder(submitVo);
			// 下单失败回到订单重新确认订单信息
			if(responseVo.getCode() == 0){
				// 下单成功取支付选项
				model.addAttribute("submitOrderResp", responseVo);
				return "pay";
			}else{
				String msg = "下单失败";
				switch (responseVo.getCode()){
					case 1: msg += "订单信息过期,请刷新在提交";break;
					case 2: msg += "订单商品价格发送变化,请确认后再次提交";break;
					case 3: msg += "商品库存不足";break;
				}
				redirectAttributes.addFlashAttribute("msg", msg);
				return "redirect:http://order.gulimall.com/toTrade";
			}
		} catch (Exception e) {
			if (e instanceof NotStockException){
				String message = e.getMessage();
				redirectAttributes.addFlashAttribute("msg", message);
			}
			return "redirect:http://order.gulimall.com/toTrade";
		}
	}
}
