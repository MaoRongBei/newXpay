package com.hrtpayment.xpay.common.service;

import com.hrtpayment.xpay.utils.exception.BusinessException;

/**
 * 支付宝公众号支付接口
 * @author aibing
 * 2016年11月22日
 */
public interface AlipayService {
 
	/**
	 * 发送支付宝下单请求，
	 * 
	 */
	String getAliAppidByOrder(String orderid);
	String getAliAppid();
	String getAlipayPayInfo(String orderid,String openid,String userid);
	String getAlipaySecret();
	int getAlipayFiid();
	
	
}
