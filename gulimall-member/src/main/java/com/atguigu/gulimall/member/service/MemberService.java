package com.atguigu.gulimall.member.service;

import java.util.Map;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.exception.PhoneExistException;
import com.atguigu.gulimall.member.exception.UserNameExistException;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.SocialUser;
import com.atguigu.gulimall.member.vo.UserRegisterVo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 会员
 *
 * @author leifengyang
 * @email leifengyang@gmail.com
 * @date 2019-10-08 09:47:05
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);
    
    void register(UserRegisterVo userRegisterVo) throws PhoneExistException, UserNameExistException;

	void checkPhone(String phone) throws PhoneExistException;

	void checkUserName(String username) throws UserNameExistException;

	/**
	 * 普通登录
	 */
	MemberEntity login(MemberLoginVo vo);

	/**
	 * 社交登录
	 */
	MemberEntity login(SocialUser socialUser);
}

