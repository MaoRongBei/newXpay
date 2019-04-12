package com.hrtpayment.xpay.common.service;

/**
 * 公众号支付接口
 * @author aibing
 * 2016年11月22日
 */
public interface WxpayService {
	String getWxpayAppid();
	/**
	 * 发送微信下单请求,更新订单状态
	 * (通道商通过接口完成下单,一码付在输入金额确定后下单)
	 * @param orderid
	 * @return
	 */
	String getWxpayPayInfo(String orderid,String openid);
	String getWxpaySecret();
	int getWxpayFiid();

	
}
