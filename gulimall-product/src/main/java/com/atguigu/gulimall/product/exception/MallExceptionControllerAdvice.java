package com.atguigu.gulimall.product.exception;

import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: MallExceptionControllerAdvice</p>
 * Description：集中处理所有异常
 * date：2020/6/1 21:19
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.firenay.mall.product.controller")
public class MallExceptionControllerAdvice {


	@ExceptionHandler(value = {WebExchangeBindException.class})
	public R handleVaildException(WebExchangeBindException e) {
		log.error("数据校验出现问题{}，异常类型：{}", e.getMessage(), e.getClass());
		BindingResult bindingResult = e.getBindingResult();

		Map<String, String> errorMap = new HashMap<>();
		bindingResult.getFieldErrors().forEach((fieldError) -> {
			// 错误字段 、 错误提示
			errorMap.put(fieldError.getField(), fieldError.getDefaultMessage());
		});
		return R.error(BizCodeEnume.VAILD_EXCEPTION.getCode(), BizCodeEnume.VAILD_EXCEPTION.getMsg()).put("data", errorMap);
	}

	@ExceptionHandler(value = Throwable.class)
	public R handleException(Throwable throwable) {

		log.error("错误：", throwable);
		return R.error(BizCodeEnume.UNKNOW_EXCEPTION.getCode(), BizCodeEnume.UNKNOW_EXCEPTION.getMsg());
	}
}