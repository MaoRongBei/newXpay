package com.hrtpayment.xpay.quickpay.common.service;

import java.math.BigDecimal;
import java.util.Map;

import com.hrtpayment.xpay.quickpay.common.bean.QuickPayBean;
import com.hrtpayment.xpay.utils.exception.BusinessException;

public interface PayService {
	//获取短信验证码
	Map<String, String> getMessage( QuickPayBean bean ) throws BusinessException;
	//订单查询
	String queryOrder(Map<String,Object > orderInfo,String type )throws BusinessException;
	String refundQueryOrder(Map<String,Object > orderInfo)throws BusinessException;
	String refund(String orderid, BigDecimal amount, Map<String, Object> oriMap);
	//预下单
	String authPay(String orderId, String amount,String gateId,String accno,String mer_id,String unno,String qpcid,String ispoint) throws BusinessException;
	//快捷订单支付
	String pay(QuickPayBean bean,String orderid,String authcode,String accno)throws BusinessException;

}
